package signalJ.services;

import akka.actor.*;
import akka.japi.pf.ReceiveBuilder;
import scala.concurrent.duration.Duration;
import signalJ.infrastructure.ProtectedData;
import signalJ.models.RequestContext;
import signalJ.services.HubActor.ClientFunctionCall;
import signalJ.services.SignalJActor.Join;

class UserActor extends AbstractActor {
    private final ProtectedData protectedData;
    private ActorRef transport;

    UserActor(ProtectedData protectedData) {
        this.protectedData = protectedData;
        receive(
                ReceiveBuilder.match(Join.class, join -> {
                    transport = context().actorOf(Props.create(WebsocketTransport.class, protectedData, join));
                    transport.tell(join, self());
                }).match(MethodReturn.class, methodReturn -> transport.tell(methodReturn, self())
                ).match(ClientFunctionCall.class, clientFunctionCall -> transport.tell(clientFunctionCall, self())
                ).match(ClientCallEnd.class, clientCallEnd -> transport.tell(clientCallEnd, self())
                ).match(SignalJActor.Reconnect.class, reconnect -> {
                    attemptStopTransport();
                    final Join join = new Join(reconnect.out, reconnect.in, reconnect.uuid);
                    transport = context().actorOf(Props.create(WebsocketTransport.class, protectedData, join));
                    transport.tell(reconnect, self());
                }).match(SignalJActor.Quit.class, quit -> {
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

    public static class MethodReturn {
		final RequestContext context;
		final Object returnValue;
		
		public MethodReturn(RequestContext context, Object returnValue) {
            this.context = context;
			this.returnValue = returnValue;
		}
	}

    public static class ClientCallEnd {
        public final RequestContext context;

        public ClientCallEnd(RequestContext context) {
            this.context = context;
        }
    }
}