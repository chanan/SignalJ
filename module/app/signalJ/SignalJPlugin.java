package signalJ;

import akka.actor.ActorRef;
import akka.actor.Props;
import play.Application;
import play.Logger;
import play.Play;
import play.Plugin;
import play.libs.Akka;
import signalJ.infrastructure.DefaultProtectedData;
import signalJ.infrastructure.ProtectedData;
import signalJ.services.SignalJActor;

import java.util.Optional;

public class SignalJPlugin extends Plugin {
    private final Application application;
    private ActorRef signalJActor;
    private ProtectedData protectedData;

    private static SignalJPlugin plugin() {
        return Play.application().plugin(SignalJPlugin.class);
    }

    public static ActorRef getSignalJActor() {
        return plugin().signalJActor;
    }

    public static ProtectedData getProtectedDataProvider() {
        return plugin().protectedData;
    }

    public SignalJPlugin(Application application) {
        this.application = application;
        try {
            this.protectedData = new DefaultProtectedData(application.configuration().getString("application.secret"));
            GlobalHost.setClassLoader(application.classloader());
        } catch (Exception e) {
            Logger.error("Could not construct the SignalJ plugin", e);
        }
    }

    @Override
    public void onStart() {
        signalJActor = Akka.system().actorOf(Props.create(SignalJActor.class, protectedData), "signalJ");
    }

    public static boolean isDev() {
        return plugin().application.isDev();
    }

    public static boolean isProd() {
        return plugin().application.isProd();
    }

    public static boolean isTest() {
        return plugin().application.isTest();
    }
}