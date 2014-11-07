package signalJ;

import play.GlobalSettings;
import play.Logger;
import play.api.mvc.Handler;
import play.mvc.Http;

public class SignalJGlobal extends GlobalSettings {
    @Override
    public Handler onRouteRequest(Http.RequestHeader requestHeader) {
        Logger.info("Uri:" + requestHeader.uri());
        final Http.RequestHeader newHeader = JavaTransportTransformer.Transform(requestHeader);
        if(!requestHeader.uri().equalsIgnoreCase(newHeader.uri())) Logger.info("New Uri:" + newHeader.uri());
        return super.onRouteRequest(newHeader);
    }
}