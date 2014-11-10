package signalJ.models;

import java.util.Map;
import java.util.UUID;

public interface TransportJoinMessage {
    public RequestContext getContext();
    public TransportType getTransportType();
}
