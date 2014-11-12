package signalJ;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.typesafe.config.Config;
import play.Application;
import play.Logger;
import play.Play;
import play.Plugin;
import play.libs.Akka;
import signalJ.infrastructure.DefaultProtectedData;
import signalJ.infrastructure.ProtectedData;
import signalJ.models.Configuration;
import signalJ.services.SignalJActor;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Optional;

public class SignalJPlugin extends Plugin {
    private final Application application;
    private ActorRef signalJActor;
    private ProtectedData protectedData;
    private final Configuration config;

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
        config = loadConfig(application);
        try {
            this.protectedData = new DefaultProtectedData(application.configuration().getString("application.secret"));
            GlobalHost.setClassLoader(application.classloader());
        } catch (Exception e) {
            Logger.error("Could not construct the SignalJ plugin", e);
        }
    }

    public static play.Configuration getConfig() {
        return plugin().application.configuration();
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

    public static Configuration getConfiguration() {
        return plugin().config;
    }

    private Configuration loadConfig(Application application) {
        return new Configuration(
            getDuration(application, "SignalJ.configuration.keepAliveTimeout", "20 seconds"),
            getDuration(application, "SignalJ.configuration.disconnectTimeout", "30 seconds"),
            getDuration(application, "SignalJ.configuration.connectionTimeout", "20 seconds")
        );
    }

    private Duration getDuration(Application application, String configName, String defaultString) {
        final String str = Optional.ofNullable(application.configuration().getString(configName)).orElse(defaultString);
        return Duration.of(getTime(str), getTimeUnit(str));
    }

    private static TemporalUnit getTimeUnit(String duration) {
        String trimmed = duration.trim().toLowerCase();
        if(trimmed.endsWith("ns") || trimmed.endsWith("nanosecond") || trimmed.endsWith("nanoseconds")) return ChronoUnit.NANOS;
        if(trimmed.endsWith("us") || trimmed.endsWith("microsecond") || trimmed.endsWith("microseconds")) return ChronoUnit.MICROS;
        if(trimmed.endsWith("ms") || trimmed.endsWith("millisecond") || trimmed.endsWith("milliseconds")) return ChronoUnit.MILLIS;
        if(trimmed.endsWith("s") || trimmed.endsWith("second") || trimmed.endsWith("seconds")) return ChronoUnit.SECONDS;
        if(trimmed.endsWith("m") || trimmed.endsWith("minute") || trimmed.endsWith("minutes")) return ChronoUnit.MINUTES;
        if(trimmed.endsWith("h") || trimmed.endsWith("hour") || trimmed.endsWith("hours")) return ChronoUnit.HOURS;
        if(trimmed.endsWith("d") || trimmed.endsWith("day") || trimmed.endsWith("days")) return ChronoUnit.DAYS;
        else return ChronoUnit.SECONDS;
    }

    //TODO: Typesafe config has a getDuration in newer version, use that when play is updated
    private static long getTime(String duration) {
        String trimmed = duration.trim();
        String number = "";
        for(char ch : trimmed.toCharArray()) {
            if(Character.isDigit(ch)) number = number + ch;
            else break;
        }
        return Long.parseLong(number);
    }
}