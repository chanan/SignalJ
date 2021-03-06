package signalJ.services;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import signalJ.infrastructure.ProtectedData;
import signalJ.models.Messages;
import signalJ.models.TransportJoinMessage;
import signalJ.models.TransportReconnectMessage;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

class UsersActor extends AbstractActor {
	//TODO: Make this a supervisor

    UsersActor() {
        receive(
                ReceiveBuilder.match(TransportJoinMessage.class, join -> {
                    final ActorRef user = getUser(join.getContext().connectionId);
                    user.forward(join, context());
                }).match(Messages.Quit.class, quit -> {
                    final ActorRef user = getUser(quit.uuid);
                    user.tell(quit, self());
                }).match(Messages.MethodReturn.class, methodReturn -> {
                    final ActorRef user = getUser(methodReturn.context.connectionId);
                    user.forward(methodReturn, context());
                }).match(Messages.ClientCallEnd.class, clientCallEnd -> {
                    final ActorRef user = getUser(clientCallEnd.context.connectionId);
                    user.forward(clientCallEnd, context());
                }).match(Messages.ClientFunctionCall.class, clientFunctionCall -> {
                    switch (clientFunctionCall.sendType) {
                        case All:
                            getContext().getChildren().forEach(user -> user.forward(clientFunctionCall, context()));
                            break;
                        case Caller:
                        case Group:
                        case InGroupExcept:
                            getContext().getChild(clientFunctionCall.context.connectionId.toString()).forward(clientFunctionCall, context());
                            break;
                        case Others:
                            final ActorRef caller = getContext().getChild(clientFunctionCall.context.connectionId.toString());
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
                                if (!allExcept.contains(user))
                                    user.forward(clientFunctionCall, context());
                            });
                            break;
                    }
                }).match(Messages.StateChange.class, state -> {
                    final ActorRef user = getUser(state.uuid);
                    user.forward(state, context());
                }).match(Messages.Error.class, error -> {
                    final ActorRef user = getUser(error.uuid);
                    user.forward(error, context());
                }).match(Messages.PollForMessages.class, poll -> {
                    final ActorRef user = getUser(poll.context.connectionId);
                    user.forward(poll, context());
                }).match(Messages.Abort.class, abort -> {
                    final ActorRef user = getUser(abort.context.connectionId);
                    user.forward(abort, context());
                }).match(TransportReconnectMessage.class, reconnect -> {
                    final ActorRef user = getUser(reconnect.getContext().connectionId);
                    user.forward(reconnect, context());
                }).build()
        );
    }

    private List<ActorRef> getUsers(UUID[] uuids) {
        return Arrays.asList(uuids).stream().map(uuid -> getContext().getChild(uuid.toString())).collect(Collectors.toList());
    }

    private ActorRef getUser(UUID uuid) {
        return Optional.ofNullable(getContext().getChild(uuid.toString())).orElseGet(() -> context().actorOf(Props.create(UserActor.class), uuid.toString()));
    }
}