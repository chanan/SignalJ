package signalJ.infrastructure;

public interface CorsPolicy {
    boolean isDomainAllowed(String domain);
}