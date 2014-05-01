package signalJ.services;
import java.util.UUID;

import akka.actor.ActorRef;
import signalJ.services.HubActor.ClientFunctionCall.SendType;

public class ClientsContext<S> {
	public final S all;
	public final S others;
	public final S caller;
	private final Class<S> clazz;
	private final UUID uuid;
    private final ActorRef signalJActor;

    @SuppressWarnings("unchecked")
	ClientsContext(Class<S> clazz, String channelName, UUID uuid, ActorRef signalJActor) {
		this.clazz = clazz;
		this.uuid = uuid;
		this.all = (S) new SenderProxy(signalJActor, SendType.All, clazz, uuid).createProxy();
		this.others = (S) new SenderProxy(signalJActor, SendType.Others, clazz, uuid).createProxy();
		this.caller = (S) new SenderProxy(signalJActor, SendType.Caller, clazz, uuid).createProxy();
        this.signalJActor = signalJActor;
	}

    @SuppressWarnings("unchecked")
	public S client(UUID... connectionIds) {
		return (S) new SenderProxy(signalJActor, SendType.Clients, clazz, uuid, connectionIds, (UUID[])null, null).createProxy();
	}
	
	@SuppressWarnings("unchecked")
	public S allExcept(UUID... connectionIds) {
		return (S) new SenderProxy(signalJActor, SendType.AllExcept, clazz, uuid, (UUID[])null, connectionIds, null).createProxy();
	}
	
	@SuppressWarnings("unchecked")
	public S group(String groupName) {
		return (S) new SenderProxy(signalJActor, SendType.Group, clazz, uuid, (UUID[])null, (UUID[])null, groupName).createProxy();
	}
	
	@SuppressWarnings("unchecked")
	public S group(String groupName, UUID... connectionIds) {
		return (S) new SenderProxy(signalJActor, SendType.InGroupExcept, clazz, uuid, (UUID[])null, connectionIds, groupName).createProxy();
	}
	
	public S inGroupExcept(String groupName, UUID... connectionIds) {
		return group(groupName, connectionIds);
	}
	
	@SuppressWarnings("unchecked")
	public S othersInGroup(String groupName) {
		UUID[] uuids = new UUID[1];
		uuids[0] = uuid;
		return (S) new SenderProxy(signalJActor, SendType.InGroupExcept, clazz, uuid, (UUID[])null, uuids, groupName).createProxy();
	}
}