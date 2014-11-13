package signalJ;

import signalJ.infrastructure.CorsPolicy;
import signalJ.infrastructure.ProtectedData;
import signalJ.infrastructure.UserIdProvider;
import signalJ.infrastructure.impl.DefaultProtectedData;
import signalJ.infrastructure.impl.DefaultUserIdProvider;
import signalJ.infrastructure.impl.DisallowAllCorsPolicy;
import signalJ.models.HubsDescriptor;
import signalJ.services.Hub;
import signalJ.services.HubContext;

public class GlobalHost {
	private final static DependencyResolver _defaultDependencyResolver = new DefaultDependencyResolver();
	private static DependencyResolver _dependencyResolver;
    private static ClassLoader _classLoader;
    private static HubsDescriptor descriptors;

    static {
        setDefaultServices(_defaultDependencyResolver);
    }

    public static HubsDescriptor getDescriptors() {
        return descriptors;
    }

    public static void setDescriptors(HubsDescriptor descriptors) {
        GlobalHost.descriptors = descriptors;
    }

    public static DependencyResolver getDependencyResolver() {
		return _dependencyResolver != null ? _dependencyResolver : _defaultDependencyResolver;
	}

	public static void setDependencyResolver(DependencyResolver dependencyResolver) {
		_dependencyResolver = dependencyResolver;
        setDefaultServices(_dependencyResolver);
	}

    private static void setDefaultServices(DependencyResolver dependencyResolver) {
        if(dependencyResolver.getService(CorsPolicy.class) == null) {
            final CorsPolicy corsPolicy = new DisallowAllCorsPolicy();
            dependencyResolver.register(CorsPolicy.class, () -> corsPolicy);
        }
        if(dependencyResolver.getService(ProtectedData.class) == null) {
            final ProtectedData protectedData = new DefaultProtectedData(SignalJPlugin.getConfig().getString("application.secret"));
            dependencyResolver.register(ProtectedData.class, () -> protectedData);
        }
        if(dependencyResolver.getService(UserIdProvider.class) == null) {
            final UserIdProvider userIdProvider = new DefaultUserIdProvider();
            dependencyResolver.register(UserIdProvider.class, () -> userIdProvider);
        }
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
        hub.setHubClassName(descriptors.getDescriptor(className).getJsonName());
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