package signalJ.services.transports;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.ReceiveTimeout;
import akka.japi.pf.ReceiveBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import play.Logger;
import play.libs.Json;
import play.mvc.WebSocket;
import scala.concurrent.duration.Duration;
import signalJ.SignalJPlugin;
import signalJ.infrastructure.Cursor;
import signalJ.infrastructure.ProtectedData;
import signalJ.models.Messages;
import signalJ.models.RequestContext;
import signalJ.models.TransportMessage;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class WebsocketTransport extends AbstractActor {
    private final UUID uuid;
    private final WebSocket.Out<JsonNode> out;
    private final WebSocket.In<JsonNode> in;
    private final String prefix = Cursor.GetCursorPrefix();
    private final ActorRef signalJActor = SignalJPlugin.getSignalJActor();
    private final ObjectMapper mapper = new ObjectMapper();
    private final ProtectedData protectedData;
    private final Map<String, String[]> queryString;

    public WebsocketTransport(ProtectedData protectedData, Messages.JoinWebsocket join) {
        this.protectedData = protectedData;
        this.uuid = join.uuid;
        this.out = join.out;
        this.in = join.in;
        this.queryString = join.queryString;

        final ActorRef self = getContext().self();

        in.onClose(() -> {
            signalJActor.tell(new Messages.Quit(uuid), self);
            self.tell(PoisonPill.getInstance(), self);
        });
        in.onMessage(json -> {
            Logger.debug("Message from user: " + uuid + " : " + json);
            signalJActor.tell(new Messages.Execute(uuid, json, queryString), self);
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
                ).match(Messages.Reconnect.class, r -> Logger.debug("Reconnect Websocket " + r.uuid)
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
