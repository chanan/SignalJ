package signalJ.models;

import akka.actor.ActorRef;
import com.fasterxml.jackson.databind.JsonNode;
import play.mvc.WebSocket;
import signalJ.services.Hub;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public class Messages {
    //TODO maybe use inheritance to make this more sane
    public static class ClientFunctionCall implements TransportMessage {
        public final String hubName;
        public final String name;
        public final Object[] args;
        public final SendType sendType;
        public final RequestContext context;
        public final Method method;
        public final UUID[] clients;
        public final UUID[] allExcept;
        public final String groupName;
        public final long messageId;

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
            this.messageId = -1;
        }

        public ClientFunctionCall(Method method, String hubName, RequestContext context, SendType sendType, String name, Object[] args, UUID[] clients, UUID[] allExcept, String groupName, long messageId) {
            this.hubName = hubName;
            this.context = context;
            this.sendType = sendType;
            this.name = name;
            this.args = args;
            this.method = method;
            this.clients = clients;
            this.allExcept = allExcept;
            this.groupName = groupName;
            this.messageId = messageId;
        }

        @Override
        public long getMessageId() {
            return messageId;
        }

        @Override
        public String toString() {
            return "ClientFunctionCall{" +
                    "hubName='" + hubName + '\'' +
                    ", name='" + name + '\'' +
                    ", args=" + Arrays.toString(args) +
                    ", sendType=" + sendType +
                    ", context=" + context +
                    ", method=" + method +
                    ", clients=" + Arrays.toString(clients) +
                    ", allExcept=" + Arrays.toString(allExcept) +
                    ", groupName='" + groupName + '\'' +
                    ", messageId=" + messageId +
                    '}';
        }
    }

    public enum SendType {
        All,
        Others,
        Caller,
        Clients,
        AllExcept,
        Group,
        InGroupExcept
    }

    public static class MethodReturn implements TransportMessage {
        public final RequestContext context;
        public final Object returnValue;
        public final long messageId;

        public MethodReturn(RequestContext context, Object returnValue) {
            this.context = context;
            this.returnValue = returnValue;
            this.messageId = -1;
        }

        public MethodReturn(RequestContext context, Object returnValue, long messageId) {
            this.context = context;
            this.returnValue = returnValue;
            this.messageId = messageId;
        }

        @Override
        public long getMessageId() {
            return messageId;
        }
    }

    public static class ClientCallEnd implements TransportMessage {
        public final RequestContext context;
        public final long messageId;

        public ClientCallEnd(RequestContext context) {
            this.context = context;
            this.messageId = -1;
        }

        public ClientCallEnd(RequestContext context, long messageId) {
            this.context = context;
            this.messageId = messageId;
        }

        @Override
        public long getMessageId() {
            return messageId;
        }
    }

    public static class GetJavaScript {

    }

    public static class HubJoin {
        public final UUID uuid;
        public final ActorRef user;

        public HubJoin(UUID uuid, ActorRef user) {
            this.uuid = uuid;
            this.user = user;
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

    public static class GroupJoin {
        public final String hubname;
        public final String groupname;
        public final UUID uuid;

        public GroupJoin(String hubname, String groupname, UUID uuid) {
            this.hubname = hubname;
            this.groupname = groupname;
            this.uuid = uuid;
        }
    }

    public static class GroupLeave {
        public final String hubname;
        public final String groupname;
        public final UUID uuid;

        public GroupLeave(String hubname, String groupname, UUID uuid) {
            this.hubname = hubname;
            this.groupname = groupname;
            this.uuid = uuid;
        }
    }

    public static class Ack {
        public final long MessageId;

        public Ack(long messageId) {
            MessageId = messageId;
        }
    }

    public static class StateChange implements TransportMessage {
        public final UUID uuid;
        public final Map<String, String> changes;
        public final long messageId;

        public StateChange(UUID uuid, Map<String, String> changes, long messageId) {
            this.uuid = uuid;
            this.changes = changes;
            this.messageId = messageId;
        }

        @Override
        public long getMessageId() {
            return messageId;
        }
    }
}