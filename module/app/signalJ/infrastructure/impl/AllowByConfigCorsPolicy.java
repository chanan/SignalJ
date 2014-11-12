package signalJ.infrastructure.impl;

import signalJ.SignalJPlugin;
import signalJ.infrastructure.CorsPolicy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AllowByConfigCorsPolicy implements CorsPolicy {
    private final List<String> domains;

    public AllowByConfigCorsPolicy() {
        final String allowedDomains = SignalJPlugin.getConfig().getString("SignalJ.cors.domains");
        if(allowedDomains == null) this.domains = new ArrayList<>();
        else this.domains = Arrays.asList(allowedDomains.split(","));
    }

    @Override
    public boolean isDomainAllowed(String domain) {
        return domains.stream().filter(d -> d.equalsIgnoreCase(domain)).findAny().isPresent();
    }
}