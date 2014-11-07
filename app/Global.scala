import com.google.inject.{Injector, Guice}
import hubs.GuiceDependencyResolver
import play.api.{Application, GlobalSettings}
import play.api.mvc.{Handler, RequestHeader}
import signalJ.{GlobalHost, TransportTransformer}

object Global extends GlobalSettings {
  val injector: Injector  = Guice.createInjector(new GuiceModule());

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    super.onRouteRequest(TransportTransformer.transform(request))
  }

  override def getControllerInstance[A](controllerClass: Class[A]): A = {
    injector.getInstance(controllerClass)
  }

  override def onStart(app: Application) {
    //This is an example of setting your own resolver
    val resolver: GuiceDependencyResolver = new GuiceDependencyResolver(injector);
    GlobalHost.setDependencyResolver(resolver);
  }
}