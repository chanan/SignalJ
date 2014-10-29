package signalJ.services;
import akka.actor.ActorRef;
import play.Logger;

import java.util.UUID;

public abstract class Hub<T> implements HubContext<T> {
	private UUID uuid;
	private String className = null;
    private ActorRef signalJActor;

    protected abstract Class<T> getInterface();
	
	void setConnectionId(UUID uuid) {
		this.uuid = uuid;
	}
	
	public UUID getConnectionId() {
		return uuid;
	}
	
	public void setHubClassName(String className) {
		if(this.className == null) this.className = className;
        Logger.debug("Hub Classname: " + className);
	}
	
	@Override
	public ClientsContext<T> clients() {
        Logger.debug("Hub: " + className);
        return new ClientsContext<T>(getInterface(), className, getConnectionId(), signalJActor);
	}
	
	@Override
	public GroupsContext groups() {
		return new GroupsContext(signalJActor);
	}

    public void setSignalJActor(ActorRef signalJActor) {
        this.signalJActor = signalJActor;
    }
}