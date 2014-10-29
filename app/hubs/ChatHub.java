package hubs;

import signalJ.services.Hub;

public class ChatHub extends Hub<ChatHubPage> {

    public void send(String name, String message) {
        clients().all.addNewMessageToPage(name, message);
    }

    @Override
    protected Class<ChatHubPage> getInterface() {
        return ChatHubPage.class;
    }
}