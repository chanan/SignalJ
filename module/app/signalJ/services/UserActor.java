package signalJ.services;

import akka.actor.*;
import akka.japi.pf.ReceiveBuilder;
import scala.concurrent.duration.Duration;
import signalJ.SignalJPlugin;
import signalJ.infrastructure.ProtectedData;
import signalJ.models.*;
import signalJ.services.transports.LongPollingTransport;
import signalJ.services.transports.ServerSentTransport;
import signalJ.services.transports.WebsocketTransport;

import java.util.*;
import java.util.concurrent.TimeUnit;

class UserActor extends AbstractActor {
    private final ProtectedData protectedData;
    private final Map<Long, Set<TransportMessage>> messages = new HashMap<>();
    private long messageId = 0;
    private ActorRef transport;
    private boolean connected = false;
    private final List<String> hubs = new ArrayList<>();
    private final ActorRef signalJActor = SignalJPlugin.getSignalJActor();
    private UUID uuid;
    private Map<String, String[]> queryString;
    private TransportType transportType;

    UserActor(ProtectedData protectedData) {
        this.protectedData = protectedData;
        receive(
                ReceiveBuilder.match(TransportJoinMessage.class, join -> {
                    uuid = join.getContext().connectionId;
                    queryString = join.getContext().queryString;
                    transportType = join.getTransportType();
                    transport = context().actorOf(Props.create(getTransportClass(join.getTransportType()), protectedData, join));
                    transport.tell(join, self());
                    connected = true;
                    context().watch(transport);
                    hubs.add(join.getContext().hubName);
                }).match(Messages.MethodReturn.class, methodReturn -> {
                    final Messages.MethodReturn message = new Messages.MethodReturn(methodReturn.out, methodReturn.context, methodReturn.returnValue, messageId++);
                    storeMessage(message);
                    if (connected) transport.tell(message, self());
                }).match(Messages.ClientFunctionCall.class, clientFunctionCall -> {
                    final Messages.ClientFunctionCall message = new Messages.ClientFunctionCall(clientFunctionCall.method, clientFunctionCall.hubName, clientFunctionCall.context, clientFunctionCall.sendType, clientFunctionCall.name, clientFunctionCall.args, clientFunctionCall.clients, clientFunctionCall.allExcept, clientFunctionCall.groupName, messageId++);
                    storeMessage(message);
                    if (connected) transport.tell(message, self());
                }).match(Messages.ClientCallEnd.class, clientCallEnd -> {
                    final Messages.ClientCallEnd message = new Messages.ClientCallEnd(clientCallEnd.out, clientCallEnd.context, messageId++);
                    storeMessage(message);
                    if (connected) transport.tell(message, self());
                }).match(Messages.Reconnect.class, reconnect -> {
                    attemptStopTransport();
                    //TODO Fix the below line to be the correct join and not hardcoded to websocket
                    final Messages.JoinWebsocket join = new Messages.JoinWebsocket(reconnect.out, reconnect.in, reconnect.context);
                    transport = context().actorOf(Props.create(getTransportClass(join.getTransportType()), protectedData, join));
                    transport.tell(reconnect, self());
                    connected = true;
                    resendMessages();
                }).match(Messages.Quit.class, quit -> {
                    context().setReceiveTimeout(Duration.create(SignalJPlugin.getConfiguration().getDisconnectTimeout(), TimeUnit.SECONDS));
                }).match(ReceiveTimeout.class, r -> {
                    hubs.stream().forEach(hub -> signalJActor.tell(new Messages.Disconnection(new RequestContext(uuid, queryString, hub)), self()));
                    context().stop(self());
                }).match(Messages.Ack.class, ack -> {
                    if(messages.containsKey(ack.message.getMessageId()) && messages.get(ack.message.getMessageId()).contains(ack.message)) {
                        messages.get(ack.message.getMessageId()).remove(ack.message);
                        if (messages.get(ack.message.getMessageId()).isEmpty())
                            messages.remove(ack.message.getMessageId());
                    }
                }).match(Terminated.class, t -> t.actor().equals(transport), t -> {
                    transport = null;
                    connected = false;
                }).match(Messages.StateChange.class, state -> {
                    storeMessage(state);
                    if (connected) transport.tell(state, self());
                }).match(Messages.Error.class, error -> {
                    storeMessage(error);
                    if (connected) transport.tell(error, self());
                }).match(Messages.PollForMessages.class, poll -> {
                    if(connected && transportType == TransportType.longPolling) transport.forward(poll, context());
                }).match(Messages.LongPollingSend.class, lps -> {
                    if(connected && transportType == TransportType.longPolling) transport.forward(lps, context());
                }).build()
        );
    }

    private Class<?> getTransportClass(TransportType transportType) throws Exception {
        switch (transportType) {
            case websocket:
                return WebsocketTransport.class;
            case serverSentEvents:
                return ServerSentTransport.class;
            case longPolling:
                return LongPollingTransport.class;
            case foreverFrames:
                throw new Exception("Not Implemented");
        }
        throw new IllegalArgumentException();
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