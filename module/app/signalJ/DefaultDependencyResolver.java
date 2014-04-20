package signalJ;
import signalJ.services.Hub;

class DefaultDependencyResolver implements DependencyResolver {

    @Override
	public Hub<?> getHubInstance(String className, ClassLoader classLoader) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		Class<?> clazz = Class.forName(className, true, classLoader);
		return (Hub<?>) clazz.newInstance();
	}
}