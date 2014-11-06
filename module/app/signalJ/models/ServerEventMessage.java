package signalJ.models;

import java.util.Map;
import java.util.UUID;

public interface ServerEventMessage {
    public UUID getUuid();

    public String getHubName();

    public Map<String, String[]> getQueryString();
}