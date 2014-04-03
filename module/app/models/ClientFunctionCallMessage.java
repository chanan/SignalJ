package models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClientFunctionCallMessage {
	private UUID uuid;
	private String hub;
	private String function;
	private List<Parameter> args = new ArrayList<Parameter>();
	private final String type = "clientFunctionCall"; 
	
	public ClientFunctionCallMessage(UUID uuid, String hub, String function) {
		super();
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
