package signalJ.services;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import play.Logger;
import signalJ.infrastructure.ProtectedData;
import signalJ.models.*;

public class SignalJActor extends AbstractActor {
    private final ActorRef usersActor;
	private final ActorRef hubsActor = context().actorOf(Props.create(HubsActor.class), "hubs");
    private final ActorRef groupsActor = context().actorOf(Props.create(GroupsActor.class), "groups");

    public SignalJActor() {
        this.usersActor = context().actorOf(Props.create(UsersActor.class), "users");
        receive(
                ReceiveBuilder.match(TransportJoinMessage.class, join -> {
                    usersActor.forward(join, context());
                    Logger.debug(join.getContext().connectionId + " logged on");
                }).match(Messages.Quit.class, quit -> {
                    usersActor.forward(quit, context());
                    groupsActor.forward(quit, context());
                }).match(Messages.RegisterHub.class, registerHub -> hubsActor.forward(registerHub, context())
                ).match(Messages.Execute.class, execute -> hubsActor.forward(execute, context())
                ).match(Messages.GroupJoin.class, groupJoin -> groupsActor.forward(groupJoin, context())
                ).match(Messages.GroupLeave.class, groupLeave -> groupsActor.forward(groupLeave, context())
                ).match(Messages.ClientFunctionCall.class, clientFunctionCall -> {
                    switch (clientFunctionCall.sendType) {
                        case All:
                        case Others:
                        case Caller:
                        case Clients:
                        case AllExcept:
                            usersActor.forward(clientFunctionCall, context());
                            break;
                        case Group:
                        case InGroupExcept:
                            groupsActor.forward(clientFunctionCall, context());
                            break;
                        default:
                            break;
                    }
                }).match(Messages.MethodReturn.class, methodReturn -> usersActor.forward(methodReturn, context())
                ).match(Messages.GetJavaScript.class, getJavaScript -> hubsActor.forward(getJavaScript, context())
                ).match(Messages.ClientCallEnd.class, clientCallEnd -> usersActor.forward(clientCallEnd, context())
                ).match(Messages.StateChange.class, state -> usersActor.forward(state, context())
                ).match(Messages.Error.class, error -> usersActor.forward(error, context())
                ).match(TransportMessage.class, msg -> hubsActor.forward(msg, context())
                ).match(Messages.PollForMessages.class, poll -> usersActor.forward(poll, context())
                ).match(ServerEventMessage.class, event -> hubsActor.forward(event, context())
                ).match(Messages.Abort.class, abort -> usersActor.forward(abort, context())
                ).match(TransportReconnectMessage.class, reconnect -> usersActor.forward(reconnect, context())
                ).build()
        );
    }
}