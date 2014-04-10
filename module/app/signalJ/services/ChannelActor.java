package signalJ.services;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import play.Logger;
import signalJ.GlobalHost;
import signalJ.models.HubsDescriptor;
import signalJ.services.ChannelsActor.ChannelJoin;
import signalJ.services.SignalJActor.Execute;
import signalJ.services.SignalJActor.RegisterHub;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class ChannelActor extends UntypedActor {
	private final Map<UUID, ActorRef> users = new HashMap<UUID, ActorRef>();
	private final static ObjectMapper mapper = new ObjectMapper();
	private final ActorRef signalJActor = ActorLocator.getSignalJActor();
	private HubsDescriptor.HubDescriptor hubDescriptor;
	private Class<? extends Hub<?>> clazz; 

	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof RegisterHub) {
			//TODO: throw error is already registered
			final RegisterHub registerHub = (RegisterHub) message;
			hubDescriptor = registerHub.descriptor;
			clazz = registerHub.hub;
			Logger.debug("Registered channel: " + registerHub.hub.getName());
		}
		if(message instanceof ChannelJoin) {
			final ChannelJoin channelJoin = (ChannelJoin) message;
			users.put(channelJoin.uuid, channelJoin.user);
		}
		if(message instanceof Quit) {
			final Quit quit = (Quit) message;
			users.remove(quit.uuid);
			if(users.isEmpty()) getContext().stop(getSelf());
		}
		if(message instanceof Execute) {
			final Execute execute = (Execute) message;
			final UUID uuid = UUID.fromString(execute.json.get("uuid").textValue());
			final String hub = execute.json.get("hub").textValue();
			final Hub<?> instance = GlobalHost.getDependencyResolver().getHubInstance(hub);
			instance.setConnectionId(uuid);
			instance.setHubClassName(hub);
			final String method = execute.json.get("method").textValue();
			final Class<?>[] classes = getParamTypeList(execute.json);
			final Method m = instance.getClass().getMethod(method, classes);
            final Object ret = m.invoke(instance, getParams(execute.json, classes));
            if(ret != null) {
            	final String id = execute.json.get("id").textValue();
            	final String returnType = execute.json.get("returnType").textValue();
            	final ActorRef user = users.get(uuid);
            	user.tell(new UserActor.MethodReturn(uuid, id, ret, hub, method, returnType), getSelf());
            }
		}
		if(message instanceof ClientFunctionCall) {
			final ClientFunctionCall clientFunctionCall = (ClientFunctionCall) message;
			switch(clientFunctionCall.sendType) {
			case All:
				for(final ActorRef user : users.values()) {
					user.forward(message, getContext());
				}
				break;
			case Caller:
				users.get(clientFunctionCall.caller).forward(message, getContext());
				break;
			case Others:
				for(final UUID uuid : users.keySet()) {
					if(uuid.equals(clientFunctionCall.caller)) continue;
					users.get(uuid).forward(message, getContext());
				}
				break;
			case Clients:
				for(final UUID uuid : clientFunctionCall.clients) {
					users.get(uuid).forward(message, getContext());
				}
				break;
			case AllExcept:
				final List<UUID> allExcept = Arrays.asList(clientFunctionCall.allExcept);
				for(final UUID uuid : users.keySet()) {
					if(allExcept.contains(uuid)) continue;
					users.get(uuid).forward(message, getContext());
				}
				break;
			case Group:
				signalJActor.forward(message, getContext());
				break;
			case InGroupExcept:
				signalJActor.forward(message, getContext());
				break;
			default:
				break;
			
			}
		}
	}
	
	private Class<?>[] getParamTypeList(JsonNode node) throws ClassNotFoundException {
		final List<Class<?>> ret = new ArrayList<Class<?>>();
		for(final JsonNode param : node.get("parameters")) {
			final Class<?> clazz = getClassForName(param.get("type").textValue());
			ret.add(clazz);
		}
		return ret.toArray(new Class<?>[0]);
	}
	
	private Class<?> getClassForName(String className) throws ClassNotFoundException {
		switch (className.toLowerCase()) {
			case "byte":
				return byte.class;
			case "short":
				return short.class;
			case "int":
				return int.class;
			case "long":
				return long.class;
			case "float":
				return float.class;
			case "double":
				return double.class;
			case "char":
				return char.class;
			case "boolean":
				return boolean.class;
			default:
				return Class.forName(className);
		}
	}
	
	private Object[] getParams(JsonNode node, Class<?>[] params ) throws ClassNotFoundException, JsonParseException, JsonMappingException, IOException {
		final List<Object> ret = new ArrayList<Object>();
		for(final JsonNode param : node.get("parameters")) {
			final Class<?> clazz = getClassForName(param.get("type").textValue());
			final Object obj = mapper.readValue(mapper.treeAsTokens(param.get("value")), clazz);
			ret.add(obj);
		}
		return ret.toArray();
	}
	
	public static class Join {
		final UUID uuid;
		final ActorRef user;

		public Join(UUID uuid, ActorRef user) {
			this.uuid = uuid;
			this.user = user;
		}
	}
	
	public static class Quit {
		final UUID uuid;

	public Quit(UUID uuid) {
			this.uuid = uuid;
		}
	}
	
	public static class Send {
		final String message;
		
		public Send(String message) {
			this.message = message;
		}
	}
	
	//TODO maybe use inheritance to make this more sane
	public static class ClientFunctionCall {
		final String channelName;
		final String name;
		final Object[] args;
		final SendType sendType;
		final UUID caller;
		final Method method;
		final UUID[] clients;
		final UUID[] allExcept;
		final String groupName;
		
		public ClientFunctionCall(Method method, String channelName, UUID caller, SendType sendType, String name, Object[] args, UUID[] clients, UUID[] allExcept, String groupName) {
			this.channelName = channelName;
			this.caller = caller;
			this.sendType = sendType;
			this.name = name;
			this.args = args;
			this.method = method;
			this.clients = clients;
			this.allExcept = allExcept;
			this.groupName = groupName;
		}

		public enum SendType
		{
			All,
			Others,
			Caller,
			Clients, 
			AllExcept, 
			Group, 
			InGroupExcept
		}
	}
}