package hubs;
import play.Logger;

public class HelloWorld extends Hub {
	public void SayHello() {
		Logger.debug("A client made me say hello!");
		this.clients().all().SendMessage("myFirstClientFunction", "Hello from server to all clients!");
		this.clients().others().SendMessage("myFirstClientFunction", "Hello from server to other clients!");
		this.clients().caller().SendMessage("myFirstClientFunction", "Hello from server to caller!");
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