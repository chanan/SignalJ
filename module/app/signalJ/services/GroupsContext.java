package signalJ.services;
import java.util.UUID;

import akka.actor.ActorRef;

public class GroupsContext {
	private final ActorRef signalJActor = ActorLocator.getSignalJActor();
	public void add(UUID connectionId, String groupName) {
		signalJActor.tell(new SignalJActor.GroupJoin(groupName, connectionId), ActorRef.noSender());
	}
	
	public void remove(UUID connectionId, String groupName) {
		signalJActor.tell(new SignalJActor.GroupLeave(groupName, connectionId), ActorRef.noSender());
	}
}