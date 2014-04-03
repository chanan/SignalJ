package models;

public class Parameter {
	private String name;
	private Object value;
	
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
