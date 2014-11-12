package signalJ.infrastructure.impl;

import signalJ.infrastructure.CorsPolicy;

public class AllowAllCorsPolicy implements CorsPolicy {
    @Override
    public boolean isDomainAllowed(String domain) {
        return true;
    }
}
