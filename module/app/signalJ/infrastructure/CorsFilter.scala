package signalJ.infrastructure

import controllers.Default
import play.api.Logger
import play.api.mvc.{Result, RequestHeader, Filter}
import play.api.http.HeaderNames._
import signalJ.GlobalHost

class CORSFilter extends Filter {

  import scala.concurrent._
  import ExecutionContext.Implicits.global

  val policy: CorsPolicy = GlobalHost.getDependencyResolver().getService(classOf[CorsPolicy]).asInstanceOf[CorsPolicy];

  def isPreFlight(r: RequestHeader) =(
    r.method.toLowerCase.equals("options")
      &&
      r.headers.get(ACCESS_CONTROL_REQUEST_METHOD).nonEmpty
    )

  def isOriginAllowed(request: RequestHeader) = {
    if(policy.isDomainAlllowed(request.headers.get(ORIGIN).getOrElse(""))) {
      request.headers.get(ORIGIN)
    } else {
      ""
    }
  }

  def headers(request: RequestHeader) = {
    isOriginAllowed(request).fold(Seq.empty[(String,String)])( a => Seq(
      ACCESS_CONTROL_ALLOW_ORIGIN -> a,
      ACCESS_CONTROL_ALLOW_METHODS -> request.headers.get(ACCESS_CONTROL_REQUEST_METHOD).getOrElse("*"),
      ACCESS_CONTROL_ALLOW_HEADERS -> request.headers.get(ACCESS_CONTROL_REQUEST_HEADERS).getOrElse(""),
      ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true"
    ))
  }

  def defaultPreFlightResponseHeaders = headers _

  def isCORS(request: RequestHeader) = request.headers.get(ORIGIN).fold(false)(_ => true)

  def apply(f: (RequestHeader) => Future[Result])(request: RequestHeader): Future[Result] = {
    Logger.trace("[cors] filtering request to add cors")
    if (isCORS(request)) {
      if (isPreFlight(request)) {
        Logger.trace("[cors] request is preflight")
        Future.successful(Default.Ok.withHeaders(headers(request):_*))
      } else {
        Logger.trace("[cors] request is normal")
        f(request).map{_.withHeaders(defaultPreFlightResponseHeaders(request):_*)}
      }
    } else {
      f(request)
    }
  }
}