package services;
import static org.reflections.ReflectionUtils.getAllMethods;
import static org.reflections.ReflectionUtils.withModifier;
import hubs.Hub;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.UUID;

import models.HubsDescriptor;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import play.Logger;
import services.SignalJActor.Describe;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

//TODO Make params into an array instead of a flat object
@Singleton
public class HubsActor extends UntypedActor {
	private final ActorRef signalJActor;
	private final HubsDescriptor hubsDescriptor = new HubsDescriptor();
	
	@Inject
	public HubsActor(@Named("SignalJActor") ActorRef signalJActor) {
		this.signalJActor = signalJActor;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onReceive(Object message) throws Exception {
		if(hubsDescriptor.isEmpty()) fillDescriptors();
		if(message instanceof GetJavaScript) {
			StringBuilder sb = new StringBuilder();
			sb.append(views.js.hubs.render()).append("\n");
			
			final ConfigurationBuilder configBuilder = build("hubs");
			final Reflections reflections = new Reflections(configBuilder.setScanners(new SubTypesScanner()));
			Set<Class<? extends Hub>> hubs = reflections.getSubTypesOf(Hub.class);
			for(final Class<? extends Hub> hub : hubs) {
				Logger.debug("Hub found: " + hub.getName());
				signalJActor.tell(new SignalJActor.RegisterHub(hub, hubsDescriptor.getDescriptor(hub.getName())), getSelf());
				for(Method m : getAllMethods(hub, withModifier(Modifier.PUBLIC))) {
					if(m.getDeclaringClass() != hub) continue;
					sb.append("function ").append(hub.getSimpleName()).append("_").append(m.getName()).append("(");
					int i = 0;
					for(Class<?> p : m.getParameterTypes()) {
						sb.append(p.getSimpleName().toLowerCase()).append("_").append(i);
						if(i != m.getParameterTypes().length - 1) sb.append(", ");
						i++;
					}
					if(!m.getReturnType().toString().equalsIgnoreCase("void")) {
						sb.append(", callback");
						sb.append(") {").append("\n");
						sb.append(views.js.addCallback.render()).append("\n");
					} else {
						sb.append(") {").append("\n");
					}
					sb.append("var j = {type: 'execute', hub: '").append(hub.getName()).append("', ");
					sb.append("method: '").append(m.getName()).append("', ");
					sb.append("paramCount: ").append(m.getParameterTypes().length);
					sb.append(", returnType: '").append(m.getReturnType()).append("'");
					i = 0;
					for(Class<?> p : m.getParameterTypes()) {
						sb.append(", ").append("param_").append(i);
						sb.append(": ").append(p.getSimpleName().toLowerCase()).append("_").append(i);
						i++;
					}
					i = 0;
					for(Class<?> p : m.getParameterTypes()) {
						sb.append(", ").append("paramType_").append(i);
						sb.append(": '").append(p.getName()).append("'");
						i++;
					}
					sb.append("};").append("\n");
					sb.append("systemsend(j);").append("\n");
					sb.append("}").append("\n");
				}
			}
			Logger.debug(hubsDescriptor.toString());
			getSender().tell(sb.toString(), getSelf());
		}
		if(message instanceof Describe) {
			final Describe describe = (Describe) message;
			final UUID uuid = UUID.fromString(describe.json.get("uuid").textValue());
        	final String id = describe.json.get("id").textValue();
        	final String hub = "system";
        	final String returnType = "json";
        	final String method = "describe";
        	final String returnValue = hubsDescriptor.toString();
			describe.user.tell(new UserActor.MethodReturn(uuid, id, returnValue, hub, method, returnType), getSelf());
		}
	}
	
	//TODO All this code can be moved to live inside hubsDescriptor
	private void fillDescriptors() throws ClassNotFoundException {
		final ConfigurationBuilder configBuilder = build("hubs");
		final Reflections reflections = new Reflections(configBuilder.setScanners(new SubTypesScanner()));
		Set<Class<? extends Hub>> hubs = reflections.getSubTypesOf(Hub.class);
		for(final Class<? extends Hub> hub : hubs) {
			hubsDescriptor.addDescriptor(hub.getName());
		}
	}

	private static ConfigurationBuilder build(String... namespaces) {
		final ConfigurationBuilder configBuilder = new ConfigurationBuilder();
		for(final String namespace : namespaces) {
			configBuilder.addUrls(ClasspathHelper.forPackage(namespace));
		}
		return configBuilder;
	}
	
	public static class GetJavaScript {
		
	}
}