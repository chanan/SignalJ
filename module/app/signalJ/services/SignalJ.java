package signalJ.services;

import akka.actor.ActorRef;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.AssetsBuilder;
import play.Logger;
import play.api.mvc.Action;
import play.api.mvc.AnyContent;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import signalJ.SignalJPlugin;
import signalJ.models.Messages;
import signalJ.models.NegotiationResponse;

import java.io.File;
import java.util.UUID;

import static akka.pattern.Patterns.ask;

//TODO Use Play 2.3 syntax
public class SignalJ extends Controller {
	private final ActorRef signalJActor = SignalJPlugin.getSignalJActor();
    private final String startStringPayload = "{ \"Response\": \"started\" }";
    private final String pongStringPayload = "{ \"Response\": \"pong\" }";

    public Result negotiate() {
        final UUID connectionId = UUID.randomUUID();
        final NegotiationResponse response = new NegotiationResponse("/signalj", connectionId + ":", connectionId, 20, 20, 20, true, "1.4", 20, 20);
        return ok(Json.toJson(response));
    }

    public WebSocket<JsonNode> connect() {
        final String connectionToken = request().getQueryString("connectionToken");
        final UUID uuid = UUID.fromString(connectionToken.substring(0, connectionToken.lastIndexOf(':')));
        return new WebSocket<JsonNode>() {
            public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out){
                try {
                    Messages.Join join = new Messages.Join(out, in, uuid);
                    signalJActor.tell(join, ActorRef.noSender());
                } catch (Exception ex) {
                    Logger.error("Error creating websocket!", ex);
                }
            }
        };
    }

    public WebSocket<JsonNode> reconnect() {
        final String connectionToken = request().getQueryString("connectionToken");
        final UUID uuid = UUID.fromString(connectionToken.substring(0, connectionToken.lastIndexOf(':')));
        return new WebSocket<JsonNode>() {
            public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out){
                try {
                    Messages.Reconnect reconnect = new Messages.Reconnect(out, in, uuid);
                    signalJActor.tell(reconnect, ActorRef.noSender());
                } catch (Exception ex) {
                    Logger.error("Error creating reconnecting websocket!", ex);
                }
            }
        };
    }

    public Result start() {
        return ok(startStringPayload);
    }

    public Result ping() {
        return ok (pongStringPayload);
    }

    public Promise<Result> hubs() {
        response().setContentType("application/javascript");
        return Promise.wrap(ask(signalJActor, new Messages.GetJavaScript(), 5000)).map(new Function<Object, Result>() {
            @Override
            public Result apply(Object response) throws Throwable {
                return ok(response.toString());
            }
        });
    }

    private static AssetsBuilder delegate = new AssetsBuilder();

    public Action<AnyContent> script() {
        final String script = SignalJPlugin.isDev() ? "jquery.signalR-2.1.2.js" : "jquery.signalR-2.1.2.min.js";
        return delegate.at("/public", script, true);
    }
}