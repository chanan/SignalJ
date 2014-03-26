package controllers;

import play.Logger;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import services.SignalJActor;
import services.SignalJActor.Join;
import akka.actor.ActorRef;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class PlaySockets extends Controller {
	private final ActorRef signalJActor;
	
	@Inject
	public PlaySockets(@Named("SignalJActor") ActorRef signalJActor) {
		this.signalJActor = signalJActor;
	}
	
	public Result js() {
		return ok(views.js.playSockets.render());
	}
	
	public WebSocket<JsonNode> join() {
		return new WebSocket<JsonNode>() {
            public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out){
                try {
                	Join join = new SignalJActor.Join(out, in);
                	signalJActor.tell(join, null);
                	signalJActor.tell(new SignalJActor.Send(join.uuid, join.uuid.toString()), null);
                } catch (Exception ex) {
                    Logger.error("Error creating websocket!", ex);
                }
            }
        };
	}
}