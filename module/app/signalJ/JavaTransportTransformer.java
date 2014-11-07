package signalJ;

import play.Logger;
import play.api.http.MediaRange;
import play.i18n.Lang;
import play.mvc.Http;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JavaTransportTransformer {
    public static Http.RequestHeader Transform(Http.RequestHeader requestHeader) {
        if(requestHeader.path().toLowerCase().endsWith("/connect") && requestHeader.getQueryString("transport") != null) {
            final String transportPath = requestHeader.path() + "/" + requestHeader.getQueryString("transport");
            Logger.debug(String.format("Transforming: %s to: %s", requestHeader.path(), transportPath));
            return createRequestHeader(requestHeader, transportPath);
        }
        return requestHeader;

    }

    private static Http.RequestHeader createRequestHeader(Http.RequestHeader requestHeader, String transportPath) {
        return new Http.RequestHeader() {
            @Override
            public String uri() {
                return transportPath + "?" + encodeAsQueryString(requestHeader.queryString());
            }

            @Override
            public String method() {
                return requestHeader.method();
            }

            @Override
            public String version() {
                return requestHeader.version();
            }

            @Override
            public String remoteAddress() {
                return requestHeader.remoteAddress();
            }

            @Override
            public boolean secure() {
                return requestHeader.secure();
            }

            @Override
            public String host() {
                return requestHeader.host();
            }

            @Override
            public String path() {
                return transportPath;
            }

            @Override
            public List<Lang> acceptLanguages() {
                return requestHeader.acceptLanguages();
            }

            @Override
            public List<String> accept() {
                return requestHeader.accept();
            }

            @Override
            public List<MediaRange> acceptedTypes() {
                return requestHeader.acceptedTypes();
            }

            @Override
            public boolean accepts(String s) {
                return requestHeader.accepts(s);
            }

            @Override
            public Map<String, String[]> queryString() {
                return requestHeader.queryString();
            }

            @Override
            public Http.Cookies cookies() {
                return requestHeader.cookies();
            }

            @Override
            public Map<String, String[]> headers() {
                return requestHeader.headers();
            }
        };
    }

    private static String encodeAsQueryString(Map<String, String[]> queryString) {
        final String qs = queryString.entrySet().stream().map(entry ->
            Arrays.asList(entry.getValue()).stream().map(v -> entry.getKey() + "=" + v).collect(Collectors.joining("&"))
        ).collect(Collectors.joining("&"));
        Logger.debug("qs: " + qs);
        return qs;
    }
}
