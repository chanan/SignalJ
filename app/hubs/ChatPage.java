package hubs;
import models.ChatMessage;

import java.util.List;

/**
 * Created by Chanan on 4/18/2014.
 */
public interface ChatPage {
    public void messageToRoom(ChatMessage chatMessage);
    public void userJoined(String username);
    public void userList(List<String> members);
}