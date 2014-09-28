package signalJ.services;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import play.Logger;
import play.mvc.WebSocket;
import signalJ.models.HubsDescriptor;
import signalJ.services.HubActor.ClientFunctionCall;

import java.util.UUID;

public class SignalJActor extends AbstractActor {
    private final ActorRef usersActor = context().actorOf(Props.create(UsersActor.class), "users");
	private final ActorRef hubsActor = context().actorOf(Props.create(HubsActor.class), "hubs");
    private final ActorRef groupsActor = context().actorOf(Props.create(GroupsActor.class), "groups");

    public SignalJActor() {
        receive(ReceiveBuilder.match(
                Join.class, join -> {
                    usersActor.forward(join, context());
                    Logger.debug(join.uuid + " logged on");
                }
        ).match(
                Quit.class, quit -> {
                    usersActor.forward(quit, context());
                    groupsActor.forward(quit, context());
                }
        ).match(
                SendToHub.class, sendToHub -> hubsActor.forward(sendToHub, context())
        ).match(
                RegisterHub.class, registerHub -> hubsActor.forward(registerHub, context())
        ).match(
                Execute.class, execute -> hubsActor.forward(execute, context())
        ).match(
                Describe.class, describe -> hubsActor.forward(describe, context())
        ).match(
                GroupJoin.class, groupJoin -> groupsActor.forward(groupJoin, context())
        ).match(
                GroupLeave.class, groupLeave -> groupsActor.forward(groupLeave, context())
        ).match(
                ClientFunctionCall.class, clientFunctionCall -> {
                    switch (clientFunctionCall.sendType) {
                        case All:
                        case Others:
                        case Caller:
                        case Clients:
                        case AllExcept:
                            usersActor.forward(clientFunctionCall, context());
                            break;
                        case Group:
                        case InGroupExcept:
                            groupsActor.forward(clientFunctionCall, context());
                            break;
                        default:
                            break;
                    }
                }
        ).match(
                UserActor.MethodReturn.class, methodReturn -> usersActor.forward(methodReturn, context())
        ).match(
                HubsActor.GetJavaScript.class, getJavaScript -> hubsActor.forward(getJavaScript, context())
        ).build());
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
	
	public static class HubJoin {
		final String hubName;
		final UUID uuid;
		
		public HubJoin(String hubName, UUID uuid) {
			this.hubName = hubName;
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
	
	public static class SendToHub {
		final String hubName;
		final String message;
		
		public SendToHub(String hubName, String message) {
			this.hubName = hubName;
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