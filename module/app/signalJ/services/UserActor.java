package signalJ.services;

import akka.actor.*;
import akka.japi.pf.ReceiveBuilder;
import scala.concurrent.duration.Duration;
import signalJ.infrastructure.ProtectedData;
import signalJ.models.Messages;
import signalJ.models.TransportMessage;

import java.util.*;

class UserActor extends AbstractActor {
    private final ProtectedData protectedData;
    private final Map<Long, TransportMessage> messages = new HashMap<>();
    private long messageId = 0;
    private ActorRef transport;
    private boolean connected = false;

    UserActor(ProtectedData protectedData) {
        this.protectedData = protectedData;
        receive(
                ReceiveBuilder.match(Messages.Join.class, join -> {
                    transport = context().actorOf(Props.create(WebsocketTransport.class, protectedData, join));
                    transport.tell(join, self());
                    connected = true;
                    context().watch(transport);
                }).match(Messages.MethodReturn.class, methodReturn -> {
                    final Messages.MethodReturn message = new Messages.MethodReturn(methodReturn.context, methodReturn.returnValue, messageId++);
                    messages.put(messageId, message);
                    transport.tell(message, self());
                }).match(Messages.ClientFunctionCall.class, clientFunctionCall -> {
                    final Messages.ClientFunctionCall message = new Messages.ClientFunctionCall(clientFunctionCall.method, clientFunctionCall.hubName, clientFunctionCall.context, clientFunctionCall.sendType, clientFunctionCall.name, clientFunctionCall.args, clientFunctionCall.clients, clientFunctionCall.allExcept, clientFunctionCall.groupName, messageId++);
                    messages.put(messageId, message);
                    transport.tell(message, self());
                }).match(Messages.ClientCallEnd.class, clientCallEnd -> {
                    final Messages.ClientCallEnd message = new Messages.ClientCallEnd(clientCallEnd.context, messageId++);
                    messages.put(messageId, message);
                    transport.tell(message, self());
                }).match(Messages.Reconnect.class, reconnect -> {
                    attemptStopTransport();
                    final Messages.Join join = new Messages.Join(reconnect.out, reconnect.in, reconnect.uuid);
                    transport = context().actorOf(Props.create(WebsocketTransport.class, protectedData, join));
                    transport.tell(reconnect, self());
                    connected = true;
                    resendMessages();
                }).match(Messages.Quit.class, quit -> {
                    //Wait a minute before shutting down allowing clients to reconnect
                    context().setReceiveTimeout(Duration.create("1 minute"));
                }).match(ReceiveTimeout.class, r -> {
                    context().stop(self());
                }).match(Messages.Ack.class, ack -> {
                    messages.remove(ack.MessageId);
                }).match(Terminated.class, t -> t.actor().equals(transport), t -> {
                    transport = null;
                    connected = false;
                }).build()
        );
    }

    private void resendMessages() {
        messages.keySet().stream().sorted().map(l -> messages.get(l)).forEach(m -> transport.tell(m, self()));
    }

    private void attemptStopTransport() {
        try {
            if (connected) transport.tell(PoisonPill.getInstance(), self());
        } catch (Exception e) { }
    }
}