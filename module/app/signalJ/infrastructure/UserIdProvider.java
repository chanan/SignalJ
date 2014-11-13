package signalJ.infrastructure;

import play.mvc.Http;

import java.util.Optional;

public interface UserIdProvider {
    Optional<String> getUserId(Http.Request request);
}
