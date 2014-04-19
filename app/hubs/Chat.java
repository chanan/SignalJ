package hubs;
import actors.Robot;
import akka.actor.ActorRef;
import akka.actor.Props;
import models.ChatMessage;
import play.libs.Akka;
import scala.concurrent.duration.Duration;
import signalJ.services.Hub;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Chanan on 4/18/2014.
 */
public class Chat extends Hub<ChatPage> {
    private final static List<String> users = new ArrayList<String>();
    private static ActorRef robot;

    public void joinChat(String username) {
        users.add(username);
        clients().others.userJoined(username);
        clients().all.userList(users);
        if(robot == null) {
            robot = Akka.system().actorOf(Props.create(Robot.class), "robot");
            Akka.system().scheduler().schedule(
                Duration.create(30, TimeUnit.SECONDS),
                Duration.create(30, TimeUnit.SECONDS),
                robot,
                "tick",
                Akka.system().dispatcher(),
                ActorRef.noSender()
            );
            users.add("Robot");
            clients().all.userJoined("Robot");
            clients().all.userList(users);
        }
    }

    public void talkToRoom(String username, String message) {
        clients().others.messageToRoom(new ChatMessage(username, message, users));
    }

    @Override
    protected Class<ChatPage> getInterface() {
        return ChatPage.class;
    }
}
