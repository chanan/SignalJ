package hubs;
import play.Logger;

public class HelloWorld extends Hub {
	public void SayHello() {
		Logger.debug("A client made me say hello!");
		TellClients("This is a test!");
	}
}