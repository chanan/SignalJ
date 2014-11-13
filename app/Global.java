import com.google.inject.Guice;
import com.google.inject.Injector;
import hubs.GuiceDependencyResolver;
import play.Application;
import play.api.mvc.EssentialFilter;
import signalJ.GlobalHost;
import signalJ.SignalJGlobal;
import signalJ.infrastructure.CORSFilter;
import signalJ.infrastructure.CorsPolicy;
import signalJ.infrastructure.impl.AllowAllCorsPolicy;

public class Global extends SignalJGlobal {
    final Injector injector = Guice.createInjector(new GuiceModule());

    @Override
    public <A> A getControllerInstance(Class<A> aClass) throws Exception {
        return injector.getInstance(aClass);
    }

    @Override
    public void onStart(Application application) {
        final GuiceDependencyResolver resolver = new GuiceDependencyResolver(injector);
        GlobalHost.setDependencyResolver(resolver);
        final CorsPolicy policy = new AllowAllCorsPolicy();
        GlobalHost.getDependencyResolver().register(CorsPolicy.class, () -> policy);
    }

    @Override
    public <T extends EssentialFilter> Class<T>[] filters() {
        return new Class[] {CORSFilter.class};
    }
}