package signalJ.services;

import akka.actor.ActorRef;
import signalJ.models.Messages;

import java.util.UUID;

public class GroupsContext {
    private final String hubname;
	private final ActorRef signalJActor;

    public GroupsContext(String className, ActorRef signalJActor) {
        this.hubname = className;
        this.signalJActor = signalJActor;
    }

    public void add(UUID connectionId, String groupName) {
		signalJActor.tell(new Messages.GroupJoin(hubname, groupName, connectionId), ActorRef.noSender());
	}
	
	public void remove(UUID connectionId, String groupName) {
		signalJActor.tell(new Messages.GroupLeave(hubname, groupName, connectionId), ActorRef.noSender());
	}
}