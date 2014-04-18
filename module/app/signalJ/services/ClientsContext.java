package signalJ.services;
import java.util.UUID;

import play.libs.Akka;
import signalJ.services.ChannelActor.ClientFunctionCall.SendType;
import akka.actor.ActorSelection;

public class ClientsContext<S> {
	public final S all;
	public final S others;
	public final S caller;
	private final Class<S> clazz;
	private final UUID uuid;
	
	@SuppressWarnings("unchecked")
	ClientsContext(Class<S> clazz, String channelName, UUID uuid) {
		this.clazz = clazz;
		this.uuid = uuid;
		this.all = (S) new SenderProxy(SendType.All, clazz, uuid).createProxy();
		this.others = (S) new SenderProxy(SendType.Others, clazz, uuid).createProxy();
		this.caller = (S) new SenderProxy(SendType.Caller, clazz, uuid).createProxy();
	}
	
	@SuppressWarnings("unchecked")
	public S client(UUID... connectionIds) {
		return (S) new SenderProxy(SendType.Clients, clazz, uuid, connectionIds, (UUID[])null, null).createProxy();
	}
	
	@SuppressWarnings("unchecked")
	public S allExcept(UUID... connectionIds) {
		return (S) new SenderProxy(SendType.AllExcept, clazz, uuid, (UUID[])null, connectionIds, null).createProxy();
	}
	
	@SuppressWarnings("unchecked")
	public S group(String groupName) {
		return (S) new SenderProxy(SendType.Group, clazz, uuid, (UUID[])null, (UUID[])null, groupName).createProxy();
	}
	
	@SuppressWarnings("unchecked")
	public S group(String groupName, UUID... connectionIds) {
		return (S) new SenderProxy(SendType.InGroupExcept, clazz, uuid, (UUID[])null, connectionIds, groupName).createProxy();
	}
	
	public S inGroupExcept(String groupName, UUID... connectionIds) {
		return group(groupName, connectionIds);
	}
	
	@SuppressWarnings("unchecked")
	public S othersInGroup(String groupName) {
		UUID[] uuids = new UUID[1];
		uuids[0] = uuid;
		return (S) new SenderProxy(SendType.InGroupExcept, clazz, uuid, (UUID[])null, uuids, groupName).createProxy();
	}
}