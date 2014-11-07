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

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
                true, "1.4", 20, 20);
        return ok(Json.toJson(response));
    }

    public WebSocket<JsonNode> connectWebsockets() {
        final String connectionToken = request().getQueryString("connectionToken");
        final UUID uuid = UUID.fromString(connectionToken.substring(0, connectionToken.lastIndexOf(':')));
        final String connectionData = request().getQueryString("connectionData");
        final Map<String, String[]> queryString = getQueryParams(request().queryString());
        return new WebSocket<JsonNode>() {
            public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out){
                try {
                    Messages.JoinWebsocket join = new Messages.JoinWebsocket(out, in, uuid, getHubName(connectionData), queryString);
                    signalJActor.tell(join, ActorRef.noSender());
                } catch (Exception ex) {
                    Logger.error("Error creating websocket!", ex);
                }
            }
        };
    }

    public Result connectServerSentEvents() {
        final String connectionToken = request().getQueryString("connectionToken");
        final UUID uuid = UUID.fromString(connectionToken.substring(0, connectionToken.lastIndexOf(':')));
        final String connectionData = request().getQueryString("connectionData");
        final Map<String, String[]> queryString = getQueryParams(request().queryString());
        return ok(new EventSource() {
            @Override
            public void onConnected() {
                Messages.JoinServerSentEvents join = new Messages.JoinServerSentEvents(this, uuid, getHubName(connectionData), queryString);
                signalJActor.tell(join, ActorRef.noSender());
            }
        });
    }

    public Result connectLongPolling() {
        Logger.info("Long Polling Connection!");
        return TODO;
    }

    //Default connect action when other transports aren't enabled in the Global object
    public WebSocket<JsonNode> connect() {
        return connectWebsockets();
    }

    public WebSocket<JsonNode> reconnect() {
        final String connectionToken = request().getQueryString("connectionToken");
        final UUID uuid = UUID.fromString(connectionToken.substring(0, connectionToken.lastIndexOf(':')));
        final String connectionData = request().getQueryString("connectionData");
        final Map<String, String[]> queryString = getQueryParams(request().queryString());
        signalJActor.tell(new Messages.Reconnection(uuid, getHubName(connectionData), queryString), ActorRef.noSender());
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
        final String connectionToken = request().getQueryString("connectionToken");
        final UUID uuid = UUID.fromString(connectionToken.substring(0, connectionToken.lastIndexOf(':')));
        final String connectionData = request().getQueryString("connectionData");
        final Map<String, String[]> queryString = getQueryParams(request().queryString());
        signalJActor.tell(new Messages.Connection(uuid, getHubName(connectionData), queryString), ActorRef.noSender());
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

    //http://localhost:9000/signalj/send?transport=serverSentEvents&clientProtocol=1.4&SomeName=SomeValue&connectionToken=392b5561-b0be-45d3-8d08-ecab9952188f%3A&connectionData=%5B%7B%22name%22%3A%22test%22%7D%5D
    public Result send() {
        final String connectionToken = request().getQueryString("connectionToken");
        final UUID uuid = UUID.fromString(connectionToken.substring(0, connectionToken.lastIndexOf(':')));
        final String connectionData = request().getQueryString("connectionData");
        final Map<String, String[]> queryString = getQueryParams(request().queryString());
        final DynamicForm requestData = Form.form().bindFromRequest();
        final JsonNode data = Json.parse(requestData.get("data"));
        signalJActor.tell(new Messages.Execute(uuid, data, queryString), ActorRef.noSender());
        return ok();
    }

    private Map<String, String[]> getQueryParams(Map<String, String[]> queryString) {
        return queryString.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
    }

    private String getHubName(String connectionData) {
        final JsonNode root = Json.parse(connectionData);
        return root.findValue("name").textValue();
    }
}