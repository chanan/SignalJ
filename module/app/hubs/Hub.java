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
		//private final Sender allSend = new Sender(SendType.Others);
		private final Sender othersSend = new Sender(SendType.Others);
		private final Sender callerSend = new Sender(SendType.Caller);
		private Class<T> clazz;
		
		@SuppressWarnings("unchecked")
		ClientsContext(Class<T> clazz) {
			this.clazz = clazz;
			this.allSend = (T) new SenderProxy(SendType.All, clazz, channelActor, caller).createProxy();
		}
		
		public T all() {
			return allSend;
		}
		
		public Sender others() {
			return othersSend;
		}
		
		public Sender caller() {
			return callerSend;
		}
	}
	
	protected final class Sender {
		private final SendType sendType;
		
		Sender(SendType sendType) {
			this.sendType = sendType;
		}
		
		public void SendMessage(String function, String message) {
			//channelActor.tell(new ChannelActor.ClientFunctionCall(caller, channelName, sendType, function, message), channelActor);
			Logger.debug(sendType + " - " + function + " " + message);
		}
	}
}