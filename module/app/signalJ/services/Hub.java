package signalJ.services;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import play.Logger;
import signalJ.models.HubsDescriptor.HubDescriptor;
import signalJ.services.ChannelActor.ClientFunctionCall.SendType;
import akka.actor.ActorRef;

public abstract class Hub<T> {
	private ActorRef channelActor;
	private UUID uuid;
	private HubDescriptor hubDescriptor;
	protected abstract Class<T> getInterface();

	void setChannelActor(ActorRef channelActor) {
		this.channelActor = channelActor;
	}
	
	void setCaller(UUID uuid) {
		this.uuid = uuid;
	}
	
	public UUID getConnectionId() {
		return uuid;
	}
	
	void SetHubDescriptor(HubDescriptor hubDescriptor) {
		this.hubDescriptor = hubDescriptor;
	}
	
	protected ClientsContext<T> clients() {
		return new ClientsContext<T>(getInterface());
	}
	
	protected GroupsContext groups() {
		return new GroupsContext();
	}
	
	protected final class GroupsContext {
		public void add(UUID connectionId, String groupName) {
			channelActor.tell(new ChannelActor.GroupJoin(groupName, connectionId), channelActor);
		}
		
		public void remove(UUID connectionId, String groupName) {
			channelActor.tell(new ChannelActor.GroupLeave(groupName, connectionId), channelActor);
		}
	}
	
	protected final class ClientsContext<S> {
		public final S all;
		public final S others;
		public final S caller;
		private final Class<S> clazz;
		
		@SuppressWarnings("unchecked")
		ClientsContext(Class<S> clazz) {
			this.clazz = clazz;
			this.all = (S) new SenderProxy(SendType.All, clazz, channelActor, uuid).createProxy();
			this.others = (S) new SenderProxy(SendType.Others, clazz, channelActor, uuid).createProxy();
			this.caller = (S) new SenderProxy(SendType.Caller, clazz, channelActor, uuid).createProxy();
		}
		
		@SuppressWarnings("unchecked")
		public S client(UUID... connectionIds) {
			return (S) new SenderProxy(SendType.Clients, clazz, channelActor, uuid, connectionIds, (UUID[])null, null).createProxy();
		}
		
		@SuppressWarnings("unchecked")
		public S allExcept(UUID... connectionIds) {
			return (S) new SenderProxy(SendType.AllExcept, clazz, channelActor, uuid, (UUID[])null, connectionIds, null).createProxy();
		}
		
		@SuppressWarnings("unchecked")
		public S group(String groupName) {
			return (S) new SenderProxy(SendType.Group, clazz, channelActor, uuid, (UUID[])null, (UUID[])null, groupName).createProxy();
		}
		
		@SuppressWarnings("unchecked")
		public S group(String groupName, UUID... connectionIds) {
			return (S) new SenderProxy(SendType.InGroupExcept, clazz, channelActor, uuid, (UUID[])null, connectionIds, groupName).createProxy();
		}
		
		public S inGroupExcept(String groupName, UUID... connectionIds) {
			return group(groupName, connectionIds);
		}
		
		@SuppressWarnings("unchecked")
		public S othersInGroup(String groupName) {
			UUID[] uuids = new UUID[1];
			uuids[0] = getConnectionId();
			return (S) new SenderProxy(SendType.InGroupExcept, clazz, channelActor, uuid, (UUID[])null, uuids, groupName).createProxy();
		}
	}
	
	//TODO: Fix this API it is bad (Fix ClientFunctionCall as well)
	private class SenderProxy implements InvocationHandler {
		private final SendType sendType;
		private final Class<?> clazz;
		private final ActorRef channelActor;
		private final UUID caller;
		private final UUID[] clients;
		private final UUID[] allExcept;
		private final String groupName;

		public SenderProxy(SendType sendType, Class<?> clazz, ActorRef channelActor, UUID caller) {
			this.sendType = sendType;
			this.clazz = clazz;
			this.channelActor = channelActor;
			this.caller = caller;
			this.clients = null;
			this.allExcept = null;
			this.groupName = null;
		}
		
		public SenderProxy(SendType sendType, Class<?> clazz, ActorRef channelActor, UUID caller, UUID[] clients, UUID[] allExcept, String groupName) {
			this.sendType = sendType;
			this.clazz = clazz;
			this.channelActor = channelActor;
			this.caller = caller;
			this.clients = clients;
			this.allExcept = allExcept;
			this.groupName = groupName;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			Logger.debug(sendType + " - " + method.getName() + " " + args);
			channelActor.tell(new ChannelActor.ClientFunctionCall(method, clazz.getName(), caller, sendType, method.getName(), args, clients, allExcept, groupName), channelActor);
			return null;
		}
		
		public Object createProxy() throws IllegalArgumentException {
	        return Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, this);
	    }
	}
}