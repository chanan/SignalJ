package signalJ;

import signalJ.services.Hub;
import signalJ.services.HubContext;

public class GlobalHost {
	private final static DependencyResolver _defaultDependencyResolver = new DefaultDependencyResolver();
	private static DependencyResolver _dependencyResolver;
    private static ClassLoader _classLoader;

	public static DependencyResolver getDependencyResolver() {
		return _dependencyResolver != null ? _dependencyResolver : _defaultDependencyResolver;
	}

	public static void setDependencyResolver(DependencyResolver dependencyResolver) {
		_dependencyResolver = dependencyResolver;
	}
	
	@SuppressWarnings("unchecked")
	public static<TInterface> HubContext<TInterface> getHub(String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		return (HubContext<TInterface>)getInstance(className);
	}
	
	@SuppressWarnings("unchecked")
	public static<TInterface> HubContext<TInterface> getHub(Class<?> clazz) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		return (HubContext<TInterface>)getInstance(clazz.getName());
	}
	
	private static Hub<?> getInstance(String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		final Hub<?> hub = getDependencyResolver().getHubInstance(className, _classLoader);
        hub.setSignalJActor(SignalJPlugin.getSignalJActor());
        hub.setHubClassName(getSimpleName(className));
		return hub;
	}

    private static String getSimpleName(String className) {
        if(className.indexOf('.') != -1) {
            return className.substring(className.lastIndexOf('.') + 1);
        } else {
            return className;
        }
    }

    static void setClassLoader(ClassLoader classLoader) {
        _classLoader = classLoader;
    }

    public static ClassLoader getClassLoader() {
        return _classLoader;
    }
}