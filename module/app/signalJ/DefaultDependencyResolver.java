package signalJ;
import signalJ.services.Hub;

import java.util.HashMap;
import java.util.function.Supplier;

class DefaultDependencyResolver implements DependencyResolver {
    private final HashMap<Class<?>, Supplier<?>> services = new HashMap<>();

    @Override
	public Hub<?> getHubInstance(String className, ClassLoader classLoader) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		Class<?> clazz = Class.forName(className, true, classLoader);
		return (Hub<?>) clazz.newInstance();
	}

    @Override
    public <T> void register(Class<T> serviceClass, Supplier<T> supplier) {
        services.put(serviceClass, supplier);
    }

    @Override
    public <T> T getService(Class<T> serviceClass) {
        final Supplier<T> supplier = (Supplier<T>) services.get(serviceClass);
        if(supplier == null) return null;
        return (T)services.get(serviceClass).get();
    }
}