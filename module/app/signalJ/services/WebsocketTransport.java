package signalJ.services;

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
import signalJ.models.RequestContext;

import java.io.IOException;
import java.util.UUID;

public class WebsocketTransport extends AbstractActor {
    private final UUID uuid;
    private final WebSocket.Out<JsonNode> out;
    private final WebSocket.In<JsonNode> in;
    private final String prefix = Cursor.GetCursorPrefix();
    private final ActorRef signalJActor = SignalJPlugin.getSignalJActor();
    private final ObjectMapper mapper = new ObjectMapper();
    private final ProtectedData protectedData;

    public WebsocketTransport(ProtectedData protectedData, SignalJActor.Join join) {
        this.protectedData = protectedData;
        this.uuid = join.uuid;
        this.out = join.out;
        this.in = join.in;

        final ActorRef self = getContext().self();

        in.onClose(() -> {
            signalJActor.tell(new SignalJActor.Quit(uuid), self);
            self.tell(PoisonPill.getInstance(), self);
        });
        in.onMessage(json -> {
            Logger.debug("Message from user: " + uuid + " : " + json);
            self.tell(new InternalMessage(json), self);
        });

        context().setReceiveTimeout(Duration.create("20 seconds"));

        receive(ReceiveBuilder.match(SignalJActor.Join.class, r -> writeConnect()
                ).match(UserActor.MethodReturn.class, methodReturn -> writeMethodReturn(methodReturn)
                ).match(HubActor.ClientFunctionCall.class, clientFunctionCall -> writeClientFunctionCall(clientFunctionCall)
                ).match(InternalMessage.class, internalMessage -> {
                    if (internalMessage.json.hasNonNull("H")) {
                        signalJActor.tell(new SignalJActor.Execute(uuid, internalMessage.json), self());
                    } else if (internalMessage.json.get("type").textValue().equalsIgnoreCase("execute")) {
                        signalJActor.tell(new SignalJActor.Execute(uuid, internalMessage.json), self());
                    } else if (internalMessage.json.get("type").textValue().equalsIgnoreCase("describe")) {
                        signalJActor.tell(new SignalJActor.Describe(internalMessage.json, self()), self());
                    } else if (internalMessage.json.get("type").textValue().equalsIgnoreCase("groupAdd")) {
                        signalJActor.tell(new SignalJActor.GroupJoin(internalMessage.json.get("group").textValue(),
                                UUID.fromString(internalMessage.json.get("uuid").textValue())), self());
                    } else if (internalMessage.json.get("type").textValue().equalsIgnoreCase("groupRemove")) {
                        signalJActor.tell(new SignalJActor.GroupLeave(internalMessage.json.get("group").textValue(),
                                UUID.fromString(internalMessage.json.get("uuid").textValue())), self());
                    }
                }).match(UserActor.ClientCallEnd.class, clientCallEnd -> writeConfirm(clientCallEnd.context)
                ).match(ReceiveTimeout.class, r -> writeHeartbeat()
                ).match(SignalJActor.Reconnect.class, r -> Logger.debug("Reconnect Websocket " + r.uuid)
                ).build()
        );
    }

    @Override
    public void postStop() throws Exception {
        Logger.debug("Transport stop");
    }

    private void writeHeartbeat() throws IOException {
        out.write(mapper.readTree("{ }"));
    }

    private void writeConnect() throws IOException {
        final StringBuilder sb = new StringBuilder();
        sb.append("{\"C\":\"");
        sb.append(prefix);
        sb.append("\",\"S\":1,\"M\":[]}");
        final JsonNode event = mapper.readTree(sb.toString());
        out.write(event);
    }

    private void writeMethodReturn(UserActor.MethodReturn methodReturn) throws IOException {
        final StringBuilder sb = new StringBuilder();
        sb.append("{").append("\"R\":");
        sb.append(Json.toJson(methodReturn.returnValue));
        sb.append(",\"I\":\"").append(methodReturn.context.messageId).append("\"}");
        final JsonNode event = mapper.readTree(sb.toString());
        out.write(event);
        Logger.debug("Return Value: " + event);
    }

    private void writeClientFunctionCall(HubActor.ClientFunctionCall clientFunctionCall) throws IOException {
        final StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"C\":");

        sb.append('"').append(prefix).append('"');
        sb.append(",\"M\":[");
        sb.append('{');
        sb.append("\"H\":").append('"').append(clientFunctionCall.hubName).append('"').append(',');
        sb.append("\"M\":").append('"').append(clientFunctionCall.method.getName()).append('"').append(',');
        sb.append("\"A\":[");
        boolean first = true;
        for (final Object obj : clientFunctionCall.args) {
            if (!first) sb.append(',');
            first = false;
            sb.append(Json.toJson(obj));
        }
        sb.append("]}]}");
        final JsonNode j = mapper.readTree(sb.toString());
        out.write(j);
        Logger.debug("ClientFunctionCall Value: " + j);
    }

    private void writeConfirm(RequestContext context) throws IOException {
        final StringBuilder sb = new StringBuilder();
        sb.append("{\"I\":\"").append(context.messageId).append("\"}");
        final JsonNode event = mapper.readTree(sb.toString());
        out.write(event);
    }

    private static class InternalMessage {
        final JsonNode json;

        public InternalMessage(JsonNode json) {
            this.json = json;
        }
    }
}
