package signalJ.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class NegotiationResponse {
    @JsonProperty("Url")
    public final String url;
    @JsonProperty("ConnectionToken")
    public final String connectionToken;
    @JsonProperty("ConnectionId")
    public final UUID connectionId;
    @JsonProperty("KeepAliveTimeout")
    public final int keepAliveTimeout;
    @JsonProperty("DisconnectTimeout")
    public final int disconnectTimeout;
    @JsonProperty("ConnectionTimeout")
    public final int connectionTimeout;
    @JsonProperty("TryWebSockets")
    public final boolean tryWebSockets;
    @JsonProperty("ProtocolVersion")
    public final String protocolVersion;
    @JsonProperty("TransportConnectTimeout")
    public final int transportConnectTimeout;
    @JsonProperty("LongPollDelay")
    public final int longPollDelay;

    public NegotiationResponse(String url, String connectionToken, UUID connectionId, int keepAliveTimeout, int disconnectTimeout, int connectionTimeout, boolean tryWebSockets, String protocolVersion, int transportConnectTimeout, int longPollDelay) {
        this.url = url;
        this.connectionToken = connectionToken;
        this.connectionId = connectionId;
        this.keepAliveTimeout = keepAliveTimeout;
        this.disconnectTimeout = disconnectTimeout;
        this.connectionTimeout = connectionTimeout;
        this.tryWebSockets = tryWebSockets;
        this.protocolVersion = protocolVersion;
        this.transportConnectTimeout = transportConnectTimeout;
        this.longPollDelay = longPollDelay;
    }
}