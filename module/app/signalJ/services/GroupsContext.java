package signalJ.services;

import akka.actor.ActorRef;

import java.util.UUID;

public class GroupsContext {
	private final ActorRef signalJActor;

    public GroupsContext(ActorRef signalJActor) {
        this.signalJActor = signalJActor;
    }

    public void add(UUID connectionId, String groupName) {
		signalJActor.tell(new SignalJActor.GroupJoin(groupName, connectionId), ActorRef.noSender());
	}
	
	public void remove(UUID connectionId, String groupName) {
		signalJActor.tell(new SignalJActor.GroupLeave(groupName, connectionId), ActorRef.noSender());
	}
}