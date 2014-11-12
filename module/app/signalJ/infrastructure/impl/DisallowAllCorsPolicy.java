package signalJ.infrastructure.impl;

import signalJ.infrastructure.CorsPolicy;

public class DisallowAllCorsPolicy implements CorsPolicy {
    @Override
    public boolean isDomainAllowed(String domain) {
        return false;
    }
}