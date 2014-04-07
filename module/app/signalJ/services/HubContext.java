package signalJ.services;

import signalJ.services.Hub.GroupsContext;

public interface HubContext<T> {
	public ClientsContext<T> clients();
	
	public GroupsContext groups();
}