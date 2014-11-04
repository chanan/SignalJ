package signalJ.services;

import akka.actor.ActorRef;
import signalJ.models.CallerState;
import signalJ.models.Messages;
import signalJ.models.RequestContext;

import java.util.Map;
import java.util.UUID;

public class ClientsContext<S> {
	public final S all;
	public final S others;
	public final S caller;
    public final CallerState callerState;
	private final Class<S> clazz;
	private final RequestContext context;
    private final ActorRef signalJActor;
    private final String hubName;

    @SuppressWarnings("unchecked")
	ClientsContext(Class<S> clazz, String hubName, RequestContext context, ActorRef signalJActor, CallerState callerState) {
        this.clazz = clazz;
		this.context = context;
		this.all = (S) new SenderProxy(signalJActor, Messages.SendType.All, clazz, hubName, context).createProxy();
		this.others = (S) new SenderProxy(signalJActor, Messages.SendType.Others, clazz, hubName, context).createProxy();
		this.caller = (S) new SenderProxy(signalJActor, Messages.SendType.Caller, clazz, hubName, context).createProxy();
        this.signalJActor = signalJActor;
        this.hubName = hubName;
        this.callerState = callerState;
	}

    @SuppressWarnings("unchecked")
	public S client(UUID... connectionIds) {
		return (S) new SenderProxy(signalJActor, Messages.SendType.Clients, clazz, hubName, context, connectionIds, (UUID[])null, null).createProxy();
	}
	
	@SuppressWarnings("unchecked")
	public S allExcept(UUID... connectionIds) {
		return (S) new SenderProxy(signalJActor, Messages.SendType.AllExcept, clazz, hubName, context, (UUID[])null, connectionIds, null).createProxy();
	}
	
	@SuppressWarnings("unchecked")
	public S group(String groupName) {
		return (S) new SenderProxy(signalJActor, Messages.SendType.Group, clazz, hubName, context, (UUID[])null, (UUID[])null, groupName).createProxy();
	}
	
	@SuppressWarnings("unchecked")
	public S group(String groupName, UUID... connectionIds) {
		return (S) new SenderProxy(signalJActor, Messages.SendType.InGroupExcept, clazz, hubName, context, (UUID[])null, connectionIds, groupName).createProxy();
	}
	
	public S inGroupExcept(String groupName, UUID... connectionIds) {
		return group(groupName, connectionIds);
	}
	
	@SuppressWarnings("unchecked")
	public S othersInGroup(String groupName) {
		UUID[] uuids = new UUID[1];
		uuids[0] = context.connectionId;
		return (S) new SenderProxy(signalJActor, Messages.SendType.InGroupExcept, clazz, hubName, context, (UUID[])null, uuids, groupName).createProxy();
	}
}