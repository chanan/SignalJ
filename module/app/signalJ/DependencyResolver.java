package signalJ;
import signalJ.services.Hub;

public interface DependencyResolver  {
	
	public Hub<?> getHubInstance(String className, ClassLoader classLoader) throws ClassNotFoundException, InstantiationException, IllegalAccessException;

}