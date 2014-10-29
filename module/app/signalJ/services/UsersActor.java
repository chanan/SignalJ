package signalJ.services;

import akka.actor.*;
import akka.japi.pf.ReceiveBuilder;
import play.Logger;
import signalJ.infrastructure.ProtectedData;
import signalJ.services.SignalJActor.Join;
import signalJ.services.SignalJActor.Quit;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

class UsersActor extends AbstractActor {
    private final ProtectedData protectedData;
	//TODO: Make this a supervisor

    UsersActor(ProtectedData protectedData) {
        this.protectedData = protectedData;
        receive(
                ReceiveBuilder.match(
                        Join.class, join -> {
                            final ActorRef user = getUser(join.uuid);
                            user.forward(join, context());
                        }
                ).match(
                        Quit.class, quit -> {
                            final ActorRef user = getUser(quit.uuid);
                            user.tell(PoisonPill.getInstance(), self());
                            Logger.debug(quit.uuid + " logged off");
                        }
                ).match(
                        GetUser.class, getUser -> {
                            final ActorRef user =  getUser(getUser.uuid);
                            sender().tell(user, self());
                        }
                ).match(
                        UserActor.MethodReturn.class, methodReturn -> {
                            final ActorRef user = getUser(methodReturn.uuid);
                            user.forward(methodReturn, context());
                        }
                ).match(
                        HubActor.ClientFunctionCall.class, clientFunctionCall -> {
                            switch (clientFunctionCall.sendType) {
                                case All:
                                    getContext().getChildren().forEach(user -> user.forward(clientFunctionCall, context()));
                                    break;
                                case Caller:
                                case Group:
                                case InGroupExcept:
                                    getContext().getChild(clientFunctionCall.caller.toString()).forward(clientFunctionCall, context());
                                    break;
                                case Others:
                                    final ActorRef caller = getContext().getChild(clientFunctionCall.caller.toString());
                                    getContext().getChildren().forEach(user -> {
                                        if (!user.equals(caller)) user.forward(clientFunctionCall, context());
                                    });
                                    break;
                                case Clients:
                                    Arrays.asList(clientFunctionCall.clients).forEach(uuid -> {
                                        final ActorRef user = getContext().getChild(uuid.toString());
                                        user.forward(clientFunctionCall, getContext());
                                    });
                                    break;
                                case AllExcept:
                                    final List<ActorRef> allExcept = getUsers(clientFunctionCall.allExcept);
                                    getContext().getChildren().forEach(user -> {
                                        if (!allExcept.contains(user)) user.forward(clientFunctionCall, context());
                                    });
                                    break;
                            }
                        }
                ).build()
        );
    }

    private List<ActorRef> getUsers(UUID[] uuids) {
        return Arrays.asList(uuids).stream().map(uuid -> getContext().getChild(uuid.toString())).collect(Collectors.toList());
    }

    private ActorRef getUser(UUID uuid) {
        return Optional.ofNullable(getContext().getChild(uuid.toString())).orElseGet(() -> context().actorOf(Props.create(UserActor.class, protectedData), uuid.toString()));
    }
	
	public static class GetUser{
		final UUID uuid;
		
		public GetUser(UUID uuid) {
			this.uuid = uuid;
		}
	}
}