package services;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;

public class ChannelActor extends UntypedActor {
	private final Map<UUID, ActorRef> users = new HashMap<UUID, ActorRef>();

	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Join) {
			final Join join = (Join) message;
			users.put(join.uuid, join.user);
		}
		if(message instanceof Quit) {
			final Quit quit = (Quit) message;
			users.remove(quit.uuid);
			if(users.isEmpty()) getContext().stop(getSelf());
		}
		if(message instanceof Send) {
			final Send send = (Send) message;
			for(final ActorRef user : users.values()) {
				user.tell(new UserActor.Send(send.message), getSelf());
			}
		}
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