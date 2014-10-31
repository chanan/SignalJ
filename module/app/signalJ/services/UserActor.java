package signalJ.services;

import akka.actor.*;
import akka.japi.pf.ReceiveBuilder;
import scala.concurrent.duration.Duration;
import signalJ.infrastructure.ProtectedData;
import signalJ.models.Messages;

class UserActor extends AbstractActor {
    private final ProtectedData protectedData;
    private ActorRef transport;

    UserActor(ProtectedData protectedData) {
        this.protectedData = protectedData;
        receive(
                ReceiveBuilder.match(Messages.Join.class, join -> {
                    transport = context().actorOf(Props.create(WebsocketTransport.class, protectedData, join));
                    transport.tell(join, self());
                }).match(Messages.MethodReturn.class, methodReturn -> transport.tell(methodReturn, self())
                ).match(Messages.ClientFunctionCall.class, clientFunctionCall -> transport.tell(clientFunctionCall, self())
                ).match(Messages.ClientCallEnd.class, clientCallEnd -> transport.tell(clientCallEnd, self())
                ).match(Messages.Reconnect.class, reconnect -> {
                    attemptStopTransport();
                    final Messages.Join join = new Messages.Join(reconnect.out, reconnect.in, reconnect.uuid);
                    transport = context().actorOf(Props.create(WebsocketTransport.class, protectedData, join));
                    transport.tell(reconnect, self());
                }).match(Messages.Quit.class, quit -> {
                    //Wait a minute before shutting down allowing clients to reconnect
                    context().setReceiveTimeout(Duration.create("1 minute"));
                }).match(ReceiveTimeout.class, r-> {
                    context().stop(self());
                }).build()
        );
    }

    private void attemptStopTransport() {
        try {
            transport.tell(PoisonPill.getInstance(), self());
        } catch (Exception e) { }
    }
}