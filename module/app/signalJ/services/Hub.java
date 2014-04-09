package signalJ.services;
import java.util.UUID;

import signalJ.models.HubsDescriptor.HubDescriptor;
import akka.actor.ActorRef;

public abstract class Hub<T> implements HubContext<T> {
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
	
	@Override
	public ClientsContext<T> clients() {
		return new ClientsContext<T>(getInterface(), channelActor, getConnectionId());
	}
	
	@Override
	public GroupsContext groups() {
		return new GroupsContext();
	}
	
	protected final class GroupsContext {
		private final ActorRef signalJActor = ActorLocator.getSignalJActor();
		public void add(UUID connectionId, String groupName) {
			signalJActor.tell(new SignalJActor.GroupJoin(groupName, connectionId), channelActor);
		}
		
		public void remove(UUID connectionId, String groupName) {
			signalJActor.tell(new SignalJActor.GroupLeave(groupName, connectionId), channelActor);
		}
	}
}