package signalJ.models;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.Json;
import play.mvc.Http;
import signalJ.GlobalHost;
import signalJ.infrastructure.ProtectedData;
import signalJ.infrastructure.Purposes;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class RequestContext {
    public final UUID connectionId;
    public final String username;
    public final Optional<Long> messageId;
    public final Map<String, String[]> queryString;
    public final String hubName;

    public RequestContext(Http.Request request) {
        final ProtectedData protectedData = GlobalHost.getDependencyResolver().getService(ProtectedData.class);
        final String connectionToken = request.getQueryString("connectionToken");
        final String decryptedToken = protectedData.unprotect(connectionToken, Purposes.ConnectionToken).get();
        connectionId = UUID.fromString(decryptedToken.substring(0, decryptedToken.lastIndexOf(':')));
        username = decryptedToken.substring(decryptedToken.indexOf(':') + 1);
        final String connectionData = request.getQueryString("connectionData");
        hubName = getHubName(connectionData);
        queryString = getQueryParams(request.queryString());
        messageId = Optional.empty();
    }

    public RequestContext(UUID connectionId, String username, Map<String, String[]> queryString, String hubName) {
        this.connectionId = connectionId;
        this.username = username;
        this.queryString = queryString;
        this.hubName = hubName;
        this.messageId = Optional.empty();
    }

    private RequestContext(UUID connectionId, String username, long messageId, Map<String, String[]> queryString, String hubName) {
        this.connectionId = connectionId;
        this.username = username;
        this.messageId = Optional.of(messageId);
        this.queryString = queryString;
        this.hubName = hubName;
    }

    public RequestContext setMessageId(int messageId) {
        return new RequestContext(this.connectionId, username, messageId, this.queryString, this.hubName);
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
                ", username='" + username + '\'' +
                ", messageId=" + messageId +
                ", queryString=" + queryString +
                ", hubName='" + hubName + '\'' +
                '}';
    }
}