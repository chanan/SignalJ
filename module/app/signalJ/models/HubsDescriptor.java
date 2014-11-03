package signalJ.models;

import signalJ.GlobalHost;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;

import static org.reflections.ReflectionUtils.getMethods;
import static org.reflections.ReflectionUtils.withModifier;

public class HubsDescriptor {
	private final Map<String, HubDescriptor> hubs = new HashMap<>();
    private String toString;
	
	public HubDescriptor addDescriptor(String name, String hubName) throws ClassNotFoundException {
		HubDescriptor hub = new HubDescriptor(name, hubName);
		hubs.put(name, hub);
		return hub;
	}
	
	public Collection<HubDescriptor> getHubDescriptors() {
		return hubs.values();
	}
	
	public HubDescriptor getDescriptor(String name) {
		return hubs.get(name);
	}
	
	public int size() {
		return hubs.size();
	}
	
	@Override
	public String toString() {
        if(toString != null) return toString;
        StringBuilder sb = new StringBuilder();
		sb.append("{\"hubs\": [");
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
	
	public class HubDescriptor {
		private final String name;
        private final String jsonName;
		private final Class<? extends HubDescriptor> hub;
		private final List<Procedure> procedures = new ArrayList<Procedure>();

		@SuppressWarnings("unchecked")
		public HubDescriptor(String name, String hubName) throws ClassNotFoundException {
			hub = (Class<? extends HubDescriptor>) Class.forName(name, false, GlobalHost.getClassLoader());
			this.name = name;
            this.jsonName = hubName.substring(0, 1).toLowerCase() + hubName.substring(1);
			init();
		}

		@SuppressWarnings("unchecked")
		private void init() throws ClassNotFoundException {
			for(final Method m : getMethods(hub, withModifier(Modifier.PUBLIC))) {
				final Procedure procedure = new Procedure(m);
				addProcedure(procedure);
			}
		}

        private void addProcedure(Procedure procedure) {
            Optional<Procedure> proc = procedures.stream()
                    .filter(p -> p.getName().equals(procedure.getName())).findFirst();
            proc.ifPresent(p -> {
                if(procedure.getParameters().size() < p.getParameters().size()) {
                    procedures.remove(p);
                    procedures.add(procedure);
                }
            });
            if(!proc.isPresent()) procedures.add(procedure);
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
		}
		
		public class Parameter {
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