package actions;

import org.apache.commons.codec.binary.Base64;
import play.Logger;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

//Note: This basic auth allows everyone in
public class BasicAuthAction extends Action.Simple {
    private static final String AUTHORIZATION = "authorization";
    private static final String WWW_AUTHENTICATE = "WWW-Authenticate";
    private static final String REALM = "Basic realm=\"SignalJ\"";

    @Override
    public F.Promise<Result> call(Http.Context context) throws Throwable {
        String authHeader = context.request().getHeader(AUTHORIZATION);
        if (authHeader == null) {
            return unauthorized(context);
        }
        String auth = authHeader.split(" ")[1];
        String[] credString = new String(Base64.decodeBase64(auth)).split(":");

        if (credString == null || credString.length != 2) {
            return unauthorized(context);
        }

        final String userId = credString[0];

        Logger.debug("Username: " + userId);

        context.request().setUsername(userId); //This does not seem to work
        context.args.put("SignalJ_Username", userId);

        return delegate.call(context);
    }

    private F.Promise<Result> unauthorized(final Http.Context context) {
        context.response().setHeader(WWW_AUTHENTICATE, REALM);
        return F.Promise.pure((Result) unauthorized());
    }
}