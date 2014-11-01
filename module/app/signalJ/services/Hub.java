package signalJ.services;
import akka.actor.ActorRef;
import play.Logger;
import signalJ.models.RequestContext;

import java.util.UUID;

public abstract class Hub<T> implements HubContext<T> {
	private RequestContext context;
	private String className = null;
    private ActorRef signalJActor;

    protected abstract Class<T> getInterface();
	
	public void setHubClassName(String className) {
		if(this.className == null) this.className = className;
	}
	
	@Override
	public ClientsContext<T> clients() {
        Logger.debug("Hub: " + className);
        return new ClientsContext<T>(getInterface(), className, context, signalJActor);
	}
	
	@Override
	public GroupsContext groups() {
		return new GroupsContext(signalJActor);
	}

    public void setSignalJActor(ActorRef signalJActor) {
        this.signalJActor = signalJActor;
    }

    @Override
    public RequestContext context() {
        return context;
    }

    public void setContext(RequestContext context) {
        if(this.context == null) this.context = context;
    }
}