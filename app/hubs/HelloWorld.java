package hubs;
import models.Person;
import play.Logger;
import services.StringService;
import signalJ.services.Hub;

import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.List;

public class HelloWorld extends Hub<FirstTestPage> {
	private StringService service;
	
	@Inject
	public HelloWorld(StringService service) {
		this.service = service;
	}
	
	public void sayHello() {
		Logger.debug("A client made me say hello!");
		clients().all.firstTestFunction();
		clients().others.firstTestFunctionWithParam(service.capitalize("Hello there!"));
		clients().caller.twoParams(2, 3);
		clients().all.complexObj(new Person("John", "Smith"));
		//Test client(s) by sending to self:
		clients().client(getConnectionId()).calledFromClient(getConnectionId());
		//Test all except by NOT sending to self:
		clients().allExcept(getConnectionId()).notCalledFromClient(getConnectionId());
        List<Person> list = new ArrayList<>();
        list.add(new Person("John", "Smith"));
        clients().caller.complexList(list);
	}
	
	public void saySomethingANumberOfTimes(String something, int number) {
		for(int i = 0; i < number; i++) {
			Logger.debug("The client said: " + something);
		}
	}
	
	public int add(int a, int b) {
		return a + b;
	}
	
	public void joinGroup(String group) {
		groups().add(getConnectionId(), group);
	}
	
	public void leaveGroup(String group) {
		groups().remove(getConnectionId(), group);
	}
	
	public void talkToGroup(String group, String message) {
		clients().group(group).sendToGroup(message);
	}
	
	public void talkToGroupOtherThanMe(String group, String message) {
		clients().group(group, getConnectionId()).sendToGroup(message);
		clients().othersInGroup(group).sendToGroup("Another way: " + message);
	}

    public void listOfInt(List<Integer> list) {
        Logger.debug("The list: " + list);
        Logger.debug("The items:");
        list.stream().forEach(i -> {
            int x = i * 2;
            Logger.debug("item * 2: " + x);
        });
    }

    public void listOfPerson(List<Person> people) {
        Logger.debug("People: " + people);
        Logger.debug("Last names:");
        people.stream().forEach(p -> Logger.debug(p.getLastName()));
    }

	@Override
	protected Class<FirstTestPage> getInterface() {
		return FirstTestPage.class;
	}
}