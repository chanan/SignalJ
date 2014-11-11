package signalJ.models;

public interface TransportReconnectMessage {
    public RequestContext getContext();
    public TransportType getTransportType();
}