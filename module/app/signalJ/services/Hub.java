package signalJ.services;
import akka.actor.ActorRef;
import play.Logger;
import signalJ.models.RequestContext;

import java.util.Map;
import java.util.UUID;

public abstract class Hub<T> implements HubContext<T> {
	private RequestContext context;
	private String className = null;
    private ActorRef signalJActor;
    private Map<String, String> callerState;

    protected abstract Class<T> getInterface();
	
	public void setHubClassName(String className) {
		if(this.className == null) this.className = className;
	}
	
	@Override
	public ClientsContext<T> clients() {
        Logger.debug("Hub: " + className);
        return new ClientsContext<T>(getInterface(), className, context, signalJActor, callerState);
	}
	
	@Override
	public GroupsContext groups() {
		return new GroupsContext(className, signalJActor);
	}

    public void setSignalJActor(ActorRef signalJActor) {
        this.signalJActor = signalJActor;
    }

    @Override
    public RequestContext context() {
        return context;
    }

    void setContext(RequestContext context) {
        if(this.context == null) this.context = context;
    }

    void setCallerState(Map<String, String> callerState) {
        this.callerState = callerState;
    }
}