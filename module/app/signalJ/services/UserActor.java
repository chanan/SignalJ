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
    private final Map<Long, Set<TransportMessage>> messages = new HashMap<>();
    private long messageId = 0;
    private Optional<ActorRef> transport = Optional.empty();
    private final List<String> hubs = new ArrayList<>();
    private final ActorRef signalJActor = SignalJPlugin.getSignalJActor();
    private UUID connectionId;
    private String username;
    private Map<String, String[]> queryString;
    private TransportType transportType;

    UserActor() {
        receive(
                ReceiveBuilder.match(TransportJoinMessage.class, join -> {
                    connectionId = join.getContext().connectionId;
                    queryString = join.getContext().queryString;
                    username = join.getContext().username;
                    transportType = join.getTransportType();
                    transport = Optional.of(context().actorOf(Props.create(getTransportClass(join.getTransportType()), join)));
                    transport.get().forward(join, context());
                    context().watch(transport.get());
                    hubs.add(join.getContext().hubName);
                }).match(Messages.MethodReturn.class, methodReturn -> {
                    final Messages.MethodReturn message = new Messages.MethodReturn(methodReturn.out, methodReturn.context, methodReturn.returnValue, messageId++);
                    storeMessage(message);
                    transport.ifPresent(t -> t.forward(message, context()));
                }).match(Messages.ClientFunctionCall.class, clientFunctionCall -> {
                    final Messages.ClientFunctionCall message = new Messages.ClientFunctionCall(clientFunctionCall.method, clientFunctionCall.hubName, clientFunctionCall.context, clientFunctionCall.sendType, clientFunctionCall.name, clientFunctionCall.args, clientFunctionCall.clients, clientFunctionCall.allExcept, clientFunctionCall.groupName, messageId++);
                    storeMessage(message);
                    transport.ifPresent(t -> t.forward(message, context()));
                }).match(Messages.ClientCallEnd.class, clientCallEnd -> {
                    final Messages.ClientCallEnd message = new Messages.ClientCallEnd(clientCallEnd.out, clientCallEnd.context, messageId++);
                    storeMessage(message);
                    transport.ifPresent(t -> t.forward(message, context()));
                }).match(TransportReconnectMessage.class, reconnect -> {
                    attemptStopTransport();
                    final TransportJoinMessage join = getJoinFromReconnect(reconnect);
                    transport = Optional.of(context().actorOf(Props.create(getTransportClass(join.getTransportType()), join)));
                    transport.get().forward(reconnect, context());
                    resendMessages();
                }).match(Messages.Quit.class, quit -> {
                    context().setReceiveTimeout(Duration.create(SignalJPlugin.getConfiguration().getDisconnectTimeout(), TimeUnit.SECONDS));
                }).match(ReceiveTimeout.class, r -> {
                    hubs.stream().forEach(hub -> signalJActor.tell(new Messages.Disconnection(new RequestContext(connectionId, username, queryString, hub)), self()));
                    context().stop(self());
                }).match(Messages.Ack.class, ack -> {
                    if (messages.containsKey(ack.message.getMessageId()) && messages.get(ack.message.getMessageId()).contains(ack.message)) {
                        messages.get(ack.message.getMessageId()).remove(ack.message);
                        if (messages.get(ack.message.getMessageId()).isEmpty())
                            messages.remove(ack.message.getMessageId());
                    }
                }).match(Terminated.class, t -> t.actor().equals(transport), t -> {
                    transport = Optional.empty();
                    context().setReceiveTimeout(Duration.create(SignalJPlugin.getConfiguration().getDisconnectTimeout(), TimeUnit.SECONDS));
                }).match(Messages.StateChange.class, state -> {
                    storeMessage(state);
                    transport.ifPresent(t -> t.forward(state, context()));
                }).match(Messages.Error.class, error -> {
                    storeMessage(error);
                    transport.ifPresent(t -> t.forward(error, context()));
                }).match(Messages.PollForMessages.class, poll -> {
                    if (transportType == TransportType.longPolling)
                        transport.ifPresent(t -> t.forward(poll, context()));
                }).match(Messages.Abort.class, abort -> {
                    if (transport.isPresent()) {
                        attemptStopTransport();
                        transport = Optional.empty();
                    }
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

    private TransportJoinMessage getJoinFromReconnect(TransportReconnectMessage reconnect) throws Exception {
        switch (reconnect.getTransportType()) {
            case websocket:
                final Messages.ReconnectWebsocket rw = (Messages.ReconnectWebsocket) reconnect;
                return new Messages.JoinWebsocket(rw.out, rw.in, rw.context);
            case serverSentEvents:
                final Messages.ReconnectServerSentEvents rs = (Messages.ReconnectServerSentEvents) reconnect;
                return new Messages.JoinServerSentEvents(rs.eventSource, rs.context);
            case longPolling:
                final Messages.ReconnectLongPolling rl = (Messages.ReconnectLongPolling) reconnect;
                return new Messages.JoinLongPolling(rl.out, rl.context);
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
                .forEach(set -> set.stream().forEach(m -> transport.ifPresent(t -> t.tell(m, self()))));
    }

    private void attemptStopTransport() {
        try {
            transport.ifPresent(t -> t.tell(PoisonPill.getInstance(), self()));
        } catch (Exception e) { }
    }
}