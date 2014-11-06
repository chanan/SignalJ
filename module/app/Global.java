import play.GlobalSettings;
import play.Logger;
import play.api.mvc.Handler;
import play.mvc.Http;

public class Global extends GlobalSettings {
    @Override
    public Handler onRouteRequest(Http.RequestHeader requestHeader) {
        Logger.info("Uri:" + requestHeader.uri());
        return super.onRouteRequest(requestHeader);
    }
}
