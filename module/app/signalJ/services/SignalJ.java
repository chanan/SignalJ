package signalJ.services;
import static akka.pattern.Patterns.ask;

import java.util.UUID;

import play.Logger;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import signalJ.services.SignalJActor.Join;
import akka.actor.ActorRef;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SignalJ extends Controller {
	private final static ActorRef signalJActor = ActorLocator.getSignalJActor();
	private final static ActorRef hubsActor = ActorLocator.getHubsActor();
	
	public static Promise<Result> hubs() {
		return Promise.wrap(ask(hubsActor, new HubsActor.GetJavaScript(), 1000)).map(new Function<Object, Result>(){
			@Override
			public Result apply(Object response) throws Throwable {
				return ok(response.toString());
			}
		});
	}
	
	public static WebSocket<JsonNode> join() {
		return new WebSocket<JsonNode>() {
            public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out){
                try {
                	Join join = new SignalJActor.Join(out, in);
                	signalJActor.tell(join, null);
                	sendUUID(out, join.uuid);
                } catch (Exception ex) {
                    Logger.error("Error creating websocket!", ex);
                }
            }
        };
	}
	
	//TODO: Convert to serialization
	private static void sendUUID(WebSocket.Out<JsonNode> out, UUID uuid) {
		final ObjectNode event = Json.newObject();
		event.put("uuid", uuid.toString());
		event.put("type", "init");
		out.write(event);
	}
}