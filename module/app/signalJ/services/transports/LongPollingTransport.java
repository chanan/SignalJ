package signalJ.services.transports;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ReceiveTimeout;
import akka.japi.pf.ReceiveBuilder;
import play.Logger;
import play.mvc.Results;
import scala.concurrent.duration.Duration;
import signalJ.SignalJPlugin;
import signalJ.infrastructure.Cursor;
import signalJ.infrastructure.ProtectedData;
import signalJ.models.Messages;
import signalJ.models.TransportMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class LongPollingTransport extends AbstractActor {
    private final UUID uuid;
    private Optional<Results.Chunks.Out<String>> out = Optional.empty();
    private final String prefix = Cursor.GetCursorPrefix();
    private final ActorRef signalJActor = SignalJPlugin.getSignalJActor();
    private final ProtectedData protectedData;
    private List<TransportMessage> messages = new ArrayList<>();

    private int count = 0;
    private final int maxCount = 30;

    public LongPollingTransport(ProtectedData protectedData, Messages.JoinLongPolling join) {
        this.protectedData = protectedData;
        this.uuid = join.context.connectionId;

        context().setReceiveTimeout(Duration.create("1 second"));

        receive(
                ReceiveBuilder.match(Messages.JoinLongPolling.class, r -> {
                    r.out.write(JsonHelper.writeConnect(prefix).toString());
                    r.out.close();
                }).match(Messages.PollForMessages.class, poll -> {
                    out.ifPresent(o -> send());
                    out = Optional.of(poll.out);
                }).match(Messages.MethodReturn.class, methodReturn -> {
                    methodReturn.out.ifPresent(o -> writeOneMessage(methodReturn, o));
                    if (!methodReturn.out.isPresent()) messages.add(methodReturn);
                }).match(Messages.ClientFunctionCall.class, clientFunctionCall -> {
                    messages.add(clientFunctionCall);
                }).match(Messages.ClientCallEnd.class, clientCallEnd -> {
                    clientCallEnd.out.ifPresent(o -> writeOneMessage(clientCallEnd, o));
                    if (!clientCallEnd.out.isPresent()) if (!out.isPresent()) messages.add(clientCallEnd);
                }).match(Messages.ReconnectLongPolling.class, r -> Logger.debug("Reconnect Longpolling " + r.context.connectionId)
                ).match(Messages.StateChange.class, state -> {
                    state.out.ifPresent(o -> writeOneMessage(state, o));
                    if (!state.out.isPresent()) messages.add(state);
                }).match(Messages.Error.class, error -> {
                    error.out.ifPresent(o -> writeOneMessage(error, o));
                    if (!error.out.isPresent()) messages.add(error);
                }).match(ReceiveTimeout.class, r -> {
                    out.ifPresent(o -> {
                        count++;
                        if(messages.isEmpty()) {
                            o.write(" ");
                            if(count == maxCount) {
                                send();
                            }
                        } else {
                            send();
                        }
                    });
                }).matchAny(x -> Logger.debug(x.toString())).build()
        );
    }

    private void send() {
        try {
            if(out.isPresent()) {
                out.get().write(JsonHelper.writeList(messages, prefix).toString());
                messages.stream().forEach(this::sendAck);
                messages.clear();
                count = 0;
                out.get().close();
                out = Optional.empty();
            }
        } catch (Exception e) {
            Logger.error("Send Error", e);
        }
    }

    private void writeOneMessage(TransportMessage m, Results.Chunks.Out<String> o) {
        try {
            if(!out.isPresent()) return;
            if (m instanceof Messages.ClientFunctionCall) {
                out.get().write(JsonHelper.writeClientFunctionCall((Messages.ClientFunctionCall) m, prefix).toString());
                sendAck(m);
            }
            if (m instanceof Messages.MethodReturn) {
                out.get().write(JsonHelper.writeMethodReturn((Messages.MethodReturn) m).toString());
                sendAck(m);
            }
            if (m instanceof Messages.ClientCallEnd) {
                out.get().write(JsonHelper.writeConfirm(((Messages.ClientCallEnd) m).context).toString());
                sendAck(m);
            }
            if (m instanceof Messages.StateChange) {
                out.get().write(JsonHelper.writeState((Messages.StateChange) m).toString());
                sendAck(m);
            }
            if (m instanceof Messages.Error) {
                out.get().write(JsonHelper.writeError((Messages.Error) m).toString());
                sendAck(m);
            }
            o.close();
        } catch (IOException e) {
            Logger.error("Error in writing to long poll transport", e);
        }
    }

    private void sendAck(TransportMessage transportMessage) {
        context().parent().tell(new Messages.Ack(transportMessage), self());
    }

    @Override
    public void postStop() throws Exception {
        Logger.debug("LongPollingTransport: " + uuid);
    }
}
