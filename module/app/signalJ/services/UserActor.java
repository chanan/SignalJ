package signalJ.services;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ReceiveTimeout;
import akka.japi.pf.ReceiveBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import play.Logger;
import play.libs.F.Callback;
import play.libs.F.Callback0;
import play.libs.Json;
import play.mvc.WebSocket;
import scala.concurrent.duration.Duration;
import signalJ.SignalJPlugin;
import signalJ.infrastructure.Cursor;
import signalJ.infrastructure.DefaultProtectedData;
import signalJ.infrastructure.ProtectedData;
import signalJ.infrastructure.Purposes;
import signalJ.models.RequestContext;
import signalJ.services.HubActor.ClientFunctionCall;
import signalJ.services.SignalJActor.Join;

import java.io.IOException;
import java.util.UUID;

class UserActor extends AbstractActor {
	private UUID uuid;
	private WebSocket.Out<JsonNode> out;
    private WebSocket.In<JsonNode> in;
    private final String prefix = Cursor.GetCursorPrefix();
    private final ActorRef signalJActor = SignalJPlugin.getSignalJActor();
    private final ObjectMapper mapper = new ObjectMapper();
    private final ProtectedData protectedData;

    UserActor(ProtectedData protectedData) {
        this.protectedData = protectedData;
        context().setReceiveTimeout(Duration.create("20 seconds"));
        receive(
                ReceiveBuilder.match(Join.class, join -> {
                    this.uuid = join.uuid;
                    this.out = join.out;
                    this.in = join.in;

                    final ActorRef self = self();
                    in.onClose(() -> signalJActor.tell(new SignalJActor.Quit(uuid), self));
                    in.onMessage(json -> {
                        Logger.debug("Message from user: " + uuid + " : " + json);
                        self.tell(new InternalMessage(json), self);
                    });
                    sendConnect();
                }).match(MethodReturn.class, methodReturn -> {
                    writeMethodReturn(methodReturn);
                }).match(ClientFunctionCall.class, clientFunctionCall -> {
                    /*final signalJ.models.Messages.ClientFunctionCall json = new signalJ.models.Messages.ClientFunctionCall(clientFunctionCall.caller, clientFunctionCall.hubName, clientFunctionCall.name);
                    if (clientFunctionCall.args != null) {
                        int i = 0;
                        for (final Object obj : clientFunctionCall.args) {
                            json.addParameter("param_" + i, obj); //TODO put real name from hubDescriptor
                            i++;
                        }
                    }
                    final JsonNode j = Json.toJson(json);*/
                    final JsonNode j = createNode(clientFunctionCall);
                    out.write(j);
                    Logger.debug("ClientFunctionCall Value: " + j);
                }).match(InternalMessage.class, internalMessage -> {
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
                }).match(ClientCallEnd.class, clientCallEnd -> {
                    writeConfirm(clientCallEnd.context);
                }).match(ReceiveTimeout.class, r -> {
                    out.write(mapper.readTree("{ }"));
                }).build()
        );
    }

    private void writeMethodReturn(MethodReturn methodReturn) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{").append("\"R\":");
        sb.append(Json.toJson(methodReturn.returnValue));
        sb.append(",\"I\":\"").append(methodReturn.context.messageId).append("\"}");
        final JsonNode event = mapper.readTree(sb.toString());
        out.write(event);
        Logger.debug("Return Value: " + event);
    }

    private void writeConfirm(RequestContext context) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"I\":\"").append(context.messageId).append("\"}");
        final JsonNode event = mapper.readTree(sb.toString());
        out.write(event);
    }

    //TODO: Convert to serialization
    private void sendConnect() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"C\":\"");
        //sb.append(SignalJPlugin.getProtectedDataProvider().protect(uuid.toString(), Purposes.ConnectionToken).get());
        sb.append(prefix);
        sb.append("\",\"S\":1,\"M\":[]}");
        final JsonNode event = mapper.readTree(sb.toString());
        out.write(event);
    }

    private JsonNode createNode(ClientFunctionCall clientFunctionCall) throws IOException {
        //final JsonNode event = mapper.readTree("{\"C\":\"d-4F8237E2-A,0|H,0|I,1|J,0\",\"S\":1,\"M\":[]}");
        //{"C":"d-74B585BD-A,2|E,0|F,1|G,0","M":[{"H":"ChatHub","M":"addNewMessageToPage","A":["Chanan","Hello"]}]}
        final StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"C\":");
        //sb.append("\"d-74B585BD-A,2|E,0|F,1|G,0\"");
        //sb.append('"').append(protectedData.protect(uuid.toString(), Purposes.ConnectionToken).get()).append('"');
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
            //sb.append('"').append(Json.toJson(obj)).append('"');
            sb.append(Json.toJson(obj));
        }
        sb.append("]}]}");
        Logger.debug("Json: " + sb.toString());
        return mapper.readTree(sb.toString());
    }

    public static class MethodReturn {
		final RequestContext context;
		final Object returnValue;
		
		public MethodReturn(RequestContext context, Object returnValue) {
            this.context = context;
			this.returnValue = returnValue;
		}
	}
	
	private static class InternalMessage {
		final JsonNode json;
		
		public InternalMessage(JsonNode json) {
			this.json = json;
		}
	}

    public static class ClientCallEnd {
        public final RequestContext context;

        public ClientCallEnd(RequestContext context) {
            this.context = context;
        }
    }
}