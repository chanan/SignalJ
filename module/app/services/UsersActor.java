package services;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import play.Logger;
import services.SignalJActor.Join;
import services.SignalJActor.Quit;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akkaGuice.PropsContext;

import com.google.inject.Singleton;

//TODO Had to remove this, make this work one day!
@Singleton
class UsersActor extends UntypedActor {
	//TODO: Make this a supervisor
	private final Map<UUID, ActorRef> users = new HashMap<UUID, ActorRef>(); //TODO: Is this needed or can we do everything with actor selection?

	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Join) {
			final Join join = (Join) message;
			final ActorRef user = getContext().actorOf(PropsContext.get(UserActor.class), join.uuid.toString());
			users.put(join.uuid, user);
			user.tell(join, getSelf());
			Logger.debug(join.uuid + " logged on");
		}
		if(message instanceof Quit) {
			final Quit quit = (Quit) message;
			final ActorRef user = users.remove(quit.uuid);
			user.tell(new UserActor.Quit(), getSelf());
			Logger.debug(quit.uuid + " logged off");
		}
		if (message instanceof GetUser) {
			final GetUser getUser = (GetUser) message;
			final ActorRef user = users.get(getUser.uuid);
			getSender().tell(user, getSelf());
		}
	}
	
	public static class GetUser{
		final UUID uuid;
		
		public GetUser(UUID uuid) {
			this.uuid = uuid;
		}
	}
}