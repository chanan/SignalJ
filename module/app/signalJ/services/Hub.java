package signalJ.services;
import java.util.UUID;

public abstract class Hub<T> implements HubContext<T> {
	private UUID uuid;
	private String className = null;
	protected abstract Class<T> getInterface();
	
	void setConnectionId(UUID uuid) {
		this.uuid = uuid;
	}
	
	public UUID getConnectionId() {
		return uuid;
	}
	
	public void setHubClassName(String className) {
		if(this.className == null) this.className = className;
	}
	
	@Override
	public ClientsContext<T> clients() {
		return new ClientsContext<T>(getInterface(), className ,getConnectionId());
	}
	
	@Override
	public GroupsContext groups() {
		return new GroupsContext();
	}
}