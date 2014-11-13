package signalJ.infrastructure.impl;

import play.mvc.Http;
import signalJ.infrastructure.UserIdProvider;

import java.util.Optional;

public class DefaultUserIdProvider implements UserIdProvider {
    @Override
    public Optional<String> getUserId(Http.Request request) {
        return request.username() != null ? Optional.of(request.username()) : Optional.empty();
    }
}
