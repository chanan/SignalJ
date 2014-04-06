package signalJ;
import signalJ.services.Hub;

class DefaultDependencyResolver implements DependencyResolver {

	@Override
	public Hub<?> getHubInstance(String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		Class<?> clazz = Class.forName(className);
		return (Hub<?>) clazz.newInstance();
	}
}