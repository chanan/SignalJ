package services;
import static org.reflections.ReflectionUtils.getAllMethods;
import static org.reflections.ReflectionUtils.withModifier;
import hubs.Hub;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.UUID;

import models.HubsDescriptor;
import models.HubsDescriptor.HubDescriptor;

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
			StringBuffer sb = new StringBuffer();
			sb.append(hubsDescriptor.toJS());
			sb.append(views.js.hubs.render()).append("\n");
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
			Logger.debug("Hub found: " + hub.getName());
			HubDescriptor descriptor = hubsDescriptor.addDescriptor(hub.getName());
			signalJActor.tell(new SignalJActor.RegisterHub(hub, descriptor), getSelf());
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