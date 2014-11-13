package hubs;

import com.google.inject.AbstractModule;
import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import signalJ.DependencyResolver;
import signalJ.services.Hub;

import java.util.function.Supplier;

public class GuiceDependencyResolver implements DependencyResolver {
	private Injector injector;
	
	public GuiceDependencyResolver(Injector injector) {
		this.injector = injector;
	}

	@Override
	public Hub<?> getHubInstance(String className, ClassLoader _classLoader) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		Class<?> clazz = Class.forName(className, true, _classLoader);
		return (Hub<?>) injector.getInstance(clazz);
	}

    @Override
    public <T> void register(Class<T> serviceClass, Supplier<T> supplier) {
        injector = injector.createChildInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(serviceClass).toInstance(supplier.get());
            }
        });
    }

    @Override
    public <T> T getService(Class<T> serviceClass) {
        try {
            return injector.getInstance(serviceClass);
        } catch (ConfigurationException e) {
            return null;
        }
    }
}