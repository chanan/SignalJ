package hubs;

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

    @Override
    protected Class<ChatHubPage> getInterface() {
        return ChatHubPage.class;
    }
}