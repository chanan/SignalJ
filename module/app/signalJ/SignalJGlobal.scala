package signalJ

import play.GlobalSettings
import play.api.Play
import play.api.mvc.Handler
import play.mvc.Http

/**
 * Can be used by the app to enable ServerSentEvents or LongPolling
 *
 * Another option if you already have a Global in your project, is to call TransportTransformer in your own Global
 */
class SignalJGlobal extends GlobalSettings {
  /**
   * Reroute requests with paths like "/connect?transport=webSockets" to "/connect/webSockets".
   */
  override def onRouteRequest(request: Http.RequestHeader): Handler = {
    val newRequest = TransportTransformer.transform(request)
      (Play.maybeApplication flatMap { app =>
        app.routes flatMap { router =>
          router.handlerFor(newRequest)
        }
      }).orNull
  }
}