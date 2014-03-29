package hubs;
import play.Logger;

public class HelloWorld extends Hub {
	public void SayHello() {
		Logger.debug("A client made me say hello!");
		TellClients("This is a test!");
	}
	
	public void SaySomethingANumberOfTimes(String something, int number) {
		for(int i = 0; i < number; i++) {
			Logger.debug("The client said: " + something);
		}
	}
	
	public int Add(int a, int b) {
		return a + b;
	}
}