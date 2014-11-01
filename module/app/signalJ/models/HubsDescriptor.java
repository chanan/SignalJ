package signalJ.models;
import play.Logger;

import static org.reflections.ReflectionUtils.getMethods;
import static org.reflections.ReflectionUtils.withModifier;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HubsDescriptor {
	public final static String VERSION = "1.0";
	private final Map<String, HubDescriptor> hubs = new HashMap<>();
	private final static String CRLF = "\n";
    private String js;
    private String toString;
    private ClassLoader classLoader;
	
	public HubDescriptor addDescriptor(String name) throws ClassNotFoundException {
		HubDescriptor hub;
        if(classLoader != null) hub = new HubDescriptor(name, this.classLoader);
        else hub = new HubDescriptor(name);
		hubs.put(name, hub);
		return hub;
	}
	
	public Collection<HubDescriptor> getHubDescriptors() {
		return hubs.values();
	}
	
	public HubDescriptor getDescriptor(String name) {
		return hubs.get(name);
	}

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }
	
	public boolean isEmpty() {
		return hubs.isEmpty();
	}
	
	public int size() {
		return hubs.size();
	}
	
	@Override
	public String toString() {
        if(toString != null) return toString;
        StringBuilder sb = new StringBuilder();
		sb.append("{\"version\": \"").append(VERSION).append("\", \"hubs\": [");
		int i = 0;
		for(final HubDescriptor hub : hubs.values()) {
			sb.append(hub);
			i++;
			if(i < hubs.size()) sb.append(", ");
		}
		sb.append("]}");
        toString = sb.toString();
		return toString;
	}
	
	public String toJS() {
        if(js != null) return js;
		StringBuilder sb = new StringBuilder();
		sb.append("//Hubs version: ").append(VERSION).append(CRLF);
		for(final HubDescriptor hub : hubs.values()) {
			sb.append(hub.toJS()).append(CRLF);
		}
        js = sb.toString();
		return js;
	}
	
	private String lowerCaseFirstChar(String className) {
		return Character.toLowerCase(className.charAt(0)) + className.substring(1);
	}
	
	public class HubDescriptor {
		private final String name;
        private final String jsonName;
		private final Class<? extends HubDescriptor> hub;
		private final List<Procedure> procedures = new ArrayList<Procedure>();

		@SuppressWarnings("unchecked")
		public HubDescriptor(String name) throws ClassNotFoundException {
			hub = (Class<? extends HubDescriptor>) Class.forName(name);
			this.name = name;
            final String temp = name.substring(name.lastIndexOf('.') + 1);
            this.jsonName = temp.substring(0, 1).toLowerCase() + temp.substring(1);
			init();
		}

        public HubDescriptor(String name, ClassLoader classLoader) throws ClassNotFoundException {
            hub = (Class<? extends HubDescriptor>) Class.forName(name, true, classLoader);
            this.name = name;
            final String temp = name.substring(name.lastIndexOf('.') + 1);
            this.jsonName = temp.substring(0, 1).toLowerCase() + temp.substring(1);
            init();
        }

		@SuppressWarnings("unchecked")
		private void init() throws ClassNotFoundException {
			for(final Method m : getMethods(hub, withModifier(Modifier.PUBLIC))) {
				final Procedure procedure = new Procedure(m);
				procedures.add(procedure);
			}
		}

        public String getJsonName() {
            return jsonName;
        }

        public String getName() {
			return name;
		}

		public List<Procedure> getProcedures() {
			return procedures;
		}
		
		@Override
		public String toString() {
			return "{\"name\": \"" + name + "\", \"procedures\": " + procedures + "}";
		}
		
		String toJS() {
            StringBuilder sb = new StringBuilder();
			sb.append("//Start hub: " + name).append(CRLF);
			for(Procedure proc : procedures) {
				sb.append(proc.toJS(hub)).append(CRLF);
			}
			sb.append("//End hub: " + name).append(CRLF);
			return sb.toString();
		}
		
		public class Procedure {
			private final String name;
			private final Class<?> returnType;
			private final Map<Integer, Parameter> parameters = new HashMap<Integer, Parameter>();
			
			Procedure(Method method){
				name = method.getName();
				returnType = method.getReturnType();
				init(method);
			}
			
			private void init(Method m) {
				int i = 0;
                for(Type c : m.getGenericParameterTypes()) {
                    Parameter param = new Parameter(c.getTypeName(), i);
                    parameters.put(i, param);
                    i++;
                }
			}

			public String getName() {
				return name;
			}

			public Class<?> getReturnType() {
				return returnType;
			}
			
			public Collection<Parameter> getParameters() {
				return parameters.values();
			}
			
			public Parameter getParameter(int index) {
				return parameters.get(index);
			}

			@Override
			public String toString() {
                StringBuilder sb = new StringBuilder();
				sb.append("{\"name\": \"").append(name).append("\", \"returnType\": \"").append(returnType).append("\"");
				if (!parameters.isEmpty()) {
					sb.append(", \"parameters\": [");
					int i = 0;
					for(final Parameter p : parameters.values()) {
						sb.append(p);
						i++;
						if(i < parameters.size()) sb.append(", ");
					}
					sb.append("]");
				}
				sb.append("}");
				return sb.toString();
			}
			
			String toJS(Class<? extends HubDescriptor> hub) {
                StringBuilder sb = new StringBuilder();
				sb.append("function ").append(lowerCaseFirstChar(hub.getSimpleName())).append("_").append(name).append("(");
				for(Parameter p : parameters.values()) {
					sb.append(p.getSimpleName().toLowerCase()).append("_").append(p.index);
					if(p.index != parameters.values().size() - 1) sb.append(", ");
				}
				if(!returnType.toString().equalsIgnoreCase("void")) {
					sb.append(", callback");
				}
				sb.append(") {").append(CRLF);
				sb.append("var j = {type: 'execute', hub: '").append(hub.getName()).append("', ");
				sb.append("method: '").append(name).append("', ");
				sb.append("returnType: '").append(returnType).append("', ");
				sb.append("parameters: [");
				for(Parameter p : parameters.values()) {
					sb.append("{ value: ").append(p.getSimpleName().toLowerCase()).append("_").append(p.index);
					sb.append(", type: '").append(p.getName()).append("'}");
					if(p.index != parameters.values().size() - 1) sb.append(", ");
				}
				sb.append("]};").append(CRLF);
				sb.append("systemsend(j");
				if(!returnType.toString().equalsIgnoreCase("void")) {
					sb.append(", callback");
				}
				sb.append(");").append(CRLF);
				sb.append("}").append(CRLF);
				return sb.toString();
			}
		}
		
		public class Parameter {
			//private final Class<?> type;
            private final String typeName;
            private final String simpleName;
			private final int index;
			
			Parameter(String typeName, int index) {
				this.typeName = typeName;
				this.index = index;
                String temp = "";
                if(typeName.contains("<")) temp = typeName.substring(0, typeName.indexOf("<"));
                else temp = typeName;
                if(temp.contains(".")) temp = temp.substring(temp.lastIndexOf(".") + 1);
                simpleName = temp;
			}

			public String getName() {
				return typeName;
			}

            public String getSimpleName() {
                return simpleName;
            }

			public int getIndex() {
				return index;
			}

			@Override
			public String toString() {
				return "{\"type\": \"" + typeName + "\", \"index\": " + index + "}";
			}
		}
	}
}