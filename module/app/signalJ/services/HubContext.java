package signalJ.services;

public interface HubContext<T> {
	public ClientsContext<T> clients();
	
	public GroupsContext groups();
}