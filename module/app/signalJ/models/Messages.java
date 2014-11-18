package signalJ.models;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.EventSource;
import play.mvc.Results;
import play.mvc.WebSocket;
import signalJ.services.Hub;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
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
        public final Optional<Results.Chunks.Out<String>> out;
        public final Optional<Map<String, String>> changes;


        public MethodReturn(RequestContext context, Object returnValue, long messageId, Results.Chunks.Out<String> out, Optional<Map<String, String>> changes) {
            this.context = context;
            this.returnValue = returnValue;
            this.messageId = messageId;
            this.out = Optional.of(out);
            this.changes = changes;
        }

        public MethodReturn(RequestContext context, Object returnValue, long messageId, Optional<Results.Chunks.Out<String>> out, Map<String, String> changes) {
            this.context = context;
            this.returnValue = returnValue;
            this.messageId = messageId;
            this.out = out;
            this.changes = Optional.of(changes);
        }

        public MethodReturn(RequestContext context, Object returnValue, long messageId, Optional<Results.Chunks.Out<String>> out, Optional<Map<String, String>> changes) {
            this.context = context;
            this.returnValue = returnValue;
            this.messageId = messageId;
            this.out = out;
            this.changes = changes;
        }

        public MethodReturn(RequestContext context, Object returnValue, long messageId, Map<String, String> changes) {
            this.context = context;
            this.returnValue = returnValue;
            this.messageId = messageId;
            this.changes = Optional.of(changes);
            this.out = Optional.empty();
        }

        public MethodReturn(RequestContext context, Object returnValue, long messageId, Optional<Map<String, String>> changes) {
            this.context = context;
            this.returnValue = returnValue;
            this.messageId = messageId;
            this.changes = changes;
            this.out = Optional.empty();
        }

        public MethodReturn(RequestContext context, Object returnValue, Results.Chunks.Out<String> out, Optional<Map<String, String>> changes) {
            this.context = context;
            this.returnValue = returnValue;
            this.messageId = -1;
            this.out = Optional.of(out);
            this.changes = changes;
        }

        public MethodReturn(RequestContext context, Object returnValue, Optional<Results.Chunks.Out<String>> out, Map<String, String> changes) {
            this.context = context;
            this.returnValue = returnValue;
            this.messageId = -1;
            this.out = out;
            this.changes = Optional.of(changes);
        }

        public MethodReturn(RequestContext context, Object returnValue, Optional<Results.Chunks.Out<String>> out) {
            this.context = context;
            this.returnValue = returnValue;
            this.messageId = -1;
            this.out = out;
            this.changes = Optional.empty();
        }

        public MethodReturn(RequestContext context, Object returnValue, Optional<Results.Chunks.Out<String>> out, Optional<Map<String, String>> changes) {
            this.context = context;
            this.returnValue = returnValue;
            this.messageId = -1;
            this.out = out;
            this.changes = changes;
        }

        public MethodReturn(RequestContext context, Object returnValue, Map<String, String> changes) {
            this.context = context;
            this.returnValue = returnValue;
            this.messageId = -1;
            this.changes = Optional.of(changes);
            this.out = Optional.empty();
        }

        @Override
        public long getMessageId() {
            return messageId;
        }
    }

    public static class ClientCallEnd implements TransportMessage {
        public final RequestContext context;
        public final long messageId;
        public final Optional<Results.Chunks.Out<String>> out;

        public ClientCallEnd(RequestContext context) {
            this.context = context;
            this.messageId = -1;
            this.out = Optional.empty();
        }

        public ClientCallEnd(RequestContext context, long messageId) {
            this.context = context;
            this.messageId = messageId;
            this.out = Optional.empty();
        }

        public ClientCallEnd(Results.Chunks.Out<String> out, RequestContext context) {
            this.context = context;
            this.messageId = -1;
            this.out = Optional.of(out);
        }

        public ClientCallEnd(Results.Chunks.Out<String> out, RequestContext context, long messageId) {
            this.context = context;
            this.messageId = messageId;
            this.out = Optional.of(out);
        }

        public ClientCallEnd(Optional<Results.Chunks.Out<String>> out, RequestContext context) {
            this.context = context;
            this.messageId = -1;
            this.out = out;
        }

        public ClientCallEnd(Optional<Results.Chunks.Out<String>> out, RequestContext context, long messageId) {
            this.context = context;
            this.messageId = messageId;
            this.out = out;
        }

        @Override
        public long getMessageId() {
            return messageId;
        }
    }

    public static class GetJavaScript {

    }

    public static class JoinWebsocket implements TransportJoinMessage {
        public final RequestContext context;
        public final WebSocket.Out<JsonNode> out;
        public final WebSocket.In<JsonNode> in;

        public JoinWebsocket(WebSocket.Out<JsonNode> out, WebSocket.In<JsonNode> in, RequestContext context) {
            this.out = out;
            this.in = in;
            this.context = context;
        }

        @Override
        public RequestContext getContext() {
            return context;
        }

        @Override
        public TransportType getTransportType() {
            return TransportType.websocket;
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
        public final RequestContext context;
        public final JsonNode json;
        public final Optional<Results.Chunks.Out<String>> out;

        public Execute(RequestContext context, JsonNode data) {
            this.context = context;
            this.json = data;
            this.out = Optional.empty();
        }

        public Execute(Results.Chunks.Out<String> out, RequestContext context, JsonNode data) {
            this.context = context;
            this.json = data;
            this.out = Optional.of(out);
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
        public final TransportMessage message;

        public Ack(TransportMessage message) {
            this.message = message;
        }
    }

    public static class StateChange implements TransportMessage {
        public final UUID uuid;
        public final Map<String, String> changes;
        public final long messageId;
        public final Optional<Results.Chunks.Out<String>> out;

        public StateChange(UUID uuid, Map<String, String> changes, long messageId) {
            this.uuid = uuid;
            this.changes = changes;
            this.messageId = messageId;
            this.out = Optional.empty();
        }

        public StateChange(Results.Chunks.Out<String> out, UUID uuid, Map<String, String> changes, long messageId) {
            this.uuid = uuid;
            this.changes = changes;
            this.messageId = messageId;
            this.out = Optional.of(out);
        }

        public StateChange(Optional<Results.Chunks.Out<String>> out, UUID uuid, Map<String, String> changes, long messageId) {
            this.uuid = uuid;
            this.changes = changes;
            this.messageId = messageId;
            this.out = out;
        }

        @Override
        public long getMessageId() {
            return messageId;
        }
    }

    public static class Error implements TransportMessage {
        public final UUID uuid;
        public final String error;
        public final long messageId;
        public final Optional<Results.Chunks.Out<String>> out;

        public Error(UUID uuid, String error) {
            this.uuid = uuid;
            this.error = error;
            this.messageId = 1;
            this.out = Optional.empty();
        }

        public Error(UUID uuid, String error, long messageId) {
            this.uuid = uuid;
            this.error = error;
            this.messageId = messageId;
            this.out = Optional.empty();
        }

        public Error(Results.Chunks.Out<String> out, UUID uuid, String error) {
            this.uuid = uuid;
            this.error = error;
            this.messageId = 1;
            this.out = Optional.of(out);
        }

        public Error(Results.Chunks.Out<String> out, UUID uuid, String error, long messageId) {
            this.uuid = uuid;
            this.error = error;
            this.messageId = messageId;
            this.out = Optional.of(out);
        }

        public Error(Optional<Results.Chunks.Out<String>> out, UUID uuid, String error) {
            this.uuid = uuid;
            this.error = error;
            this.messageId = 1;
            this.out = out;
        }

        public Error(Optional<Results.Chunks.Out<String>> out, UUID uuid, String error, long messageId) {
            this.uuid = uuid;
            this.error = error;
            this.messageId = messageId;
            this.out = out;
        }

        @Override
        public long getMessageId() {
            return messageId;
        }
    }

    public static class Connection implements ServerEventMessage {
        public final RequestContext context;

        public Connection(RequestContext context) {
            this.context = context;
        }

        @Override
        public RequestContext getContext() {
            return context;
        }
    }

    public static class Reconnection implements ServerEventMessage {
        public final RequestContext context;

        public Reconnection(RequestContext context) {
            this.context = context;
        }

        @Override
        public RequestContext getContext() {
            return context;
        }
    }

    public static class Disconnection implements ServerEventMessage {
        public final RequestContext context;

        public Disconnection(RequestContext context) {
            this.context = context;
        }

        @Override
        public RequestContext getContext() {
            return context;
        }
    }

    public static class JoinServerSentEvents implements TransportJoinMessage {
        public final RequestContext context;
        public final EventSource eventSource;

        public JoinServerSentEvents(EventSource eventSource, RequestContext context) {
            this.eventSource = eventSource;
            this.context = context;
        }

        @Override
        public RequestContext getContext() {
            return context;
        }

        @Override
        public TransportType getTransportType() {
            return TransportType.serverSentEvents;
        }
    }

    public static class JoinLongPolling implements TransportJoinMessage {
        public final RequestContext context;
        public final Results.Chunks.Out<String> out;

        public JoinLongPolling(Results.Chunks.Out<String> out, RequestContext context) {
            this.out = out;
            this.context = context;
        }

        @Override
        public RequestContext getContext() {
            return context;
        }

        @Override
        public TransportType getTransportType() {
            return TransportType.longPolling;
        }
    }

    public static class PollForMessages {
        public final Results.Chunks.Out<String> out;
        public final RequestContext context;

        public PollForMessages(Results.Chunks.Out<String> out, RequestContext context) {
            this.context = context;
            this.out = out;
        }
    }

    public static class Abort {
        public final RequestContext context;

        public Abort(RequestContext context) {
            this.context = context;
        }
    }

    public static class ReconnectWebsocket implements TransportReconnectMessage {
        public final RequestContext context;
        public final WebSocket.Out<JsonNode> out;
        public final WebSocket.In<JsonNode> in;

        public ReconnectWebsocket(WebSocket.Out<JsonNode> out, WebSocket.In<JsonNode> in, RequestContext context) {
            this.out = out;
            this.in = in;
            this.context = context;
        }

        @Override
        public RequestContext getContext() {
            return context;
        }

        @Override
        public TransportType getTransportType() {
            return TransportType.websocket;
        }
    }

    public static class ReconnectServerSentEvents implements TransportReconnectMessage {
        public final RequestContext context;
        public final EventSource eventSource;

        public ReconnectServerSentEvents(EventSource eventSource, RequestContext context) {
            this.eventSource = eventSource;
            this.context = context;
        }

        @Override
        public RequestContext getContext() {
            return context;
        }

        @Override
        public TransportType getTransportType() {
            return TransportType.serverSentEvents;
        }
    }

    public static class ReconnectLongPolling implements TransportReconnectMessage {
        public final RequestContext context;
        public final Results.Chunks.Out<String> out;

        public ReconnectLongPolling(Results.Chunks.Out<String> out, RequestContext context) {
            this.out = out;
            this.context = context;
        }

        @Override
        public RequestContext getContext() {
            return context;
        }

        @Override
        public TransportType getTransportType() {
            return TransportType.longPolling;
        }
    }
}