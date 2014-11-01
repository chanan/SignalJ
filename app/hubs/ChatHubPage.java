package hubs;

import java.util.List;

public interface ChatHubPage {
    public void addNewMessageToPage(String name, String message, List<String> words);
}
