package services;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import play.Logger;
import services.ChannelsActor.ChannelJoin;
import services.SignalJActor.Execute;
import services.SignalJActor.RegisterHub;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Singleton;

@Singleton
public class ChannelActor extends UntypedActor {
	private final Map<UUID, ActorRef> users = new HashMap<UUID, ActorRef>();
	private final static ObjectMapper mapper = new ObjectMapper();
	private Object instance;

	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof RegisterHub) {
			final RegisterHub registerHub = (RegisterHub) message;
			instance = registerHub.hub.newInstance();
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
			final String method = execute.json.get("method").textValue();
			Class<?>[] classes = getParamTypeList(execute.json);
			Method m = instance.getClass().getMethod(method, classes);
            m.invoke(instance, getParams(execute.json, classes));
		}
		if(message instanceof Send) {
			final Send send = (Send) message;
			for(final ActorRef user : users.values()) {
				user.tell(new UserActor.Send(send.message), getSelf());
			}
		}
	}
	
	private Class<?>[] getParamTypeList(JsonNode node) throws ClassNotFoundException {
		final List<Class<?>> ret = new ArrayList<Class<?>>();
		final int paramCount = node.get("paramCount").intValue();
		for(int i = 0; i < paramCount; i++) {
			final Class<?> clazz = getClassForName(node.get("paramType_" + i).textValue());
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
	
	private Object[] getParams(JsonNode node, Class<?>[] params ) throws JsonParseException, JsonMappingException, IOException {
		final List<Object> ret = new ArrayList<Object>();
		final int paramCount = node.get("paramCount").intValue();
		for(int i = 0; i < paramCount; i++) {
			final JsonParser param = mapper.treeAsTokens(node.get("param_" + i));
			final Class<?> clazz = params[i];
			Object obj = mapper.readValue(param, clazz);
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
}