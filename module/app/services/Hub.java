package services;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

import models.HubsDescriptor.HubDescriptor;
import play.Logger;
import services.ChannelActor.ClientFunctionCall.SendType;
import akka.actor.ActorRef;

public abstract class Hub<T> {
	private ActorRef channelActor;
	private UUID uuid;
	private HubDescriptor hubDescriptor;
	protected abstract Class<T> getInterface();
	//protected ClientsContext<T> clients;

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
			return (S) new SenderProxy(SendType.Clients, clazz, channelActor, uuid, connectionIds, (UUID[])null).createProxy();
		}
		
		@SuppressWarnings("unchecked")
		public S allExcept(UUID... connectionIds) {
			S proxy = (S) new SenderProxy(SendType.AllExcept, clazz, channelActor, uuid, (UUID[])null, connectionIds).createProxy();
			return proxy;
		}
	}
	
	private class SenderProxy implements InvocationHandler {
		private final SendType sendType;
		private final Class<?> clazz;
		private final ActorRef channelActor;
		private final UUID caller;
		private final UUID[] clients;
		private final UUID[] allExcept;

		public SenderProxy(SendType sendType, Class<?> clazz, ActorRef channelActor, UUID caller) {
			this.sendType = sendType;
			this.clazz = clazz;
			this.channelActor = channelActor;
			this.caller = caller;
			this.clients = null;
			this.allExcept = null;
		}
		
		public SenderProxy(SendType sendType, Class<?> clazz, ActorRef channelActor, UUID caller, UUID[] clients, UUID[] allExcept) {
			this.sendType = sendType;
			this.clazz = clazz;
			this.channelActor = channelActor;
			this.caller = caller;
			this.clients = clients;
			this.allExcept = allExcept;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			Logger.debug(sendType + " - " + method.getName() + " " + args);
			Logger.debug("channelActor: " + channelActor);
			channelActor.tell(new ChannelActor.ClientFunctionCall(method, clazz.getName(), caller, sendType, method.getName(), args, clients, allExcept), channelActor);
			return null;
		}
		
		public Object createProxy() throws IllegalArgumentException {
	        return Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, this);
	    }
	}
}