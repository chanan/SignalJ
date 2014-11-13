package signalJ.infrastructure.impl;

import play.mvc.Http;
import signalJ.infrastructure.UserIdProvider;

import java.util.Optional;

public class DefaultUserIdProvider implements UserIdProvider {
    @Override
    public Optional<String> getUserId(Http.Context request) {
        return Optional.empty();
    }
}