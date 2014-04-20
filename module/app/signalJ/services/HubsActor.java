package signalJ.services;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import play.Logger;
import signalJ.GlobalHost;
import signalJ.models.HubsDescriptor;
import signalJ.models.HubsDescriptor.HubDescriptor;
import signalJ.services.SignalJActor.Describe;

import java.util.Set;
import java.util.UUID;

class HubsActor extends UntypedActor {
	private final HubsDescriptor hubsDescriptor = new HubsDescriptor();
    private String js;

	@Override
	public void onReceive(Object message) throws Exception {
		if(hubsDescriptor.isEmpty()) fillDescriptors();
		if(message instanceof GetJavaScript) {
			getSender().tell(js, getSelf());
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
        if(message instanceof HubJoin) {
            final HubJoin hubJoin = (HubJoin) message;
            for(final ActorRef hub : getContext().getChildren()) {
                hub.tell(hubJoin, getSelf());
            }
        }
        if(message instanceof SignalJActor.SendToHub) {
            final SignalJActor.SendToHub sendToHub = (SignalJActor.SendToHub) message;
            final ActorRef hub = getHub(sendToHub.hubName);
            hub.tell(new HubActor.Send(sendToHub.message), getSelf());
        }
        if(message instanceof SignalJActor.Execute) {
            final SignalJActor.Execute execute = (SignalJActor.Execute) message;
            final ActorRef hub = getHub(execute.json.get("hub").textValue());
            hub.forward(message, getContext());
        }
        if(message instanceof GetHub) {
            final GetHub getHub = (GetHub) message;
            final ActorRef hub = getHub(getHub.hubName);
            getSender().tell(hub, getSelf());
        }
	}

	@SuppressWarnings("unchecked")
	private void fillDescriptors() throws ClassNotFoundException {
        hubsDescriptor.setClassLoader(GlobalHost.getClassLoader());
		final ConfigurationBuilder configBuilder = build("hubs");
		final Reflections reflections = new Reflections(configBuilder.setScanners(new SubTypesScanner()));
		final Set<Class<? extends Hub>> hubs = reflections.getSubTypesOf(Hub.class);
		for(final Class<? extends Hub> hub : hubs) {
			Logger.debug("Hub found: " + hub.getName());
			final HubDescriptor descriptor = hubsDescriptor.addDescriptor(hub.getName());
            final ActorRef channel = getHub(hub.getName());
            channel.tell(new SignalJActor.RegisterHub((Class<? extends Hub<?>>) hub, descriptor), getSelf());
		}
        js = hubsDescriptor.toJS() + signalJ.views.js.hubs.render() + "\n";
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

    private ActorRef getHub(String hubName) {
        final ActorRef hub = getContext().getChild(hubName);
        if(hub != null) return hub;
        return getContext().actorOf(Props.create(HubActor.class), hubName);
    }

    public static class HubJoin {
        final UUID uuid;
        final ActorRef user;

        public HubJoin(UUID uuid, ActorRef user) {
            this.uuid = uuid;
            this.user = user;
        }
    }

    public static class GetHub {
        final String hubName;

        public GetHub(String hubName) {
            this.hubName = hubName;
        }
    }
}