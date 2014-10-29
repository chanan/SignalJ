package signalJ.services;

import akka.actor.ActorRef;
import play.Logger;
import signalJ.services.HubActor.ClientFunctionCall.SendType;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

class SenderProxy implements InvocationHandler {
	private final SendType sendType;
	private final Class<?> clazz;
    private final String hubName;
	private final UUID caller;
	private final UUID[] clients;
	private final UUID[] allExcept;
	private final String groupName;
    private final ActorRef signalJActor;

	public SenderProxy(ActorRef signalJActor, SendType sendType, Class<?> clazz, String hubName, UUID caller) {
		this.sendType = sendType;
		this.clazz = clazz;
        this.hubName = hubName;
		this.caller = caller;
		this.clients = null;
		this.allExcept = null;
		this.groupName = null;
        this.signalJActor = signalJActor;
	}
	
	public SenderProxy(ActorRef signalJActor, SendType sendType, Class<?> clazz, String hubName, UUID caller, UUID[] clients, UUID[] allExcept, String groupName) {
		this.sendType = sendType;
		this.clazz = clazz;
        this.hubName = hubName;
		this.caller = caller;
		this.clients = clients;
		this.allExcept = allExcept;
		this.groupName = groupName;
        this.signalJActor = signalJActor;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Logger.debug(sendType + ": " + hubName + " - " + method.getName() + " " + argsToString(args));
        signalJActor.tell(new HubActor.ClientFunctionCall(method, hubName, caller, sendType, method.getName(), args, clients, allExcept, groupName), ActorRef.noSender());
		return null;
	}

    private String argsToString(Object[] args) {
        if(args == null) return "";
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        int i = 0;
        for (final Object o : args) {
            sb.append(o.toString());
            i++;
            if(i != args.length) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    public Object createProxy() throws IllegalArgumentException {
        return Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, this);
    }
}