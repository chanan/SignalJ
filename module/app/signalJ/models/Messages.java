package signalJ.models;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Messages {
	public static class ClientFunctionCall {
		private final UUID uuid;
		private final String hub;
		private final String function;
		private final List<Parameter> args = new ArrayList<Parameter>();
		private final String type = "clientFunctionCall"; 
		
		public ClientFunctionCall(UUID uuid, String hub, String function) {
			this.uuid = uuid;
			this.hub = hub;
			this.function = function;
		}
		
		public UUID getUuid() {
			return uuid;
		}
		
		public String getHub() {
			return hub;
		}
		
		public String getFunction() {
			return function;
		}
		
		public List<Parameter> getArgs() {
			return args;
		}
		
		public String getType() {
			return type;
		}
		
		public Parameter addParameter(String name, Object value) {
			Parameter p = new Parameter(name, value);
			args.add(p);
			return p;
		}
		
		@Override
		public String toString() {
			return "{uuid: " + uuid + ", hub: " + hub + ", function: " + function + ", args: [" + args + "], type: " + type + "}";
		}
	}

	public static class MethodReturn {
		private final UUID uuid;
		private final String id;
		private final String hub;
		private final String method;
		private final String returnType;
		private final Object returnValue;
		private final String type = "methodReturn";
		
		public MethodReturn(UUID uuid, String id, String hub, String method, String returnType, Object returnValue) {
			this.uuid = uuid;
			this.id = id;
			this.hub = hub;
			this.method = method;
			this.returnType = returnType;
			this.returnValue = returnValue;
		}
		
		public UUID getUuid() {
			return uuid;
		}
		
		public String getId() {
			return id;
		}
		
		public String getHub() {
			return hub;
		}
		
		public String getMethod() {
			return method;
		}
		
		public String getReturnType() {
			return returnType;
		}
		
		public Object getReturnValue() {
			return returnValue;
		}
		
		public String getType() {
			return type;
		}
		
		@Override
		public String toString() {
			return "{uuid: " + uuid + ", id: " + id + ", hub: " + hub + ", method: " + method + ", returnType: " + returnType + ", returnValue: " + returnValue + "}";
		}
	}
	
	public static class Parameter {
		private final String name;
		private final Object value;
		
		public String getName() {
			return name;
		}
		
		public Object getValue() {
			return value;
		}
		
		@Override
		public String toString() {
			return "{name: " + name + ", value: " + value.toString() + "}";
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Parameter other = (Parameter) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}
		
		public Parameter(String name, Object value) {
			this.name = name;
			this.value = value;
		}	
	}
}