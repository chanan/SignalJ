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
	private UUID caller;
	private HubDescriptor hubDescriptor;
	protected abstract Class<T> getInterface();

	void setChannelActor(ActorRef channelActor) {
		this.channelActor = channelActor;
	}
	
	void setCaller(UUID uuid) {
		this.caller = uuid;
	}
	
	void SetHubDescriptor(HubDescriptor hubDescriptor) {
		this.hubDescriptor = hubDescriptor;
	}
	
	protected ClientsContext<T> clients() {
		return new ClientsContext<T>(getInterface());
	}
	
	protected final class ClientsContext<S> {
		private final S allSend;
		private final S othersSend;
		private final S callerSend;
		
		@SuppressWarnings("unchecked")
		ClientsContext(Class<S> clazz) {
			this.allSend = (S) new SenderProxy(SendType.All, clazz, channelActor, caller).createProxy();
			this.othersSend = (S) new SenderProxy(SendType.Others, clazz, channelActor, caller).createProxy();
			this.callerSend = (S) new SenderProxy(SendType.Caller, clazz, channelActor, caller).createProxy();
		}
		
		public S all() {
			return allSend;
		}
		
		public S others() {
			return othersSend;
		}
		
		public S caller() {
			return callerSend;
		}
	}
	
	private class SenderProxy implements InvocationHandler {
		private final SendType sendType;
		private final Class<?> clazz;
		private final ActorRef channelActor;
		private final UUID caller;

		public SenderProxy(SendType sendType, Class<?> clazz, ActorRef channelActor, UUID caller) {
			this.sendType = sendType;
			this.clazz = clazz;
			this.channelActor = channelActor;
			this.caller = caller;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			Logger.debug(sendType + " - " + method.getName() + " " + args);		
			channelActor.tell(new ChannelActor.ClientFunctionCall(method, clazz.getName(), caller, sendType, method.getName(), args), channelActor);
			return null;
		}
		
		public Object createProxy() throws IllegalArgumentException {
	        return Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, this);
	    }
	}
}