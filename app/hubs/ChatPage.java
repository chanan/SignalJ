package hubs;
import models.ChatMessage;

import java.util.List;

public interface ChatPage {
    public void messageToRoom(ChatMessage chatMessage);
    public void userJoined(String username);
    public void userList(List<String> members);
}