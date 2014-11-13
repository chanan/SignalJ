import com.google.inject.{Guice, Injector}
import hubs.GuiceDependencyResolver
import play.api.mvc.{EssentialAction, Filters, Handler, RequestHeader}
import play.api.{Application, GlobalSettings}
import signalJ.infrastructure.CORSFilter
import signalJ.{GlobalHost, TransportTransformer}

object Global extends GlobalSettings {
  val injector: Injector  = Guice.createInjector(new GuiceModule())

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    super.onRouteRequest(TransportTransformer.transform(request))
  }

  override def getControllerInstance[A](controllerClass: Class[A]): A = {
    injector.getInstance(controllerClass)
  }

  override def onStart(app: Application) {
    //This is an example of setting your own resolver
    val resolver: GuiceDependencyResolver = new GuiceDependencyResolver(injector)
    GlobalHost.setDependencyResolver(resolver)
    /*val policy: CorsPolicy = new AllowAllCorsPolicy
    GlobalHost.getDependencyResolver.Register(classOf[CorsPolicy], () -> policy)*/
  }

  override def doFilter(next: EssentialAction): EssentialAction = {
    Filters(super.doFilter(next), CORSFilter)
  }
}