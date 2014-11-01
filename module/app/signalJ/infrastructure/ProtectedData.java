package signalJ.infrastructure;

import java.util.Optional;

public interface ProtectedData {
    public Optional<String> protect(String data, String purpose);
    public Optional<String> unprotect(String protectedValue, String purpose);
}