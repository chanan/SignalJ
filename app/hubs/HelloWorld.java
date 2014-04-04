package hubs;
import models.Person;
import play.Logger;
import services.Hub;

public class HelloWorld extends Hub<FirstTestPage> {
	public void SayHello() {
		Logger.debug("A client made me say hello!");
		clients().all.firstTestFucntion();
		clients().others.firstTestFunctionWithParam("Hi");
		clients().caller.twoParams(2, 3);
		clients().all.complexObj(new Person("John", "Smith"));
		//Test client(s) by sending to self:
		clients().client(getConnectionId()).calledFromClient(getConnectionId());
		//Test all except by NOT sending to self:
		clients().allExcept(getConnectionId()).notCalledFromClient(getConnectionId());
	}
	
	public void SaySomethingANumberOfTimes(String something, int number) {
		for(int i = 0; i < number; i++) {
			Logger.debug("The client said: " + something);
		}
	}
	
	public int Add(int a, int b) {
		return a + b;
	}

	@Override
	protected Class<FirstTestPage> getInterface() {
		return FirstTestPage.class;
	}
}