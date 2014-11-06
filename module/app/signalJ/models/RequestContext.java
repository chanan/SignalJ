package signalJ.models;

import java.util.Map;
import java.util.UUID;

public class RequestContext {
    public final UUID connectionId;
    public final int messageId;
    public final Map<String, String[]> queryString;

    public RequestContext(UUID connectionId, int messageId, Map<String, String[]> queryString) {
        this.connectionId = connectionId;
        this.messageId = messageId;
        this.queryString = queryString;
    }

    @Override
    public String toString() {
        return "RequestContext{" +
                "connectionId=" + connectionId +
                ", messageId=" + messageId +
                ", queryString=" + queryString +
                '}';
    }
}