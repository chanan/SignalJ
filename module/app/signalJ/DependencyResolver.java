package signalJ;
import signalJ.services.Hub;

import java.util.function.Supplier;

public interface DependencyResolver  {
	
	public Hub<?> getHubInstance(String className, ClassLoader classLoader) throws ClassNotFoundException, InstantiationException, IllegalAccessException;

    void Register(Class<?> serviceClass, Supplier<Object> supplier);

    Object getService(Class<?> serviceClass);
}