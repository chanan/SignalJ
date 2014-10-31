package signalJ.models;
import akka.actor.ActorRef;
import com.fasterxml.jackson.databind.JsonNode;
import play.mvc.WebSocket;
import signalJ.services.Hub;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Messages {
    //TODO maybe use inheritance to make this more sane
    public static class ClientFunctionCall {
        public final String hubName;
        public final String name;
        public final Object[] args;
        public final SendType sendType;
        public final RequestContext context;
        public final Method method;
        public final UUID[] clients;
        public final UUID[] allExcept;
        public final String groupName;

        public ClientFunctionCall(Method method, String hubName, RequestContext context, SendType sendType, String name, Object[] args, UUID[] clients, UUID[] allExcept, String groupName) {
            this.hubName = hubName;
            this.context = context;
            this.sendType = sendType;
            this.name = name;
            this.args = args;
            this.method = method;
            this.clients = clients;
            this.allExcept = allExcept;
            this.groupName = groupName;
        }
    }

    public enum SendType
    {
        All,
        Others,
        Caller,
        Clients,
        AllExcept,
        Group,
        InGroupExcept
    }

    public static class MethodReturn {
        public final RequestContext context;
        public final Object returnValue;

        public MethodReturn(RequestContext context, Object returnValue) {
            this.context = context;
            this.returnValue = returnValue;
        }
    }

    public static class ClientCallEnd {
        public final RequestContext context;

        public ClientCallEnd(RequestContext context) {
            this.context = context;
        }
    }
	
	/*public static class Parameter {
		private final String name;
		private final Object value;
		
		public String getName() {
			return name;
		}
		
		public Object getValue() {
			return value;
		}
		
		@Override
		public String toString() {
			return "{name: " + name + ", value: " + value.toString() + "}";
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Parameter other = (Parameter) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}
		
		public Parameter(String name, Object value) {
			this.name = name;
			this.value = value;
		}	
	}*/

    public static class GetUser{
        public final UUID uuid;

        public GetUser(UUID uuid) {
            this.uuid = uuid;
        }
    }

    public static class GetJavaScript {

    }

    public static class GetJavaScript2 {

    }

    public static class HubJoin {
        public final UUID uuid;
        public final ActorRef user;

        public HubJoin(UUID uuid, ActorRef user) {
            this.uuid = uuid;
            this.user = user;
        }
    }

    public static class GetHub {
        public final String hubName;

        public GetHub(String hubName) {
            this.hubName = hubName;
        }
    }

    /*public static class Join {
        public final UUID uuid;
        public final ActorRef user;

        public Join(UUID uuid, ActorRef user) {
            this.uuid = uuid;
            this.user = user;
        }
    }*/

    public static class Send {
        public final String message;

        public Send(String message) {
            this.message = message;
        }
    }

    public static class Join {
        public final UUID uuid;
        public final WebSocket.Out<JsonNode> out;
        public final WebSocket.In<JsonNode> in;

        public Join(WebSocket.Out<JsonNode> out, WebSocket.In<JsonNode> in, UUID uuid) {
            this.out = out;
            this.in = in;
            this.uuid = uuid;
        }
    }

    public static class Reconnect {
        public final UUID uuid;
        public final WebSocket.Out<JsonNode> out;
        public final WebSocket.In<JsonNode> in;

        public Reconnect(WebSocket.Out<JsonNode> out, WebSocket.In<JsonNode> in, UUID uuid) {
            this.out = out;
            this.in = in;
            this.uuid = uuid;
        }
    }

    /*public static class HubJoin {
        final String hubName;
        final UUID uuid;

        public HubJoin(String hubName, UUID uuid) {
            this.hubName = hubName;
            this.uuid = uuid;
        }
    }*/

    public static class Quit {
        public final UUID uuid;

        public Quit(UUID uuid) {
            this.uuid = uuid;
        }
    }

    public static class RegisterHub {
        public final Class<? extends Hub<?>> hub;
        public final HubsDescriptor.HubDescriptor descriptor;

        public RegisterHub(Class<? extends Hub<?>> hub, HubsDescriptor.HubDescriptor descriptor) {
            this.hub = hub;
            this.descriptor = descriptor;
        }
    }

    public static class Execute {
        public final UUID uuid;
        public final JsonNode json;

        public Execute(UUID uuid, JsonNode json) {
            this.uuid = uuid;
            this.json = json;
        }
    }

    public static class Describe {
        public final JsonNode json;
        public final ActorRef user;

        public Describe(JsonNode json, ActorRef user) {
            this.json = json;
            this.user = user;
        }
    }

    public static class GroupJoin {
        public final String groupname;
        public final UUID uuid;

        public GroupJoin(String groupname, UUID uuid) {
            this.groupname = groupname;
            this.uuid = uuid;
        }
    }

    public static class GroupLeave {
        public final String groupname;
        public final UUID uuid;

        public GroupLeave(String groupname, UUID uuid) {
            this.groupname = groupname;
            this.uuid = uuid;
        }
    }
}