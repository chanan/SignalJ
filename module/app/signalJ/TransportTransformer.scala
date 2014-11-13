package signalJ

import play.api.mvc.{Headers, RequestHeader}
import play.mvc.Http

object TransportTransformer {
  def transform(request: RequestHeader): RequestHeader = {
    if(request.path.toLowerCase().endsWith("/connect") && request.getQueryString("transport") != null) {
      val maybeTransport = request.getQueryString("transport")
      val transportPath = maybeTransport.fold(request.path) { transport => request.path + "/" + transport}
      return request.copy(path = transportPath)
    } else if(request.path.toLowerCase().endsWith("/reconnect") && request.getQueryString("transport") != null) {
      val maybeTransport = request.getQueryString("transport")
      val transportPath = maybeTransport.fold(request.path) { transport => request.path + "/" + transport}
      return request.copy(path = transportPath)
    }
    request
  }

  def transform(request: Http.RequestHeader): RequestHeader = {
    if(request.path.toLowerCase().endsWith("/connect") && request.getQueryString("transport") != null) {
      val transport = request.getQueryString("transport")
      return convertRequest(request, request.path + "/" + transport)
    } else if(request.path.toLowerCase().endsWith("/reconnect") && request.getQueryString("transport") != null) {
      val transport = request.getQueryString("transport")
      return convertRequest(request, request.path + "/" + transport)
    }
    return convertRequest(request, request.path())
  }

  /**
   * Convert Java RequestHeader to Scala RequestHeader, with a new path.
   * Only converts enough to make it possible to reroute this request.
   */
  private def convertRequest(request: Http.RequestHeader, newPath: String): RequestHeader = {
    new RequestHeader {
      val id = 0L
      val tags = Map.empty[String, String]
      val uri = newPath
      val path = newPath
      val method = request.method
      val version = request.version
      val queryString = Map.empty[String, Seq[String]]
      val headers = new Headers { val data = Seq.empty }
      val remoteAddress = request.remoteAddress
      val secure = request.secure
    }
  }
}