package signalJ.infrastructure;

public interface CorsPolicy {
    boolean isDomainAlllowed(String domain);
}