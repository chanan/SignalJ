package signalJ.services.transports;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.ReceiveTimeout;
import akka.japi.pf.ReceiveBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import play.Logger;
import play.mvc.WebSocket;
import scala.concurrent.duration.Duration;
import signalJ.SignalJPlugin;
import signalJ.infrastructure.Cursor;
import signalJ.infrastructure.ProtectedData;
import signalJ.models.Messages;
import signalJ.models.RequestContext;
import signalJ.models.TransportMessage;

import java.util.concurrent.TimeUnit;

public class WebsocketTransport extends AbstractActor {
    private final WebSocket.Out<JsonNode> out;
    private final WebSocket.In<JsonNode> in;
    private final String prefix = Cursor.GetCursorPrefix();
    private final ActorRef signalJActor = SignalJPlugin.getSignalJActor();
    private final RequestContext context;

    public WebsocketTransport(Messages.JoinWebsocket join) {
        this.out = join.out;
        this.in = join.in;
        this.context = join.context;

        final ActorRef self = getContext().self();

        in.onClose(() -> {
            signalJActor.tell(new Messages.Quit(context.connectionId), self);
            self.tell(PoisonPill.getInstance(), self);
        });
        in.onMessage(json -> {
            Logger.debug("Message from user: " + context.connectionId + " : " + json);
            signalJActor.tell(new Messages.Execute(context, json), self);
        });

        context().setReceiveTimeout(Duration.create(SignalJPlugin.getConfiguration().getKeepAliveTimeout() / 2, TimeUnit.SECONDS));

        receive(
                ReceiveBuilder.match(Messages.JoinWebsocket.class, r -> out.write(JsonHelper.writeConnect(prefix))
                ).match(Messages.MethodReturn.class, methodReturn -> {
                    out.write(JsonHelper.writeMethodReturn(methodReturn));
                    sendAck(methodReturn);
                }).match(Messages.ClientFunctionCall.class, clientFunctionCall -> {
                    out.write(JsonHelper.writeClientFunctionCall(clientFunctionCall, prefix));
                    sendAck(clientFunctionCall);
                }).match(Messages.ClientCallEnd.class, clientCallEnd -> {
                    out.write(JsonHelper.writeConfirm(clientCallEnd.context));
                    sendAck(clientCallEnd);
                }).match(ReceiveTimeout.class, r -> out.write(JsonHelper.writeHeartbeat())
                ).match(Messages.ReconnectWebsocket.class, r -> Logger.debug("Reconnect Websocket " + r.context.connectionId)
                ).match(Messages.StateChange.class, state -> {
                    out.write(JsonHelper.writeState(state));
                    sendAck(state);
                }).match(Messages.Error.class, error -> {
                    out.write(JsonHelper.writeError(error));
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
