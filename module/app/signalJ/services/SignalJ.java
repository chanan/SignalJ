package signalJ.services;

import akka.actor.ActorRef;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.AssetsBuilder;
import play.Logger;
import play.api.mvc.Action;
import play.api.mvc.AnyContent;
import play.data.DynamicForm;
import play.data.Form;
import play.libs.EventSource;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import signalJ.SignalJPlugin;
import signalJ.models.Configuration;
import signalJ.models.Messages;
import signalJ.models.NegotiationResponse;
import signalJ.models.RequestContext;

import java.util.UUID;

import static akka.pattern.Patterns.ask;

public class SignalJ extends Controller {
	private final ActorRef signalJActor = SignalJPlugin.getSignalJActor();
    private final String startStringPayload = "{ \"Response\": \"started\" }";
    private final String pongStringPayload = "{ \"Response\": \"pong\" }";
    private final AssetsBuilder delegate = new AssetsBuilder();

    public Result negotiate() {
        final UUID connectionId = UUID.randomUUID();
        final Configuration config = SignalJPlugin.getConfiguration();
        final NegotiationResponse response = new NegotiationResponse("/signalj", connectionId + ":", connectionId,
                config.getKeepAliveTimeout(), config.getDisconnectTimeout(), config.getConnectionTimeout(),
                true, "1.4", 5, 0);
        return ok(Json.toJson(response));
    }

    public WebSocket<JsonNode> connectWebsockets() {
        final RequestContext context = new RequestContext(request());
        return new WebSocket<JsonNode>() {
            public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out){
                try {
                    Messages.JoinWebsocket join = new Messages.JoinWebsocket(out, in, context);
                    signalJActor.tell(join, ActorRef.noSender());
                } catch (Exception ex) {
                    Logger.error("Error creating websocket!", ex);
                }
            }
        };
    }

    public Result connectServerSentEvents() {
        final RequestContext context = new RequestContext(request());
        return ok(new EventSource() {
            @Override
            public void onConnected() {
                Messages.JoinServerSentEvents join = new Messages.JoinServerSentEvents(this, context);
                signalJActor.tell(join, ActorRef.noSender());
            }
        });
    }

    public Result connectLongPolling() {
        final RequestContext context = new RequestContext(request());
        Chunks<String> chunks = StringChunks.whenReady(out -> {
            Messages.JoinLongPolling join = new Messages.JoinLongPolling(out, context);
            signalJActor.tell(join, ActorRef.noSender());
        });
        response().setContentType("application/json");
        return ok(chunks);
    }

    //Default connect action when other transports aren't enabled in the Global object
    public WebSocket<JsonNode> connect() {
        return connectWebsockets();
    }

    public WebSocket<JsonNode> reconnect() {
        final RequestContext context = new RequestContext(request());
        signalJActor.tell(new Messages.Reconnection(context), ActorRef.noSender());
        return new WebSocket<JsonNode>() {
            public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out){
                try {
                    Messages.Reconnect reconnect = new Messages.Reconnect(out, in, context);
                    signalJActor.tell(reconnect, ActorRef.noSender());
                } catch (Exception ex) {
                    Logger.error("Error creating reconnecting websocket!", ex);
                }
            }
        };
    }

    public Result start() {
        final RequestContext context = new RequestContext(request());
        signalJActor.tell(new Messages.Connection(context), ActorRef.noSender());
        return ok(startStringPayload);
    }

    public Result ping() {
        return ok(pongStringPayload);
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

    public Action<AnyContent> script() {
        final String script = SignalJPlugin.isDev() ? "jquery.signalR-2.1.2.js" : "jquery.signalR-2.1.2.min.js";
        return delegate.at("/public", script, true);
    }

    public Result send() {
        final RequestContext context = new RequestContext(request());
        final DynamicForm requestData = Form.form().bindFromRequest();
        final JsonNode data = Json.parse(requestData.get("data"));
        if(request().getQueryString("transport").equalsIgnoreCase("longPolling")) {
            Chunks<String> chunks = StringChunks.whenReady(out -> {
                signalJActor.tell(new Messages.Execute(out, context, data), ActorRef.noSender());
            });
            response().setContentType("application/json");
            return ok(chunks);
        } else {
            signalJActor.tell(new Messages.Execute(context, data), ActorRef.noSender());
            return ok();
        }
    }

    public Result poll() {
        final RequestContext context = new RequestContext(request());
        Chunks<String> chunks = StringChunks.whenReady(out -> {
            Messages.PollForMessages poll = new Messages.PollForMessages(out, context);
            signalJActor.tell(poll, ActorRef.noSender());
        });
        response().setContentType("application/json");
        return ok(chunks);
    }

    public Result abort() {
        return TODO;
    }
}