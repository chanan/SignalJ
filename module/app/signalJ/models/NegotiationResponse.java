package signalJ.models;

import java.util.UUID;

public class NegotiationResponse {
    public final String Url;
    public final String ConnectionToken;
    public final UUID ConnectionId;
    public final int KeepAliveTimeout;
    public final int DisconnectTimeout;
    public final int ConnectionTimeout;
    public final boolean TryWebSockets;
    public final String ProtocolVersion;
    public final int TransportConnectTimeout;
    public final int LongPollDelay;

    public NegotiationResponse(String url, String connectionToken, UUID connectionId, int keepAliveTimeout, int disconnectTimeout, int connectionTimeout, boolean tryWebSockets, String protocolVersion, int transportConnectTimeout, int longPollDelay) {
        Url = url;
        ConnectionToken = connectionToken;
        ConnectionId = connectionId;
        KeepAliveTimeout = keepAliveTimeout;
        DisconnectTimeout = disconnectTimeout;
        ConnectionTimeout = connectionTimeout;
        TryWebSockets = tryWebSockets;
        ProtocolVersion = protocolVersion;
        TransportConnectTimeout = transportConnectTimeout;
        LongPollDelay = longPollDelay;
    }
}
