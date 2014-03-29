package services;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import play.Logger;
import services.ChannelsActor.ChannelJoin;
import services.SignalJActor.Execute;
import services.SignalJActor.RegisterHub;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;

import com.google.inject.Singleton;

@Singleton
public class ChannelActor extends UntypedActor {
	private final Map<UUID, ActorRef> users = new HashMap<UUID, ActorRef>();
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
			Method m = instance.getClass().getMethod(method, (Class<?>[]) null);
            m.invoke(instance, (Class<?>[]) null);
		}
		if(message instanceof Send) {
			final Send send = (Send) message;
			for(final ActorRef user : users.values()) {
				user.tell(new UserActor.Send(send.message), getSelf());
			}
		}
	}
	
//	private Method matchingMethod(String methodName, Object[] params) {
//		Method ret = null;
//		for(final Method m : instance.getClass().getm)
//		return ret;
//	}
	
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