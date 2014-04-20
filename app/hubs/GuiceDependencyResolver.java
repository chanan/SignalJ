package hubs;
import signalJ.DependencyResolver;
import signalJ.services.Hub;

import com.google.inject.Injector;

public class GuiceDependencyResolver implements DependencyResolver {
	private final Injector injector;
	
	public GuiceDependencyResolver(Injector injector) {
		this.injector = injector;
	}

	@Override
	public Hub<?> getHubInstance(String className, ClassLoader _classLoader) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		Class<?> clazz = Class.forName(className, true, _classLoader);
		return (Hub<?>) injector.getInstance(clazz);
	}
}