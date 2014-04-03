package hubs;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.UUID;

import models.HubsDescriptor.HubDescriptor;
import play.Logger;
import services.ChannelActor;
import services.ChannelActor.ClientFunctionCall.SendType;
import akka.actor.ActorRef;

public abstract class Hub<T> {
	private ActorRef signalJActor;
	private ActorRef channelActor;
	private String channelName;
	private UUID caller;
	private HubDescriptor hubDescriptor;
	protected abstract Class<T> getInterface();
	
	public void setSignalJActor(ActorRef signalJActor) {
		this.signalJActor = signalJActor;
	}
	
	public void setChannelActor(ActorRef channelActor) {
		this.channelActor = channelActor;
	}
	
	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}
	
	public void setCaller(UUID uuid) {
		this.caller = uuid;
	}
	
	public void SetHubDescriptor(HubDescriptor hubDescriptor) {
		this.hubDescriptor = hubDescriptor;
	}
	
	protected ClientsContext<T> clients() {
		return new ClientsContext<T>(getInterface());
	}
	
	protected final class ClientsContext<T> {
		private final T allSend;
		private final T othersSend;
		private final T callerSend;
		
		@SuppressWarnings("unchecked")
		ClientsContext(Class<T> clazz) {
			this.allSend = (T) new SenderProxy(SendType.All, clazz, channelActor, caller).createProxy();
			this.othersSend = (T) new SenderProxy(SendType.Others, clazz, channelActor, caller).createProxy();
			this.callerSend = (T) new SenderProxy(SendType.Caller, clazz, channelActor, caller).createProxy();
		}
		
		public T all() {
			return allSend;
		}
		
		public T others() {
			return othersSend;
		}
		
		public T caller() {
			return callerSend;
		}
	}
}