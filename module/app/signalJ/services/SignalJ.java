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
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import signalJ.GlobalHost;
import signalJ.SignalJPlugin;
import signalJ.infrastructure.ProtectedData;
import signalJ.infrastructure.Purposes;
import signalJ.infrastructure.UserIdProvider;
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
    private final UserIdProvider userIdProvider = GlobalHost.getDependencyResolver().getService(UserIdProvider.class);
    private final ProtectedData protectedData = GlobalHost.getDependencyResolver().getService(ProtectedData.class);

    public Result negotiate() throws Exception {
        final UUID connectionId = UUID.randomUUID();
        final Configuration config = SignalJPlugin.getConfiguration();
        final String userId = userIdProvider.getUserId(ctx()).orElse("");
        final String connectionToken = protectedData.protect(String.format("%s:%s", connectionId, userId), Purposes.ConnectionToken)
                .orElseThrow(() -> new Exception("Cannot create connection token"));
        final NegotiationResponse response = new NegotiationResponse("/signalj", connectionToken, connectionId,
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

    public WebSocket<JsonNode> reconnectWebsockets() {
        final RequestContext context = new RequestContext(request());
        return new WebSocket<JsonNode>() {
            public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out){
                try {
                    signalJActor.tell(new Messages.Reconnection(context), ActorRef.noSender());
                    signalJActor.tell(new Messages.ReconnectWebsocket(out, in, context), ActorRef.noSender());
                } catch (Exception ex) {
                    Logger.error("Error creating reconnecting websocket!", ex);
                }
            }
        };
    }

    public Result reconnectServerSentEvents() {
        final RequestContext context = new RequestContext(request());
        return ok(new EventSource() {
            @Override
            public void onConnected() {
                signalJActor.tell(new Messages.Reconnection(context), ActorRef.noSender());
                signalJActor.tell(new Messages.ReconnectServerSentEvents(this, context), ActorRef.noSender());
            }
        });
    }

    public Result reconnectLongPolling() {
        return TODO;
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

    @BodyParser.Of(value = BodyParser.Text.class)
    public Result abort() {
        final RequestContext context = new RequestContext(request());
        signalJActor.tell(new Messages.Abort(context), ActorRef.noSender());
        return ok();
    }
}