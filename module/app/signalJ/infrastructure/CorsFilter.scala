package signalJ.infrastructure

import controllers.Default
import play.api.Logger
import play.api.mvc.{Result, RequestHeader, Filter}
import play.api.http.HeaderNames._
import signalJ.GlobalHost
import signalJ.infrastructure.impl.DisallowAllCorsPolicy

class CORSFilter extends Filter {

  import scala.concurrent._
  import ExecutionContext.Implicits.global

  val policy: CorsPolicy = getPolicy

  private def getPolicy() = {
    if(GlobalHost.getDependencyResolver().getService(classOf[CorsPolicy]) != null)
      GlobalHost.getDependencyResolver().getService(classOf[CorsPolicy]).asInstanceOf[CorsPolicy]
    else
      new DisallowAllCorsPolicy
  }

  def isPreFlight(r: RequestHeader) =(
    r.method.toLowerCase.equals("options")
      &&
      r.headers.get(ACCESS_CONTROL_REQUEST_METHOD).nonEmpty
    )

  def isOriginAllowed(request: RequestHeader) = {
    request.headers.get(ORIGIN)
    .flatMap(o => if (policy.isDomainAllowed(request.headers.get(ORIGIN).getOrElse(""))) request.headers.get(ORIGIN) else None)
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
    if (isCORS(request)) {
      if (isPreFlight(request)) {
        Future.successful(Default.Ok.withHeaders(headers(request):_*))
      } else {
        f(request).map{_.withHeaders(defaultPreFlightResponseHeaders(request):_*)}
      }
    } else {
      f(request)
    }
  }
}