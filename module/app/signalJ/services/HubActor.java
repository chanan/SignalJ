package signalJ.services;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import play.Logger;
import signalJ.GlobalHost;
import signalJ.SignalJPlugin;
import signalJ.models.HubsDescriptor;
import signalJ.services.SignalJActor.Execute;
import signalJ.services.SignalJActor.RegisterHub;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class HubActor extends AbstractActor {
	private final static ObjectMapper mapper = new ObjectMapper();
	private final ActorRef signalJActor = SignalJPlugin.getSignalJActor();
	private HubsDescriptor.HubDescriptor hubDescriptor;
	private Class<? extends Hub<?>> clazz;

    HubActor() {
        receive(
                ReceiveBuilder.match(RegisterHub.class, registerHub -> {
                    hubDescriptor = registerHub.descriptor;
                    clazz = registerHub.hub;
                    Logger.debug("Registered hub actor: " + clazz.getName());
                }).match(Execute.class, execute -> {
                    Logger.debug("Clazz: " + clazz.getName() + " " + clazz.getSimpleName());
                    final UUID uuid = execute.uuid;
                    final Hub<?> instance = (Hub<?>)GlobalHost.getHub(clazz.getName());//   .getDependencyResolver().getHubInstance(hub, _classLoader);
                    instance.setConnectionId(uuid);
                    instance.setHubClassName(clazz.getSimpleName());
                    final String methodName = execute.json.get("M").textValue();
                    //final Class<?>[] classes = getParamTypeList(execute.json);
                    //final Method m = instance.getClass().getMethod(method, classes);
                    final Method m = getMethod(instance, methodName, execute.json.get("A"));
                    final Object ret = m.invoke(instance, getParams(m, execute.json.get("A")));
                    /*if(ret != null) {
                        final String id = execute.json.get("id").textValue();
                        final String returnType = execute.json.get("returnType").textValue();
                        signalJActor.tell(new UserActor.MethodReturn(uuid, id, ret, hub, method, returnType), self());
                    }*/
                }).build()
        );
    }

    private Method getMethod(Hub<?> instance, String methodName, JsonNode args) {
        Method ret = null;
        for(Method m : instance.getClass().getDeclaredMethods()) {
            boolean match = false;
            if(m.getName().equals(methodName) && m.getParameterCount() == args.size()) {
                boolean parseError = false;
                for(int i = 0; i < m.getParameterCount(); i++) {
                    final Class<?> clazz = m.getParameterTypes()[i];
                    try {
                        final Object obj = mapper.readValue(mapper.treeAsTokens(args.get(i)), clazz);
                    } catch (Exception e) {
                        Logger.error("Parse error", e);
                        parseError = true;
                        break;
                    }
                }
                if(!parseError) match = true;
            }
            if(match) {
                ret = m;
                break;
            }
        }
        return ret;
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
        String temp;
        if(className.contains("<")) temp = className.substring(0, className.indexOf("<"));
        else temp = className;
		switch (temp.toLowerCase()) {
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
				return Class.forName(temp, false, GlobalHost.getClassLoader());
		}
	}
	
	private Object[] getParams(Method m, JsonNode args) throws ClassNotFoundException, JsonParseException, JsonMappingException, IOException {
		final List<Object> ret = new ArrayList<Object>();
        for(int i = 0; i < m.getParameterCount(); i++) {
            final Class<?> clazz = m.getParameterTypes()[i];
            final Object obj = mapper.readValue(mapper.treeAsTokens(args.get(i)), clazz);
            ret.add(obj);
        }
        return ret.toArray();
		/*for(final JsonNode param : node.get("parameters")) {
            final String className = param.get("type").textValue();
            if(className.contains("<")) {
                final String className1 = className.substring(0, className.indexOf("<"));
                final String className2 = className.substring(className.indexOf("<") + 1, className.length() - 1);
                final Class<?> clazz1 = getClassForName(className1);
                final Class<?> clazz2 = getClassForName(className2);
                final JavaType javaType = mapper.getTypeFactory().constructParametricType(clazz1, clazz2);
                final Object obj = mapper.readValue(mapper.treeAsTokens(param.get("value")), javaType);
                ret.add(obj);
            } else {
                final Class<?> clazz = getClassForName(param.get("type").textValue());
                final Object obj = mapper.readValue(mapper.treeAsTokens(param.get("value")), clazz);
                ret.add(obj);
            }
		}
		return ret.toArray();*/
	}
	
	public static class Join {
		final UUID uuid;
		final ActorRef user;

		public Join(UUID uuid, ActorRef user) {
			this.uuid = uuid;
			this.user = user;
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
		final String hubName;
		final String name;
		final Object[] args;
		final SendType sendType;
		final UUID caller;
		final Method method;
		final UUID[] clients;
		final UUID[] allExcept;
		final String groupName;
		
		public ClientFunctionCall(Method method, String hubName, UUID caller, SendType sendType, String name, Object[] args, UUID[] clients, UUID[] allExcept, String groupName) {
			this.hubName = hubName;
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