package signalJ.services.transports;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.ReceiveTimeout;
import akka.japi.pf.ReceiveBuilder;
import play.Logger;
import play.libs.EventSource;
import play.libs.F;
import scala.concurrent.duration.Duration;
import signalJ.SignalJPlugin;
import signalJ.infrastructure.Cursor;
import signalJ.infrastructure.ProtectedData;
import signalJ.models.Messages;
import signalJ.models.TransportMessage;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ServerSentTransport extends AbstractActor {
    private final EventSource eventSource;
    private final String prefix = Cursor.GetCursorPrefix();
    private final ActorRef signalJActor = SignalJPlugin.getSignalJActor();
    private final ProtectedData protectedData;

    public ServerSentTransport(ProtectedData protectedData, Messages.JoinServerSentEvents join) {
        this.protectedData = protectedData;
        this.eventSource = join.eventSource;

        final ActorRef self = getContext().self();
        final UUID connectionId = join.context.connectionId;

        eventSource.onDisconnected(new F.Callback0() {
            @Override
            public void invoke() throws Throwable {
                signalJActor.tell(new Messages.Quit(connectionId), self);
                self.tell(PoisonPill.getInstance(), self);
            }
        });

        context().setReceiveTimeout(Duration.create(SignalJPlugin.getConfiguration().getKeepAliveTimeout() / 2, TimeUnit.SECONDS));

        receive(
                ReceiveBuilder.match(Messages.JoinServerSentEvents.class, r -> eventSource.send(EventSource.Event.event(JsonHelper.writeConnect(prefix)))
                ).match(Messages.MethodReturn.class, methodReturn -> {
                    eventSource.send(EventSource.Event.event(JsonHelper.writeMethodReturn(methodReturn)));
                    sendAck(methodReturn);
                }).match(Messages.ClientFunctionCall.class, clientFunctionCall -> {
                    eventSource.send(EventSource.Event.event(JsonHelper.writeClientFunctionCall(clientFunctionCall, prefix)));
                    sendAck(clientFunctionCall);
                }).match(Messages.ClientCallEnd.class, clientCallEnd -> {
                    eventSource.send(EventSource.Event.event(JsonHelper.writeConfirm(clientCallEnd.context)));
                    sendAck(clientCallEnd);
                }).match(ReceiveTimeout.class, r -> eventSource.send(EventSource.Event.event(JsonHelper.writeHeartbeat()))
                ).match(Messages.Reconnect.class, r -> Logger.debug("Reconnect ServerSentEvents " + r.context.connectionId)
                ).match(Messages.StateChange.class, state -> {
                    eventSource.send(EventSource.Event.event(JsonHelper.writeState(state)));
                    sendAck(state);
                }).match(Messages.Error.class, error -> {
                    eventSource.send(EventSource.Event.event(JsonHelper.writeError(error)));
                    sendAck(error);
                }).build()
        );
    }

    private void sendAck(TransportMessage transportMessage) {
        context().parent().tell(new Messages.Ack(transportMessage), self());
    }

    @Override
    public void postStop() throws Exception {
        Logger.debug("Transport stop");
    }
}