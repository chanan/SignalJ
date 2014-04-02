package hubs;

import java.util.UUID;

import play.Logger;
import services.ChannelActor;
import services.ChannelActor.ClientFunctionCall.SendType;
import akka.actor.ActorRef;

public abstract class Hub {
	private final ClientsContext clientsContext = new ClientsContext();
	private ActorRef signalJActor;
	private ActorRef channelActor;
	private String channelName;
	private UUID caller;
	
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
	
	protected ClientsContext clients() {
		return clientsContext;
	}
	
	protected final class ClientsContext {
		private final Sender allSend = new Sender(SendType.All);
		private final Sender othersSend = new Sender(SendType.Others);
		private final Sender callerSend = new Sender(SendType.Caller);
		
		public Sender all() {
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
			channelActor.tell(new ChannelActor.ClientFunctionCall(caller, channelName, sendType, function, message), channelActor);
			Logger.debug(sendType + " - " + function + " " + message);
		}
	}
}