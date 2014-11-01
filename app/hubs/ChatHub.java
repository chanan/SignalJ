package hubs;

import models.Person;
import play.Logger;
import signalJ.services.Hub;

import java.util.ArrayList;
import java.util.List;

public class ChatHub extends Hub<ChatHubPage> {

    public void send(String name, String message) {
        List<String> words = new ArrayList<>();
        words.add("Hi");
        words.add("Hello");
        clients().all.addNewMessageToPage(name, message, words);
    }

    public String send2(List<String> words) {
        Logger.debug(words.toString());
        return "yay";
    }

    public void complexObj(Person person) {
        Logger.debug(person.getFirstName());
    }

    public void complexList(String str, List<Person> list) {
        list.stream().forEach(p -> Logger.debug(p.getFirstName()));
    }

    @Override
    protected Class<ChatHubPage> getInterface() {
        return ChatHubPage.class;
    }
}