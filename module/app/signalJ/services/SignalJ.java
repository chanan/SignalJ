package signalJ.services;

import akka.actor.ActorRef;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.Logger;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import signalJ.SignalJPlugin;
import signalJ.infrastructure.Cursor;
import signalJ.infrastructure.ProtectedData;
import signalJ.infrastructure.Purposes;
import signalJ.models.NegotiationResponse;
import signalJ.services.SignalJActor.Join;

import java.io.IOException;
import java.util.UUID;

import static akka.pattern.Patterns.ask;

//TODO Use Play 2.3 syntax
public class SignalJ extends Controller {
	private final ActorRef signalJActor = SignalJPlugin.getSignalJActor();
    private final ObjectMapper mapper = new ObjectMapper();
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
                    Join join = new SignalJActor.Join(out, in, uuid);
                    signalJActor.tell(join, ActorRef.noSender());
                    //sendUUID(out, join.uuid);
                } catch (Exception ex) {
                    Logger.error("Error creating websocket!", ex);
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

    public Promise<Result> hubs2() {
        return Promise.wrap(ask(signalJActor, new HubsActor.GetJavaScript2(), 1000)).map(new Function<Object, Result>() {
            @Override
            public Result apply(Object response) throws Throwable {
                return ok(response.toString());
            }
        });
    }
}