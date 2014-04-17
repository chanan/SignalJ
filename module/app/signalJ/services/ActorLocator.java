package signalJ.services;
import play.libs.Akka;
import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.Props;

public class ActorLocator {
	private final static ActorRef signalJActor = Akka.system().actorOf(Props.create(SignalJActor.class), "signalJ");
	private final static ActorRef hubsActor = Akka.system().actorOf(Props.create(HubsActor.class), "hubs");
	
	public static ActorRef getSignalJActor() {
		return signalJActor;
	}
	public static ActorRef getHubsActor() {
		return hubsActor;
	}
}
