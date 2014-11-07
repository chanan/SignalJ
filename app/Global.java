import hubs.GuiceDependencyResolver;
import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.api.mvc.Handler;
import play.mvc.Http;
import signalJ.*;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class Global extends SignalJGlobal {
	private final Injector injector = Guice.createInjector(new GuiceModule());

	@Override
	public <A> A getControllerInstance(Class<A> clazz) throws Exception {
		return injector.getInstance(clazz);
	}

	@Override
	public void onStart(Application arg0) {
		//This is an example of setting your own resolver
		GuiceDependencyResolver resolver = new GuiceDependencyResolver(injector);
		GlobalHost.setDependencyResolver(resolver);
	}
}