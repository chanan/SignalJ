package signalJ.services;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import play.Logger;
import play.mvc.WebSocket;
import signalJ.models.HubsDescriptor;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;

import com.fasterxml.jackson.databind.JsonNode;

class SignalJActor extends UntypedActor  {
	//private final ActorRef usersActor;
	private final ActorRef channelsActor = ActorLocator.getChannelsActor(getContext());
	private final ActorRef hubsActor = ActorLocator.getHubsActor();
	private final Map<UUID, ActorRef> users = new HashMap<UUID, ActorRef>();
	
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Join) {
			final Join join = (Join) message;
			final ActorRef user = ActorLocator.getUserActor(getContext(), join.uuid.toString());
			users.put(join.uuid, user);
			user.tell(join, getSelf());
			channelsActor.tell(new ChannelsActor.ChannelJoin(join.uuid, user), getSelf());
			Logger.debug(join.uuid + " logged on");
		}
		if(message instanceof Quit) {
			final Quit quit = (Quit) message;
			final ActorRef user = users.remove(quit.uuid);
			user.tell(new UserActor.Quit(), getSelf());
			Logger.debug(quit.uuid + " logged off");
		}
		if(message instanceof SendToChannel) {
			channelsActor.forward(message, getContext());
		}
		if(message instanceof RegisterHub) {
			channelsActor.forward(message, getContext());
		}
		if(message instanceof Execute) {
			channelsActor.forward(message, getContext());
		}
		if(message instanceof Describe) {
			hubsActor.forward(message, getContext());
		}
	}
	
	public static class Join {
		public final UUID uuid = UUID.randomUUID();
        final WebSocket.Out<JsonNode> out;
        final WebSocket.In<JsonNode> in;
        
        public Join(WebSocket.Out<JsonNode> out, WebSocket.In<JsonNode> in) {
            this.out = out;
            this.in = in;
        }
    }
	
	public static class ChannelJoin {
		final String channelName;
		final UUID uuid;
		
		public ChannelJoin(String channelName, UUID uuid) {
			this.channelName = channelName;
			this.uuid = uuid;
		}
	}
	
	public static class Quit {
		final UUID uuid;
		
		public Quit(UUID uuid) {
			this.uuid = uuid;
		}
	}
	
	public static class SendToAll {
		final String message;
		
		public SendToAll(String message) {
			this.message = message;
		}
	}
	
	public static class Send {
		final UUID uuid;
		final String message;
		
		public Send(UUID uuid, String message) {
			this.uuid = uuid;
			this.message = message; 
		}
	}
	
	public static class SendToChannel {
		final String channel;
		final String message;
		
		public SendToChannel(String channel, String message) {
			this.channel = channel;
			this.message = message;
		}
	}
	
	public static class RegisterHub {
		final Class<? extends Hub<?>> hub;
		final HubsDescriptor.HubDescriptor descriptor;
		
		public RegisterHub(Class<? extends Hub<?>> hub, HubsDescriptor.HubDescriptor descriptor) {
			this.hub = hub;
			this.descriptor = descriptor;
		}
	}
	
	public static class Execute {
		final JsonNode json;
		
		public Execute(JsonNode json) {
			this.json = json;
		}
	}
	
	public static class Describe {
		final JsonNode json;
		final ActorRef user;
		
		public Describe(JsonNode json, ActorRef user) {
			this.json = json;
			this.user = user;
		}
	}
}