package models;
import java.util.List;

/**
 * Created by Chanan on 4/19/2014.
 */
public class ChatMessage {
    public final String username;
    public final String message;
    public final List<String> members;

    public ChatMessage(String username, String message, List<String> members) {
        this.username = username;
        this.message = message;
        this.members = members;
    }
}
