package controllers;
import java.util.UUID;

import play.Logger;
import play.libs.Json;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import services.SignalJActor;
import services.SignalJActor.Join;
import akka.actor.ActorRef;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import static akka.pattern.Patterns.ask;
import play.libs.F.Function;

public class SignalJ extends Controller {
	private final ActorRef signalJActor;
	private final ActorRef hubsActor;
	
	@Inject
	public SignalJ(@Named("SignalJActor") ActorRef signalJActor, @Named("HubsActor") ActorRef hubsActor) {
		this.signalJActor = signalJActor;
		this.hubsActor = hubsActor;
	}
	
	public Result js() {
		return ok(views.js.playSockets.render());
	}
	
	public Promise<Result> hubs() {
		return Promise.wrap(ask(hubsActor, "Get", 1000)).map(new Function<Object, Result>(){
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
	
	private void sendUUID(WebSocket.Out<JsonNode> out, UUID uuid) {
		final ObjectNode event = Json.newObject();
		event.put("uuid", uuid.toString());
		event.put("type", "init");
		out.write(event);
	}
}