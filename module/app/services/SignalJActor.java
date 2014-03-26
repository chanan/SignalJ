package services;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import play.Logger;
import play.mvc.WebSocket;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akkaGuice.PropsContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Singleton;

@Singleton
public class SignalJActor extends UntypedActor {
	private final Map<UUID, ActorRef> users = new HashMap<UUID, ActorRef>();
	private final Map<String, ActorRef> channels = new HashMap<String, ActorRef>();
	
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Join) {
			final Join join = (Join) message;
			final ActorRef user = getContext().actorOf(PropsContext.get(UserActor.class));
			users.put(join.uuid, user);
			user.tell(join, getSelf());
			Logger.debug(join.uuid + " logged on");
		}
		if(message instanceof Quit) {
			final Quit quit = (Quit) message;
			final ActorRef user = users.remove(quit.uuid);
			user.tell(new UserActor.Quit(), getSelf());
			Logger.debug(quit.uuid + " logged off");
		}
		if(message instanceof SendToAll) {
			final SendToAll sendToAll = (SendToAll) message;
			for(final ActorRef user : users.values()) {
				user.tell(new UserActor.Send(sendToAll.message), getSelf());
			}
		}
		if(message instanceof Send) {
			final Send send = (Send) message;
			final ActorRef user = users.get(send.uuid);
			user.tell(new UserActor.Send(send.message), getSelf());
		}
		if(message instanceof SendToChannel) {
			final SendToChannel sendToChannel = (SendToChannel) message;
			final ActorRef channel = channels.get(sendToChannel.channel);
			channel.tell(new ChannelActor.Send(sendToChannel.message), getSelf());
		}
		if(message instanceof ChannelJoin) {
			final ChannelJoin channelJoin = (ChannelJoin) message;
			ActorRef channel;
			if(!channels.containsKey(channelJoin.channelName)) {
				channel = getContext().actorOf(PropsContext.get(ChannelActor.class));
				channels.put(channelJoin.channelName, channel);
			} else {
				channel = channels.get(channelJoin.channelName);
			}
			final ActorRef user = users.get(channelJoin.uuid);
			channel.tell(new ChannelActor.Join(channelJoin.uuid, user), getSelf());
			user.tell(new UserActor.ChannelJoin(channelJoin.channelName, channel), getSelf());
			Logger.debug(channelJoin.uuid + " join channel: " + channelJoin.channelName);
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
}