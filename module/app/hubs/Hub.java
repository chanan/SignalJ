package hubs;
import play.Logger;

public abstract class Hub {
	public void TellClients(String message) {
		Logger.debug("This does nothing for now: " + message);
	}
}