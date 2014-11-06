package hubs;
import models.Person;
import play.Logger;
import services.StringService;
import signalJ.annotations.HubName;
import signalJ.services.Hub;

import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@HubName("test")
public class HelloWorld extends Hub<FirstTestPage> {
	private StringService service;
	
	@Inject
	public HelloWorld(StringService service) {
		this.service = service;
	}
	
	public void sayHello() {
		Logger.debug("A client made me say hello! Below are some tests of my functionality");

        Logger.debug("State:");

        clients().callerState.forEach((k, v) -> Logger.debug(k + ": " + v));
        clients().callerState.put("prop", "Server side change");
        clients().callerState.put("ServerProp", "Added on the server");

		clients().all.firstTestFunction();
		clients().others.firstTestFunctionWithParam(service.capitalize("Hello there!"));
		clients().caller.twoParams(2, 3);
		clients().all.complexObj(new Person("John", "Smith"));
		//Test client(s) by sending to self:
		clients().client(context().connectionId).calledFromClient(context().connectionId);
		//Test all except by NOT sending to self:
		clients().allExcept(context().connectionId).notCalledFromClient(context().connectionId);
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
	
	public void talkToGroup(String group, String message) {
		clients().group(group).sendToGroup(message);
	}
	
	public void talkToGroupOtherThanMe(String group, String message) {
		clients().group(group, context().connectionId).sendToGroup(message);
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

    public void joinGroup(String group) {
        groups().add(context().connectionId, group);
    }

    public void leaveGroup(String group) {
        groups().remove(context().connectionId, group);
    }

    public void joinGroup(UUID connectionId, String group) {
        groups().add(connectionId, group);
    }

    public void leaveGroup(UUID connectionId, String group) {
        groups().remove(connectionId, group);
    }

    public void causeError() throws Exception {
        throw new Exception("Boom");
    }

	@Override
	protected Class<FirstTestPage> getInterface() {
		return FirstTestPage.class;
	}

    @Override
    public void onConnected() {
        Logger.debug("Connected! " + context().connectionId);
        clients().all.calledFromOnConnected();
    }

    @Override
    public void onDisconnected() {
        Logger.debug("Disconnected: " + context().connectionId);
    }
}