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
import signalJ.models.CallerState;
import signalJ.models.HubsDescriptor;
import signalJ.models.Messages;
import signalJ.models.RequestContext;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

class HubActor extends AbstractActor {
	private final static ObjectMapper mapper = new ObjectMapper();
	private final ActorRef signalJActor = SignalJPlugin.getSignalJActor();
	private HubsDescriptor.HubDescriptor hubDescriptor;
	private Class<? extends Hub<?>> clazz;

    HubActor() {
        receive(
                ReceiveBuilder.match(Messages.RegisterHub.class, registerHub -> {
                    hubDescriptor = registerHub.descriptor;
                    clazz = registerHub.hub;
                    Logger.debug("Registered hub actor: " + hubDescriptor.getJsonName());
                }).match(Messages.Execute.class, execute -> {
                    final UUID uuid = execute.uuid;
                    final String methodName = execute.json.get("M").textValue();
                    try {
                        final Hub<?> instance = (Hub<?>) GlobalHost.getHub(clazz.getName());//   .getDependencyResolver().getHubInstance(hub, _classLoader);
                        final RequestContext context = new RequestContext(uuid, execute.json.get("I").asInt());
                        instance.setContext(context);
                        instance.setCallerState(getState(execute.json.get("S")));
                        final Method m = getMethod(instance, methodName, execute.json.get("A"));
                        final Object ret = m.invoke(instance, getParams(m, execute.json.get("A")));
                        instance.getCallerState().getChanges().ifPresent(changes -> signalJActor.tell(new Messages.StateChange(uuid, changes, context.messageId), self()));
                        if (ret == null)
                            signalJActor.tell(new Messages.ClientCallEnd(context), self());
                        else
                            signalJActor.tell(new Messages.MethodReturn(context, ret), self());
                    } catch (Exception e) {
                        Logger.error("Error in executing hub method", e);
                        signalJActor.tell(new Messages.Error(uuid, String.format("Error occurred while executing %s.%s", hubDescriptor.getJsonName(), methodName)), self());
                    }
                }).build()
        );
    }

    private CallerState getState(JsonNode json) {
        final Map<String, String> map = new HashMap<>();
        final Iterator<Map.Entry<String, JsonNode>> iter = json.fields();
        while(iter.hasNext()) {
            final Map.Entry<String, JsonNode> entry = iter.next();
            map.put(entry.getKey(), entry.getValue().textValue());
        }
        return new CallerState(map);
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
		final List<Object> ret = new ArrayList<>();
        for(int i = 0; i < m.getParameterCount(); i++) {
            final Type type = m.getGenericParameterTypes()[i];
            if(type.getTypeName().contains("<")) {
                final String genericClassName = type.getTypeName().substring(0, type.getTypeName().indexOf("<"));
                final String className = type.getTypeName().substring(type.getTypeName().indexOf("<") + 1, type.getTypeName().length() - 1);
                final Class<?> genericClazz = getClassForName(genericClassName);
                final Class<?> clazz = getClassForName(className);
                final JavaType javaType = mapper.getTypeFactory().constructParametricType(genericClazz, clazz);
                final Object obj = mapper.readValue(mapper.treeAsTokens(args.get(i)), javaType);
                ret.add(obj);
            } else {
                final Class<?> clazz = m.getParameterTypes()[i];
                final Object obj = mapper.readValue(mapper.treeAsTokens(args.get(i)), clazz);
                ret.add(obj);
            }
        }
        return ret.toArray();
	}
}