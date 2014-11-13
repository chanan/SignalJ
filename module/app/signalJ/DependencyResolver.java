package signalJ;
import signalJ.services.Hub;

import java.util.function.Supplier;

public interface DependencyResolver  {
	
	public Hub<?> getHubInstance(String className, ClassLoader classLoader) throws ClassNotFoundException, InstantiationException, IllegalAccessException;

    <T> void register(Class<T> serviceClass, Supplier<T> supplier);

    <T> T getService(Class<T> serviceClass);
}