package services;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import play.Logger;
import services.SignalJActor.Join;
import services.SignalJActor.Quit;
import services.SignalJActor.Send;
import services.SignalJActor.SendToAll;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akkaGuice.PropsContext;

import com.google.inject.Singleton;


//TODO Had to remove this, make this work one day!
@Singleton
public class UsersActor extends UntypedActor {
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
		if(message instanceof SendToAll) {
			final SendToAll sendToAll = (SendToAll) message;
			for(final ActorRef user : users.values()) {
				user.tell(new UserActor.Send(sendToAll.message), getSelf());
			}
		}
		if(message instanceof Send) {
			final Send send = (Send) message;
			final ActorRef user = users.get(send.uuid);
			user.tell(new UserActor.Send(send.message), getSelf());
		}
		if (message instanceof GetUser) {
			final GetUser getUser = (GetUser) message;
			final ActorRef user = users.get(getUser.uuid);
			getSender().tell(user, getSelf());
		}
		
//		if(message instanceof ChannelJoin) {
//			final ChannelJoin channelJoin = (ChannelJoin) message;
//			final ActorRef user = users.get(channelJoin.uuid);
//			user.tell(new UserActor.ChannelJoin(channelJoin.channelName, channel), getSelf());	
//		}
	}
	
	public static class GetUser{
		final UUID uuid;
		
		public GetUser(UUID uuid) {
			this.uuid = uuid;
		}
	}
}