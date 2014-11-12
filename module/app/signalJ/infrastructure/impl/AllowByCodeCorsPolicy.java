package signalJ.infrastructure.impl;

import signalJ.infrastructure.CorsPolicy;

import java.util.Arrays;
import java.util.List;

public class AllowByCodeCorsPolicy implements CorsPolicy {
    private final List<String> domains;

    public AllowByCodeCorsPolicy(String... domains) {
        this.domains = Arrays.asList(domains);
    }

    @Override
    public boolean isDomainAllowed(String domain) {
        return domains.stream().filter(d -> d.equalsIgnoreCase(domain)).findAny().isPresent();
    }
}