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
import signalJ.models.Messages;

import java.util.*;
import java.util.stream.Collectors;

class HubsActor extends AbstractActor {
	private final HubsDescriptor hubsDescriptor = new HubsDescriptor();
    private final String newline = System.getProperty("line.separator");
    private String js;
    private String js2;

    HubsActor() {
        try {
            fillDescriptors();
        } catch (Exception e) {
            Logger.error("Error creating hub descriptor", e);
        }
        receive(
                ReceiveBuilder.match(Messages.GetJavaScript.class, request ->  sender().tell(js, self())
                ).match(Messages.GetJavaScript2.class, request ->  sender().tell(js2, self())
                ).match(Messages.Describe.class, describe -> {
                    //TODO Re-Enable describe?
                    final UUID uuid = UUID.fromString(describe.json.get("uuid").textValue());
                    final String id = describe.json.get("id").textValue();
                    final String hub = "system";
                    final String returnType = "json";
                    final String method = "describe";
                    final String returnValue = hubsDescriptor.toString();
                    //describe.user.tell(new UserActor.MethodReturn(uuid, id, returnValue, hub, method, returnType), self());
                }).match(Messages.HubJoin.class, hubJoin -> getContext().getChildren().forEach(hub -> hub.tell(hubJoin, self()))
                ).match(Messages.Execute.class, execute -> {
                    final ActorRef hub = getHub(execute.json.get("H").textValue());
                    hub.forward(execute, getContext());
                }).build()
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
            final ActorRef hubActor = createHub(hub.getSimpleName());
            hubActor.tell(new Messages.RegisterHub((Class<? extends Hub<?>>) hub, descriptor), self());
		}
        js = hubsDescriptor.toJS() + signalJ.views.js.hubs.render() + "\n";

        //SignalR
        js2 = generateProxy(hubsDescriptor);
	}

    private String generateProxy(HubsDescriptor hubsDescriptor)
    {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(HubDescriptor HubDescriptor : hubsDescriptor.getHubDescriptors()) {
            if (!first) {
                appendLine(sb);
                appendLine(sb);
                sb.append("    ");
            }
            generateType(sb, HubDescriptor);
            first = false;
        }
        if (sb.length() > 0) sb.append(";");
        final String script = signalJ.views.js.hubs2.render() + newline;
        return script.replace("/*hubs*/", sb.toString());
        //return javaScriptMinifier.Minify(script);
    }

    private void generateType(StringBuilder sb, HubDescriptor hubDescriptor) {
        sb.append(String.format("    proxies['%s'] = this.createHubProxy('%s'); ", hubDescriptor.getJsonName(), hubDescriptor.getJsonName())).append(newline);
        sb.append(String.format("        proxies['%s'].client = { };", hubDescriptor.getJsonName())).append(newline);
        sb.append(String.format("        proxies['%s'].server = {", hubDescriptor.getJsonName()));

        boolean first = true;

        for(HubsDescriptor.HubDescriptor.Procedure method : hubDescriptor.getProcedures())
        {
            if (!first)
            {
                sb.append(",").append(newline);
            }
            generateMethod(sb, method, hubDescriptor.getJsonName());
            first = false;
        }
        appendLine(sb);
        sb.append("        }");
    }

    private void generateMethod(StringBuilder sb, HubDescriptor.Procedure method, String HubDescriptorName) {
        appendLine(sb);
        sb.append(String.format("            %s: function (%s) {", method.getName(), commas(method.getParameters()))).append(newline);
        sb.append(String.format("                return proxies['%s'].invoke.apply(proxies['%s'], $.merge([\"%s\"], $.makeArray(arguments)));", HubDescriptorName, HubDescriptorName, method.getName())).append(newline);
        sb.append("             }");
    }

    private String commas(Collection<HubDescriptor.Parameter> parameters) {
        final List<String> names = parameters.stream().map(p -> p.getSimpleName().toLowerCase() + "_" + p.getIndex()).collect(Collectors.toList());
        return String.join(",", names);
    }

    private StringBuilder appendLine(StringBuilder sb) {
        return sb.append(newline);
    }

    private static ConfigurationBuilder build(String... namespaces) {
		final ConfigurationBuilder configBuilder = new ConfigurationBuilder();
		for(final String namespace : namespaces) {
			configBuilder.addUrls(ClasspathHelper.forPackage(namespace));
		}
		return configBuilder;
	}

    private ActorRef getHub(String hubName) {
        final String name = hubName.toLowerCase();
        return Optional.ofNullable(getContext().getChild(name)).orElseThrow(() -> new IllegalArgumentException());
    }

    private ActorRef createHub(String hubName) {
        final String name = hubName.toLowerCase();
        return Optional.ofNullable(getContext().getChild(name)).orElseGet(() -> context().actorOf(Props.create(HubActor.class), name));
    }
}