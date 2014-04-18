package signalJ.services;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import akka.actor.Props;
import play.Logger;
import play.libs.Akka;
import play.mvc.WebSocket;
import signalJ.models.HubsDescriptor;
import signalJ.services.ChannelActor.ClientFunctionCall;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.UntypedActor;

import com.fasterxml.jackson.databind.JsonNode;

class SignalJActor extends UntypedActor  {

	private final ActorRef channelsActor = getContext().actorOf(Props.create(ChannelsActor.class), "channels");
    private final ActorRef usersActor = getContext().actorOf(Props.create(UsersActor.class), "users");
	private final ActorRef hubsActor = ActorLocator.getHubsActor();
    private final ActorRef groupsActor = getContext().actorOf(Props.create(GroupsActor.class), "groups");
	
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Join) {
			final Join join = (Join) message;
            usersActor.forward(message, getContext());
			Logger.debug(join.uuid + " logged on");
		}
		if(message instanceof Quit) {
            usersActor.forward(message, getContext());
            groupsActor.forward(message, getContext());
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
		if(message instanceof GroupJoin) {
			groupsActor.forward(message, getContext());
		}
		if(message instanceof GroupLeave) {
            groupsActor.forward(message, getContext());
		}
		if(message instanceof ClientFunctionCall) {
			final ClientFunctionCall clientFunctionCall = (ClientFunctionCall) message;
			switch(clientFunctionCall.sendType) {

                case All:
                case Others:
                case Caller:
                case Clients:
                case AllExcept:
                    usersActor.forward(message, getContext());
                    break;
                case Group:
				case InGroupExcept:
					groupsActor.forward(message, getContext());
				break;
				default:
					break;
			}
		}
        if(message instanceof UserActor.MethodReturn) {
            usersActor.forward(message, getContext());
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
	
	public static class GroupJoin {
		final String groupname;
		final UUID uuid;
		
		public GroupJoin(String groupname, UUID uuid) {
			this.groupname = groupname;
			this.uuid = uuid;
		}
	}
	
	public static class GroupLeave {
		final String groupname;
		final UUID uuid;
		
		public GroupLeave(String groupname, UUID uuid) {
			this.groupname = groupname;
			this.uuid = uuid;
		}
	}
}