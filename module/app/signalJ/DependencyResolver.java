package signalJ;
import signalJ.services.Hub;

public interface DependencyResolver  {
	
	public Hub<?> getHubInstance(String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException;

}