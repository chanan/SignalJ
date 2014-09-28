package signalJ.services;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import play.Logger;
import signalJ.GlobalHost;
import signalJ.models.HubsDescriptor;
import signalJ.models.HubsDescriptor.HubDescriptor;
import signalJ.services.SignalJActor.Describe;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

class HubsActor extends AbstractActor {
	private final HubsDescriptor hubsDescriptor = new HubsDescriptor();
    private String js;

    HubsActor() {
        try {
            fillDescriptors();
        } catch (Exception e) {
            Logger.error("Error creating hub descriptor", e);
        }
        receive(
                ReceiveBuilder.match(
                        GetJavaScript.class, request ->  sender().tell(js, self())
                ).match(
                        Describe.class, describe -> {
                            final UUID uuid = UUID.fromString(describe.json.get("uuid").textValue());
                            final String id = describe.json.get("id").textValue();
                            final String hub = "system";
                            final String returnType = "json";
                            final String method = "describe";
                            final String returnValue = hubsDescriptor.toString();
                            describe.user.tell(new UserActor.MethodReturn(uuid, id, returnValue, hub, method, returnType), self());
                        }
                ).match(
                        HubJoin.class, hubJoin -> getContext().getChildren().forEach(hub -> hub.tell(hubJoin, self()))
                ).match(
                        SignalJActor.SendToHub.class, sendToHub -> {
                            final ActorRef hub = getHub(sendToHub.hubName);
                            hub.tell(new HubActor.Send(sendToHub.message), self());
                        }
                ).match(
                        SignalJActor.Execute.class, execute -> {
                            final ActorRef hub = getHub(execute.json.get("hub").textValue());
                            hub.forward(execute, getContext());
                        }
                ).match(
                        GetHub.class, getHub -> {
                            final ActorRef hub = getHub(getHub.hubName);
                            sender().tell(hub, self());
                        }
                ).build()
        );
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
            channel.tell(new SignalJActor.RegisterHub((Class<? extends Hub<?>>) hub, descriptor), self());
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
        return Optional.ofNullable(getContext().getChild(hubName)).orElseGet(() -> context().actorOf(Props.create(HubActor.class), hubName));
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