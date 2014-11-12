package signalJ.infrastructure.impl;

import signalJ.infrastructure.CorsPolicy;

public class DefaultCorsPolicy implements CorsPolicy {
    @Override
    public boolean isDomainAlllowed(String domain) {
        return true;
    }
}