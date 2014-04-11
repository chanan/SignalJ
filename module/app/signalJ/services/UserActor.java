package signalJ.services;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import play.Logger;
import play.libs.F.Callback;
import play.libs.F.Callback0;
import play.libs.Json;
import play.mvc.WebSocket;
import signalJ.services.ChannelActor.ClientFunctionCall;
import signalJ.services.SignalJActor.Join;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;

import com.fasterxml.jackson.databind.JsonNode;

class UserActor extends UntypedActor {
	private UUID uuid;
	private WebSocket.Out<JsonNode> out;
    private WebSocket.In<JsonNode> in;
    private final ActorRef signalJActor = ActorLocator.getSignalJActor();
    
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Join) {
			final Join join = (Join) message;
			this.uuid = join.uuid;
			this.out = join.out;
			this.in = join.in;
			
			final ActorRef self = getSelf();
			in.onClose(new Callback0() {
				public void invoke() {
					signalJActor.tell(new SignalJActor.Quit(uuid), self);
	            }
			});
			in.onMessage(new Callback<JsonNode>() {
				@Override
				public void invoke(JsonNode json) throws Throwable {
					Logger.debug("Message from user: " + json);
					self.tell(new InternalMessage(json), self);
				}
			});
		}
		if(message instanceof MethodReturn) {
			final MethodReturn methodReturn = (MethodReturn) message;
			final signalJ.models.Messages.MethodReturn json = new signalJ.models.Messages.MethodReturn(methodReturn.uuid, methodReturn.id, methodReturn.hub, methodReturn.method, methodReturn.returnType, methodReturn.returnValue);
			final JsonNode j = Json.toJson(json);
			out.write(j);
			Logger.debug("Return Value: " + j);
		}
		if(message instanceof ClientFunctionCall) {
			final ClientFunctionCall clientFunctionCall = (ClientFunctionCall) message;
			final signalJ.models.Messages.ClientFunctionCall json = new signalJ.models.Messages.ClientFunctionCall(clientFunctionCall.caller, clientFunctionCall.channelName, clientFunctionCall.name);
			if(clientFunctionCall.args != null) {
				int i = 0;
				for(final Object obj : clientFunctionCall.args) {
					json.addParameter("param_" + i , obj); //TODO put real name from hubDescriptor
					i++;
				}
			}
			final JsonNode j = Json.toJson(json);
			out.write(j);
			Logger.debug("ClientFunctionCall Value: " + j);
		}
		if(message instanceof InternalMessage) {
			final InternalMessage internalMessage = (InternalMessage) message;
			if(internalMessage.json.get("type").textValue().equalsIgnoreCase("ChannelJoin")) {
				signalJActor.tell(new SignalJActor.ChannelJoin(internalMessage.json.get("channel").textValue(), 
						UUID.fromString(internalMessage.json.get("uuid").textValue())), getSelf());
			}
			if(internalMessage.json.get("type").textValue().equalsIgnoreCase("SendToAll")) {
				signalJActor.tell(new SignalJActor.SendToAll(internalMessage.json.get("message").textValue()), getSelf());
			}
			if(internalMessage.json.get("type").textValue().equalsIgnoreCase("SendToChannel")) {
				signalJActor.tell(new SignalJActor.SendToChannel(internalMessage.json.get("channel").textValue(),
						internalMessage.json.get("message").textValue()), getSelf());
			}
			if(internalMessage.json.get("type").textValue().equalsIgnoreCase("execute")) {
				signalJActor.tell(new SignalJActor.Execute(internalMessage.json), getSelf());
			}
			if(internalMessage.json.get("type").textValue().equalsIgnoreCase("describe")) {
				signalJActor.tell(new SignalJActor.Describe(internalMessage.json, getSelf()), getSelf());
			}
			if(internalMessage.json.get("type").textValue().equalsIgnoreCase("groupAdd")) {
				signalJActor.tell(new SignalJActor.GroupJoin(internalMessage.json.get("group").textValue(),
						UUID.fromString(internalMessage.json.get("uuid").textValue())), getSelf());
			}
			if(internalMessage.json.get("type").textValue().equalsIgnoreCase("groupRemove")) {
				signalJActor.tell(new SignalJActor.GroupLeave(internalMessage.json.get("group").textValue(),
						UUID.fromString(internalMessage.json.get("uuid").textValue())), getSelf());
			}
		}
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
	
	public static class ChannelJoin {
		final String channelName;
		final ActorRef channel;
		
		public ChannelJoin(String channelName, ActorRef channel) {
			this.channelName = channelName;
			this.channel = channel;
		}
	}
	
	public static class Quit {
		
	}
	
	private static class InternalMessage {
		final JsonNode json;
		
		public InternalMessage(JsonNode json) {
			this.json = json;
		}
	}
}