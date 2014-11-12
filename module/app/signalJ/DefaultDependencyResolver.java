package signalJ;
import signalJ.services.Hub;

import java.util.HashMap;
import java.util.function.Supplier;

class DefaultDependencyResolver implements DependencyResolver {
    private final HashMap<Class<?>, Supplier<Object>> services = new HashMap<>();

    @Override
	public Hub<?> getHubInstance(String className, ClassLoader classLoader) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		Class<?> clazz = Class.forName(className, true, classLoader);
		return (Hub<?>) clazz.newInstance();
	}

    @Override
    public void Register(Class<?> serviceClass, Supplier<Object> supplier) {
        services.put(serviceClass, supplier);
    }

    @Override
    public Object getService(Class<?> serviceClass) {
        return services.get(serviceClass).get();
    }
}