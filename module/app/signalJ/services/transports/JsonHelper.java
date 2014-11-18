package signalJ.services.transports;

import com.fasterxml.jackson.databind.JsonNode;
import play.Logger;
import play.libs.Json;
import signalJ.models.Messages;
import signalJ.models.RequestContext;
import signalJ.models.TransportMessage;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class JsonHelper {
    public static JsonNode writeConnect(String prefix) throws IOException {
        final StringBuilder sb = new StringBuilder();
        sb.append("{\"C\":\"");
        sb.append(prefix);
        sb.append("\",\"S\":1,\"M\":[]}");
        return Json.parse(sb.toString());
    }

    public static JsonNode writeError(Messages.Error error) throws IOException {
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format("{\"I\":\"%s\",\"E\":\"%s\"}", error.messageId, error.error));
        final JsonNode j = Json.parse(sb.toString());
        Logger.debug("Error: " + j);
        return j;
    }

    public static JsonNode writeState(Messages.StateChange state) throws IOException {
        final StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append(writeStateChanges(state.changes));
        sb.append(",\"I\":\"").append(state.messageId).append('"');
        sb.append('}');
        final JsonNode j = Json.parse(sb.toString());
        Logger.debug("State Change: " + j);
        return j;
    }

    private static String writeStateChanges(Map<String, String> changes) {
        final StringBuilder sb = new StringBuilder();
        sb.append("\"S\":{");
        boolean first = true;
        for(final Map.Entry<String, String> entry : changes.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append(String.format("\"%s\": \"%s\"", entry.getKey(), entry.getValue()));
        }
        sb.append('}');
        return sb.toString();
    }

    public static JsonNode writeHeartbeat() throws IOException {
        return Json.parse("{ }");
    }

    public static JsonNode writeMethodReturn(Messages.MethodReturn methodReturn) throws IOException {
        final StringBuilder sb = new StringBuilder();
        sb.append('{');
        methodReturn.changes.ifPresent(changes -> sb.append(writeStateChanges(changes)).append(','));
        sb.append("\"R\":");
        sb.append(Json.toJson(methodReturn.returnValue));
        sb.append(",\"I\":\"").append(methodReturn.context.messageId.get()).append("\"}");
        final JsonNode event = Json.parse(sb.toString());
        Logger.debug("Return Value: " + event);
        return event;
    }

    public static JsonNode writeClientFunctionCall(Messages.ClientFunctionCall clientFunctionCall, String prefix) throws IOException {
        final StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"C\":");

        sb.append('"').append(prefix).append('"');
        sb.append(",\"M\":[");
        sb.append(writeClientFunctionCall(clientFunctionCall));
        sb.append("]}");
        final JsonNode j = Json.parse(sb.toString());
        Logger.debug("ClientFunctionCall Value: " + j);
        return j;
    }

    private static String writeClientFunctionCall(Messages.ClientFunctionCall clientFunctionCall) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"H\":").append('"').append(clientFunctionCall.hubName).append('"').append(',');
        sb.append("\"M\":").append('"').append(clientFunctionCall.method.getName()).append('"').append(',');
        sb.append("\"A\":[");
        if(clientFunctionCall.args != null) {
            boolean first = true;
            for (final Object obj : clientFunctionCall.args) {
                if (!first) sb.append(',');
                first = false;
                sb.append(Json.toJson(obj));
            }
        }
        sb.append("]}");
        return sb.toString();
    }

    public static JsonNode writeList(List<TransportMessage> list, String prefix) throws IOException {
        final StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"C\":");
        sb.append('"').append(prefix).append('"');
        sb.append(",\"M\":[");
        boolean firstCall = true;
        for(TransportMessage m : list) {
            if (!firstCall) sb.append(',');
            firstCall = false;

            if(m instanceof Messages.ClientFunctionCall) {
                sb.append(writeClientFunctionCall((Messages.ClientFunctionCall)m));
            }

            if(m instanceof Messages.StateChange) {
                sb.append(writeState((Messages.StateChange)m));
            }

            if(m instanceof Messages.Error) {
                sb.append(writeError((Messages.Error)m));
            }

            if(m instanceof Messages.MethodReturn) {
                sb.append(writeMethodReturn((Messages.MethodReturn)m));
            }
        }
        sb.append("]}");
        final JsonNode j = Json.parse(sb.toString());
        Logger.debug("List ClientFunctionCall Value: " + j);
        return j;
    }

    public static JsonNode writeConfirm(RequestContext context) throws IOException {
        final StringBuilder sb = new StringBuilder();
        sb.append("{\"I\":\"").append(context.messageId.get()).append("\"}");
        return Json.parse(sb.toString());

    }
}