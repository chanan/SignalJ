package signalJ.services;

import akka.actor.*;
import akka.japi.pf.ReceiveBuilder;
import scala.concurrent.duration.Duration;
import signalJ.SignalJPlugin;
import signalJ.infrastructure.ProtectedData;
import signalJ.models.Messages;
import signalJ.models.TransportMessage;

import java.util.*;

class UserActor extends AbstractActor {
    private final ProtectedData protectedData;
    private final Map<Long, Set<TransportMessage>> messages = new HashMap<>();
    private long messageId = 0;
    private ActorRef transport;
    private boolean connected = false;
    private final List<String> hubs = new ArrayList<>();
    private final ActorRef signalJActor = SignalJPlugin.getSignalJActor();
    private UUID uuid;

    UserActor(ProtectedData protectedData) {
        this.protectedData = protectedData;
        receive(
                ReceiveBuilder.match(Messages.Join.class, join -> {
                    uuid = join.uuid;
                    transport = context().actorOf(Props.create(WebsocketTransport.class, protectedData, join));
                    transport.tell(join, self());
                    connected = true;
                    context().watch(transport);
                    hubs.add(join.hubName);
                }).match(Messages.MethodReturn.class, methodReturn -> {
                    final Messages.MethodReturn message = new Messages.MethodReturn(methodReturn.context, methodReturn.returnValue, messageId++);
                    storeMessage(message);
                    if (connected) transport.tell(message, self());
                }).match(Messages.ClientFunctionCall.class, clientFunctionCall -> {
                    final Messages.ClientFunctionCall message = new Messages.ClientFunctionCall(clientFunctionCall.method, clientFunctionCall.hubName, clientFunctionCall.context, clientFunctionCall.sendType, clientFunctionCall.name, clientFunctionCall.args, clientFunctionCall.clients, clientFunctionCall.allExcept, clientFunctionCall.groupName, messageId++);
                    storeMessage(message);
                    if (connected) transport.tell(message, self());
                }).match(Messages.ClientCallEnd.class, clientCallEnd -> {
                    final Messages.ClientCallEnd message = new Messages.ClientCallEnd(clientCallEnd.context, messageId++);
                    storeMessage(message);
                    if (connected) transport.tell(message, self());
                }).match(Messages.Reconnect.class, reconnect -> {
                    attemptStopTransport();
                    final Messages.Join join = new Messages.Join(reconnect.out, reconnect.in, reconnect.uuid, null);
                    transport = context().actorOf(Props.create(WebsocketTransport.class, protectedData, join));
                    transport.tell(reconnect, self());
                    connected = true;
                    resendMessages();
                }).match(Messages.Quit.class, quit -> {
                    //Wait a minute before shutting down allowing clients to reconnect
                    context().setReceiveTimeout(Duration.create("1 minute"));
                }).match(ReceiveTimeout.class, r -> {
                    hubs.stream().forEach(hub -> signalJActor.tell(new Messages.Disconnection(uuid, hub), self()));
                    context().stop(self());
                }).match(Messages.Ack.class, ack -> {
                    messages.get(ack.message.getMessageId()).remove(ack.message);
                    if(messages.get(ack.message.getMessageId()).isEmpty()) messages.remove(ack.message.getMessageId());
                }).match(Terminated.class, t -> t.actor().equals(transport), t -> {
                    transport = null;
                    connected = false;
                }).match(Messages.StateChange.class, state -> {
                    storeMessage(state);
                    if (connected) transport.tell(state, self());
                }).match(Messages.Error.class, error -> {
                    storeMessage(error);
                    if (connected) transport.tell(error, self());
                }).build()
        );
    }

    private void storeMessage(TransportMessage message) {
        messages.putIfAbsent(message.getMessageId(), new HashSet<>());
        messages.get(message.getMessageId()).add(message);
    }

    private void resendMessages() {
        messages.keySet().stream().sorted().map(l -> messages.get(l))
                .forEach(set -> set.stream().forEach(m -> transport.tell(m, self())));
    }

    private void attemptStopTransport() {
        try {
            if (connected) transport.tell(PoisonPill.getInstance(), self());
        } catch (Exception e) { }
    }
}