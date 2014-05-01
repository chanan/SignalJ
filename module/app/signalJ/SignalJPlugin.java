package signalJ;

import akka.actor.ActorRef;
import akka.actor.Props;
import play.Application;
import play.Logger;
import play.Play;
import play.Plugin;
import play.libs.Akka;
import signalJ.services.SignalJActor;

/**
 * Created by Chanan on 4/20/2014.
 */
public class SignalJPlugin extends Plugin {
    private final Application application;
    private ActorRef signalJActor;

    private static SignalJPlugin plugin() {
        return Play.application().plugin(SignalJPlugin.class);
    }

    public static ActorRef getSignalJActor() {
        return plugin().signalJActor;
    }

    public SignalJPlugin(Application application) {
        this.application = application;
        try {
            GlobalHost.setClassLoader(application.classloader());
        } catch (Exception e) {
            Logger.error("Error on constructor", e);
        }
        Logger.debug("here");
    }

    @Override
    public void onStart() {
        signalJActor = Akka.system().actorOf(Props.create(SignalJActor.class), "signalJ");
    }
}