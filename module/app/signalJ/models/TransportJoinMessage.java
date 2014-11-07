package signalJ.models;

import java.util.Map;
import java.util.UUID;

public interface TransportJoinMessage {
    public UUID getConnectionId();
    public String getHubName();
    public Map<String, String[]> getQueryString();
    public TransportType getTransportType();
}
