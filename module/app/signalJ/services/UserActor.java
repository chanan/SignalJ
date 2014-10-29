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
import signalJ.infrastructure.DefaultProtectedData;
import signalJ.infrastructure.ProtectedData;
import signalJ.infrastructure.Purposes;
import signalJ.services.HubActor.ClientFunctionCall;
import signalJ.services.SignalJActor.Join;

import java.io.IOException;
import java.util.UUID;

class UserActor extends AbstractActor {
	private UUID uuid;
	private WebSocket.Out<JsonNode> out;
    private WebSocket.In<JsonNode> in;
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
                }).match(MethodReturn.class, methodReturn -> {
                    final signalJ.models.Messages.MethodReturn json = new signalJ.models.Messages.MethodReturn(methodReturn.uuid, methodReturn.id, methodReturn.hub, methodReturn.method, methodReturn.returnType, methodReturn.returnValue);
                    final JsonNode j = Json.toJson(json);
                    out.write(j);
                    Logger.debug("Return Value: " + j);
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
                }).match(ReceiveTimeout.class, r-> {
                    out.write(mapper.readTree("{ }"));
                }).build()
        );
    }

    private JsonNode createNode(ClientFunctionCall clientFunctionCall) throws IOException {
        //final JsonNode event = mapper.readTree("{\"C\":\"d-4F8237E2-A,0|H,0|I,1|J,0\",\"S\":1,\"M\":[]}");
        //{"C":"d-74B585BD-A,2|E,0|F,1|G,0","M":[{"H":"ChatHub","M":"addNewMessageToPage","A":["Chanan","Hello"]}]}
        final StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"C\":");
        //sb.append("\"d-74B585BD-A,2|E,0|F,1|G,0\"");
        sb.append('"').append(protectedData.protect(uuid.toString(), Purposes.ConnectionToken).get()).append('"');
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
		final UUID uuid;
		final String id;
		final Object returnValue;
		final String hub;
		final String method;
		final String returnType;
		
		public MethodReturn(UUID uuid, String id, Object returnValue, String hub, String method, String returnType) {
			this.uuid = uuid;
			this.id = id;
			this.returnValue = returnValue;
			this.hub = hub;
			this.method = method;
			this.returnType = returnType;
		}
	}
	
	private static class InternalMessage {
		final JsonNode json;
		
		public InternalMessage(JsonNode json) {
			this.json = json;
		}
	}
}