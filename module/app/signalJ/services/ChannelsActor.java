package signalJ.services;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import akka.actor.Props;
import play.Logger;
import signalJ.models.HubsDescriptor;
import signalJ.services.SignalJActor.Execute;
import signalJ.services.SignalJActor.RegisterHub;
import signalJ.services.SignalJActor.SendToChannel;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;

public class ChannelsActor extends UntypedActor {
	private final Map<String, HubsDescriptor.HubDescriptor> descriptors = new HashMap<>();

	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof RegisterHub) {
			final RegisterHub registerHub = (RegisterHub) message;
			final String channelName = registerHub.hub.getName();
            final ActorRef channel = getChannel(channelName);
			channel.tell(registerHub, getSelf());
			if(!descriptors.containsKey(channelName)) descriptors.put(channelName, registerHub.descriptor);
		}
		if(message instanceof ChannelJoin) {
			final ChannelJoin channelJoin = (ChannelJoin) message;
			for(final ActorRef channel : getContext().getChildren()) {
				channel.tell(channelJoin, getSelf());
			}
		}
		if(message instanceof SendToChannel) {
			final SendToChannel sendToChannel = (SendToChannel) message;
			final ActorRef channel = getChannel(sendToChannel.channel);
			channel.tell(new ChannelActor.Send(sendToChannel.message), getSelf());
		}
		if(message instanceof Execute) {
            final Execute execute = (Execute) message;
			final ActorRef channel = getChannel(execute.json.get("hub").textValue());
			channel.forward(message, getContext());
		}
		if(message instanceof GetChannel) {
			final GetChannel getChannel = (GetChannel) message;
			final ActorRef channel = getChannel(getChannel.channelName);
			getSender().tell(channel, getSelf());
		}
/*        if(message instanceof SignalJActor.Quit) {
            final SignalJActor.Quit quit = (SignalJActor.Quit) message;
            for(final ActorRef channel : getContext().getChildren()) {
                channel.forward(message, getContext());
            }
        }*/
	}

    private ActorRef getChannel(String channelName) {
        final ActorRef channel = getContext().getChild(channelName);
        if(channel != null) return channel;
        return getContext().actorOf(Props.create(ChannelActor.class), channelName);
    }
	
	public static class ChannelJoin {
		final UUID uuid;
		final ActorRef user; 
		
		public ChannelJoin(UUID uuid, ActorRef user) {
			this.uuid = uuid;
			this.user = user;
		}
	}
	
	public static class GetChannel {
		final String channelName;
		
		public GetChannel(String channelName) {
			this.channelName = channelName;
		}
	}
}