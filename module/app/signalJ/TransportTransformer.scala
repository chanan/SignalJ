package signalJ

import play.api.mvc.RequestHeader
import play.mvc.Http.{RequestHeader => JavaHeader}

object TransportTransformer {
  def transform(request: RequestHeader): RequestHeader = {
    if(request.path.toLowerCase().endsWith("/connect") && request.getQueryString("transport") != null) {
      val maybeTransport = request.getQueryString("transport")
      val transportPath = maybeTransport.fold(request.path) { transport => request.path + "/" + transport}
      return request.copy(path = transportPath)
    }
    request
  }
}