package signalJ;
import play.Application;
import play.Plugin;

/**
 * Created by Chanan on 4/20/2014.
 */
public class SignalJPlugin extends Plugin {
    private final Application application;

    public SignalJPlugin(Application application) {
        this.application = application;
        GlobalHost.setClassLoader(application.classloader());
    }
}