package signalJ;

public class GlobalHost {
	private final static DependencyResolver _defaultDependencyResolver = new DefaultDependencyResolver();
	private static DependencyResolver _dependencyResolver;

	public static DependencyResolver getDependencyResolver() {
		return _dependencyResolver != null ? _dependencyResolver : _defaultDependencyResolver;
	}

	public static void setDependencyResolver(DependencyResolver dependencyResolver) {
		_dependencyResolver = dependencyResolver;
	}
	
	protected String getInjector() {
		return "";
	}
}