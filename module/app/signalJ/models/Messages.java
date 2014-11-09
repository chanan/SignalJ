package signalJ.models;

import akka.actor.ActorRef;
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

        public MethodReturn(RequestContext context, Object returnValue) {
            this.context = context;
            this.returnValue = returnValue;
            this.messageId = -1;
            this.out = Optional.empty();
        }

        public MethodReturn(RequestContext context, Object returnValue, long messageId) {
            this.context = context;
            this.returnValue = returnValue;
            this.messageId = messageId;
            this.out = Optional.empty();
        }

        public MethodReturn(Results.Chunks.Out<String> out, RequestContext context, Object returnValue) {
            this.context = context;
            this.returnValue = returnValue;
            this.messageId = -1;
            this.out = Optional.of(out);
        }

        public MethodReturn(Results.Chunks.Out<String> out, RequestContext context, Object returnValue, long messageId) {
            this.context = context;
            this.returnValue = returnValue;
            this.messageId = -messageId;
            this.out = Optional.of(out);
        }

        public MethodReturn(Optional<Results.Chunks.Out<String>> out, RequestContext context, Object returnValue) {
            this.context = context;
            this.returnValue = returnValue;
            this.messageId = -1;
            this.out = out;
        }

        public MethodReturn(Optional<Results.Chunks.Out<String>> out, RequestContext context, Object returnValue, long messageId) {
            this.context = context;
            this.returnValue = returnValue;
            this.messageId = -messageId;
            this.out = out;
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
        public final UUID uuid;
        public final WebSocket.Out<JsonNode> out;
        public final WebSocket.In<JsonNode> in;
        public final String hubName;
        public final Map<String, String[]> queryString;

        public JoinWebsocket(WebSocket.Out<JsonNode> out, WebSocket.In<JsonNode> in, UUID uuid, String hubName, Map<String, String[]> queryString) {
            this.out = out;
            this.in = in;
            this.uuid = uuid;
            this.hubName = hubName;
            this.queryString = queryString;
        }

        @Override
        public UUID getConnectionId() {
            return uuid;
        }

        @Override
        public String getHubName() {
            return hubName;
        }

        @Override
        public Map<String, String[]> getQueryString() {
            return queryString;
        }

        @Override
        public TransportType getTransportType() {
            return TransportType.websocket;
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
        public final Map<String, String[]> queryString;
        public final Optional<Results.Chunks.Out<String>> out;

        public Execute(UUID uuid, JsonNode json, Map<String, String[]> queryString) {
            this.uuid = uuid;
            this.json = json;
            this.queryString = queryString;
            this.out = Optional.empty();
        }

        public Execute(Results.Chunks.Out<String> out, UUID uuid, JsonNode json, Map<String, String[]> queryString) {
            this.uuid = uuid;
            this.json = json;
            this.queryString = queryString;
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
        public final UUID uuid;
        public final String hubName;
        public final Map<String, String[]> queryString;

        public Connection(UUID uuid, String hubName, Map<String, String[]> queryString) {
            this.uuid = uuid;
            this.hubName = hubName;
            this.queryString = queryString;
        }

        @Override
        public UUID getUuid() {
            return uuid;
        }

        @Override
        public String getHubName() {
            return hubName;
        }

        @Override
        public Map<String, String[]> getQueryString() {
            return queryString;
        }
    }

    public static class Reconnection implements ServerEventMessage {
        public final UUID uuid;
        public final String hubName;
        public final Map<String, String[]> queryString;

        public Reconnection(UUID uuid, String hubName, Map<String, String[]> queryString) {
            this.uuid = uuid;
            this.hubName = hubName;
            this.queryString = queryString;
        }

        @Override
        public UUID getUuid() {
            return uuid;
        }

        @Override
        public String getHubName() {
            return hubName;
        }

        @Override
        public Map<String, String[]> getQueryString() {
            return queryString;
        }
    }

    public static class Disconnection implements ServerEventMessage {
        public final UUID uuid;
        public final String hubName;
        public final Map<String, String[]> queryString;

        public Disconnection(UUID uuid, String hubName, Map<String, String[]> queryString) {
            this.uuid = uuid;
            this.hubName = hubName;
            this.queryString = queryString;
        }

        @Override
        public UUID getUuid() {
            return uuid;
        }

        @Override
        public String getHubName() {
            return hubName;
        }

        @Override
        public Map<String, String[]> getQueryString() {
            return queryString;
        }
    }

    public static class JoinServerSentEvents implements TransportJoinMessage {
        public final UUID uuid;
        public final EventSource eventSource;
        public final String hubName;
        public final Map<String, String[]> queryString;

        public JoinServerSentEvents(EventSource eventSource, UUID uuid, String hubName, Map<String, String[]> queryString) {
            this.eventSource = eventSource;
            this.uuid = uuid;
            this.hubName = hubName;
            this.queryString = queryString;
        }

        @Override
        public UUID getConnectionId() {
            return uuid;
        }

        @Override
        public String getHubName() {
            return hubName;
        }

        @Override
        public Map<String, String[]> getQueryString() {
            return queryString;
        }

        @Override
        public TransportType getTransportType() {
            return TransportType.serverSentEvents;
        }
    }

    public static class JoinLongPolling implements TransportJoinMessage {
        public final UUID uuid;
        public final Results.Chunks.Out<String> out;
        public final String hubName;
        public final Map<String, String[]> queryString;

        public JoinLongPolling(Results.Chunks.Out<String> out, UUID uuid, String hubName, Map<String, String[]> queryString) {
            this.out = out;
            this.uuid = uuid;
            this.hubName = hubName;
            this.queryString = queryString;
        }

        @Override
        public UUID getConnectionId() {
            return uuid;
        }

        @Override
        public String getHubName() {
            return hubName;
        }

        @Override
        public Map<String, String[]> getQueryString() {
            return queryString;
        }

        @Override
        public TransportType getTransportType() {
            return TransportType.longPolling;
        }
    }

    public static class PollForMessages {
        public final UUID uuid;
        public final Results.Chunks.Out<String> out;
        public final String hubName;
        public final Map<String, String[]> queryString;

        public PollForMessages(Results.Chunks.Out<String> out, UUID uuid, String hubName, Map<String, String[]> queryString) {
            this.out = out;
            this.uuid = uuid;
            this.hubName = hubName;
            this.queryString = queryString;
        }
    }

    public static class LongPollingSend {
        public final UUID uuid;
        public final Results.Chunks.Out<String> out;

        public LongPollingSend(UUID uuid, Results.Chunks.Out<String> out) {
            this.uuid = uuid;
            this.out = out;
        }
    }

    public static class LongPollingBeat {

    }
}