package signalJ.services;

import akka.actor.ActorRef;
import signalJ.models.Messages;

import java.util.UUID;

public class GroupsContext {
	private final ActorRef signalJActor;

    public GroupsContext(ActorRef signalJActor) {
        this.signalJActor = signalJActor;
    }

    public void add(UUID connectionId, String groupName) {
		signalJActor.tell(new Messages.GroupJoin(groupName, connectionId), ActorRef.noSender());
	}
	
	public void remove(UUID connectionId, String groupName) {
		signalJActor.tell(new Messages.GroupLeave(groupName, connectionId), ActorRef.noSender());
	}
}