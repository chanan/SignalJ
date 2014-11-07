package signalJ

import play.api.GlobalSettings
import play.api.mvc.{Handler, RequestHeader}

/**
 * Can be used by the app to enable ServerSentEvents or LongPolling
 *
 * Another option if you already have a Global in your project, is to call TransportTransformer in your own Global
 */
object SignalJGlobal extends GlobalSettings {
  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    super.onRouteRequest(TransportTransformer.transform(request))
  }
}
