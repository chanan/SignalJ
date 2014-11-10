package signalJ.models;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.Json;
import play.mvc.Http;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class RequestContext {
    public final UUID connectionId;
    public final Optional<Long> messageId;
    public final Map<String, String[]> queryString;
    public final String hubName;

    public RequestContext(Http.Request request) {
        final String connectionToken = request.getQueryString("connectionToken");
        connectionId = UUID.fromString(connectionToken.substring(0, connectionToken.lastIndexOf(':')));
        final String connectionData = request.getQueryString("connectionData");
        hubName = getHubName(connectionData);
        queryString = getQueryParams(request.queryString());
        messageId = Optional.empty();
    }

    public RequestContext(UUID connectionId, Map<String, String[]> queryString, String hubName) {
        this.connectionId = connectionId;
        this.queryString = queryString;
        this.hubName = hubName;
        this.messageId = Optional.empty();
    }

    private RequestContext(UUID connectionId, long messageId, Map<String, String[]> queryString, String hubName) {
        this.connectionId = connectionId;
        this.messageId = Optional.of(messageId);
        this.queryString = queryString;
        this.hubName = hubName;
    }

    public RequestContext setMessageId(int messageId) {
        return new RequestContext(this.connectionId, messageId, this.queryString, this.hubName);
    }

    private Map<String, String[]> getQueryParams(Map<String, String[]> queryString) {
        return queryString.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
    }

    private String getHubName(String connectionData) {
        final JsonNode root = Json.parse(connectionData);
        return root.findValue("name").textValue();
    }

    @Override
    public String toString() {
        return "RequestContext{" +
                "connectionId=" + connectionId +
                ", messageId=" + messageId +
                ", queryString=" + queryString +
                ", hubName='" + hubName + '\'' +
                '}';
    }
}