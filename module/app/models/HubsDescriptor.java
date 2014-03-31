package models;
import static org.reflections.ReflectionUtils.getAllMethods;
import static org.reflections.ReflectionUtils.withModifier;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HubsDescriptor {
	public final static String VERSION = "1.0";
	public final Map<String, HubDescriptor> hubs = new HashMap<>();
	private final static String CRLF = "\n";
	
	public HubDescriptor addDescriptor(String name) throws ClassNotFoundException {
		final HubDescriptor hub = new HubDescriptor(name);
		hubs.put(name, hub);
		return hub;
	}
	
	public Collection<HubDescriptor> getHubDescriptors() {
		return hubs.values();
	}
	
	public HubDescriptor getDescriptor(String name) {
		return hubs.get(name);
	}
	
	public boolean isEmpty() {
		return hubs.isEmpty();
	}
	
	public int size() {
		return hubs.size();
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("{\"version\": \"").append(VERSION).append("\", \"hubs\": [");
		int i = 0;
		for(final HubDescriptor hub : hubs.values()) {
			sb.append(hub);
			i++;
			if(i < hubs.size()) sb.append(", ");
		}
		sb.append("]}");
		return sb.toString();
	}
	
	public String toJS() {
		StringBuffer sb = new StringBuffer();
		sb.append("//Hubs version: ").append(VERSION).append(CRLF);
		for(final HubDescriptor hub : hubs.values()) {
			sb.append(hub.toJS()).append(CRLF);
		}
		return sb.toString();
	}
	
	public class HubDescriptor {
		private final String name;
		private final Class<? extends HubDescriptor> hub;
		private final List<Procedure> procedures = new ArrayList<Procedure>();

		@SuppressWarnings("unchecked")
		public HubDescriptor(String name) throws ClassNotFoundException {
			this.name = name;
			hub = (Class<? extends HubDescriptor>) Class.forName(name);
			init();
		}
		
		@SuppressWarnings("unchecked")
		private void init() throws ClassNotFoundException {
			for(final Method m : getAllMethods(hub, withModifier(Modifier.PUBLIC))) {
				final Procedure procedure = new Procedure(m);
				procedures.add(procedure);
			}
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
			StringBuffer sb = new StringBuffer();
			sb.append("//Start hub: " + name).append(CRLF);
			for(Procedure proc : procedures) {
				sb.append(proc.toJS(hub)).append(CRLF);
			}
			sb.append("//End hub: " + name).append(CRLF);
			return sb.toString();
		}
		
		class Procedure {
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
				for(Class<?> p : m.getParameterTypes()) {
					Parameter param = new Parameter(p, i);
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
				StringBuffer sb = new StringBuffer();
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
				StringBuffer sb = new StringBuffer();
				sb.append("function ").append(hub.getSimpleName()).append("_").append(name).append("(");
				for(Parameter p : parameters.values()) {
					sb.append(p.type.getSimpleName().toLowerCase()).append("_").append(p.index);
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
					sb.append("{ value: ").append(p.type.getSimpleName().toLowerCase()).append("_").append(p.index);
					sb.append(", type: '").append(p.type.getName()).append("'}");
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
		
		class Parameter {
			private final Class<?> type;
			private final int index;
			
			Parameter(Class<?> type, int index) {
				this.type = type;
				this.index = index;
			}

			public Class<?> getType() {
				return type;
			}

			public int getIndex() {
				return index;
			}

			@Override
			public String toString() {
				return "{\"type\": \"" + type + "\", \"index\": " + index + "}";
			}
		}
	}
}