package signalJ.models;

import java.util.UUID;

public class RequestContext {
    public final UUID connectionId;
    public final int messageId;

    public RequestContext(UUID connectionId, int messageId) {
        this.connectionId = connectionId;
        this.messageId = messageId;
    }
}