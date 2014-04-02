package hubs;
import models.Person;
import play.Logger;

public class HelloWorld extends Hub<FirstTestPage> {
	public void SayHello() {
		Logger.debug("A client made me say hello!");
		clients().all().firstTestFucntion();
		clients().all().firstTestFunctionWithParam("Hi");
		clients().all().twoParams(2, 3);
		clients().all().complexObj(new Person("John", "Smith"));
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