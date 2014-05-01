package signalJ.services;

import akka.actor.ActorRef;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.Logger;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import signalJ.SignalJPlugin;
import signalJ.services.SignalJActor.Join;

import java.util.UUID;

import static akka.pattern.Patterns.ask;

public class SignalJ extends Controller {
	private final ActorRef signalJActor = SignalJPlugin.getSignalJActor();
	
	public Promise<Result> hubs() {
		return Promise.wrap(ask(signalJActor, new HubsActor.GetJavaScript(), 1000)).map(new Function<Object, Result>(){
			@Override
			public Result apply(Object response) throws Throwable {
				return ok(response.toString());
			}
		});
	}
	
	public WebSocket<JsonNode> join() {
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
	private void sendUUID(WebSocket.Out<JsonNode> out, UUID uuid) {
		final ObjectNode event = Json.newObject();
		event.put("uuid", uuid.toString());
		event.put("type", "init");
		out.write(event);
	}
}