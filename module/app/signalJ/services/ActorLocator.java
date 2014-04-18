package signalJ.services;
import play.libs.Akka;
import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.Props;

public class ActorLocator {
	private final static ActorRef signalJActor = Akka.system().actorOf(Props.create(SignalJActor.class), "signalJ");
	
	public static ActorRef getSignalJActor() {
		return signalJActor;
	}
}
