package signalJ.services;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.UntypedActor;
import play.Logger;
import signalJ.services.SignalJActor.Join;
import signalJ.services.SignalJActor.Quit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class UsersActor extends UntypedActor {
	//TODO: Make this a supervisor

	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Join) {
			final Join join = (Join) message;
			final ActorRef user = getUser(join.uuid);
			user.forward(join, getContext());
		}
		if(message instanceof Quit) {
			final Quit quit = (Quit) message;
            final ActorRef user = getUser(quit.uuid);
            user.tell(PoisonPill.getInstance(), getSelf());
			Logger.debug(quit.uuid + " logged off");
		}
		if (message instanceof GetUser) {
			final GetUser getUser = (GetUser) message;
			final ActorRef user =  getUser(getUser.uuid);
			getSender().tell(user, getSelf());
		}
        if(message instanceof UserActor.MethodReturn) {
            final UserActor.MethodReturn methodReturn = (UserActor.MethodReturn) message;
            final ActorRef user = getUser(methodReturn.uuid);
            user.forward(message, getContext());
        }
        if(message instanceof HubActor.ClientFunctionCall) {
            final HubActor.ClientFunctionCall clientFunctionCall = (HubActor.ClientFunctionCall) message;
            switch (clientFunctionCall.sendType) {
                case All:
                    for (final ActorRef user : getContext().getChildren()) {
                        user.forward(message, getContext());
                    }
                    break;
                case Caller:
                case Group:
                case InGroupExcept:
                    getContext().getChild(clientFunctionCall.caller.toString()).forward(message, getContext());
                    break;
                case Others:
                    final ActorRef caller = getContext().getChild(clientFunctionCall.caller.toString());
                    for (final ActorRef user : getContext().getChildren()) {
                        if (user.equals(caller)) continue;
                        user.forward(message, getContext());
                    }
                    break;
                case Clients:
                    for (final UUID uuid : clientFunctionCall.clients) {
                        final ActorRef user = getContext().getChild(uuid.toString());
                        user.forward(message, getContext());
                    }
                    break;
                case AllExcept:
                    final List<ActorRef> allExcept = getUsers(clientFunctionCall.allExcept);
                    for (final ActorRef user : getContext().getChildren()) {
                        if (allExcept.contains(user)) continue;
                        user.forward(message, getContext());
                    }
                    break;
            }
        }
	}

    private List<ActorRef> getUsers(UUID[] uuids) {
        final List<ActorRef> actors = new ArrayList<>();
        for(final UUID uuid : uuids) {
            actors.add(getContext().getChild(uuid.toString()));
        }
        return actors;
    }

    private ActorRef getUser(UUID uuid) {
        final ActorRef user = getContext().getChild(uuid.toString());
        if(user != null) return user;
        return getContext().actorOf(Props.create(UserActor.class), uuid.toString());
    }
	
	public static class GetUser{
		final UUID uuid;
		
		public GetUser(UUID uuid) {
			this.uuid = uuid;
		}
	}
}