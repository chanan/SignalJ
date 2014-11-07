package signalJ.models;

import java.time.Duration;

public class Configuration {
    public final Duration keepAliveDuration;
    public final Duration disconnectDuration;
    public final Duration connectionDuration;

    public Configuration(Duration keepAliveDuration, Duration disconnectDuration, Duration connectionDuration) {
        this.keepAliveDuration = keepAliveDuration;
        this.disconnectDuration = disconnectDuration;
        this.connectionDuration = connectionDuration;
    }

    public int getKeepAliveTimeout() {
        return (int) keepAliveDuration.getSeconds();
    }

    public int getDisconnectTimeout() {
        return (int) disconnectDuration.getSeconds();
    }

    public int getConnectionTimeout() {
        return (int) connectionDuration.getSeconds();
    }
}