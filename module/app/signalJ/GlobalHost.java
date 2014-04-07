package signalJ;

import signalJ.services.HubContext;


public class GlobalHost {
	private final static DependencyResolver _defaultDependencyResolver = new DefaultDependencyResolver();
	private static DependencyResolver _dependencyResolver;

	public static DependencyResolver getDependencyResolver() {
		return _dependencyResolver != null ? _dependencyResolver : _defaultDependencyResolver;
	}

	public static void setDependencyResolver(DependencyResolver dependencyResolver) {
		_dependencyResolver = dependencyResolver;
	}
	
	//TODO: Needs channelActor.
	@SuppressWarnings("unchecked")
	public static<TInterface> HubContext<TInterface> getHub(String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		return (HubContext<TInterface>)getDependencyResolver().getHubInstance(className);
	}
	
	@SuppressWarnings("unchecked")
	public static<TInterface> HubContext<TInterface> getHub(Class<?> clazz) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		return (HubContext<TInterface>)getDependencyResolver().getHubInstance(clazz.getName());
	}
}