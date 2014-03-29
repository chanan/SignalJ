package services;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import play.Logger;
import play.libs.F.Callback;
import play.libs.F.Callback0;
import play.libs.Json;
import play.mvc.WebSocket;
import services.SignalJActor.Join;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.name.Named;

class UserActor extends UntypedActor {
	private UUID uuid;
	private WebSocket.Out<JsonNode> out;
    private WebSocket.In<JsonNode> in;
    private final ActorRef signalJActor;
    private final Map<String, ActorRef> channels = new HashMap<String, ActorRef>();
    
    
    @Inject
    public UserActor(@Named("SignalJActor") ActorRef signalJActor) {
    	this.signalJActor = signalJActor;
    }

	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Join) {
			final Join join = (Join) message;
			if(uuid == null) new IllegalArgumentException("Join cannot be called twice!");
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
		if(message instanceof ChannelJoin) {
			final ChannelJoin channelJoin = (ChannelJoin) message;
			channels.put(channelJoin.channelName, channelJoin.channel);
		}
		if(message instanceof Quit) {
			for(final ActorRef channel : channels.values()) {
				channel.tell(new ChannelActor.Quit(uuid), getSelf());
			}
			getContext().stop(getSelf());
		}
		if(message instanceof Send) {
			final Send send = (Send) message;
			final ObjectNode event = Json.newObject();
			event.put("uuid", uuid.toString());
			event.put("type", "message");
			event.put("message", send.message);
			out.write(event);
			Logger.debug(uuid + ": " + send.message);
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
		}
	}
	
	public static class Send {
		final String message;
		
		public Send(String message) {
			this.message = message;
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