package signalJ.services;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

import play.Logger;
import signalJ.services.ChannelActor.ClientFunctionCall.SendType;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;

class SenderProxy implements InvocationHandler {
	private final SendType sendType;
	private final Class<?> clazz;
	private final ActorSelection channelActor;
	private final UUID caller;
	private final UUID[] clients;
	private final UUID[] allExcept;
	private final String groupName;

	public SenderProxy(SendType sendType, Class<?> clazz, ActorSelection channelActor, UUID caller) {
		this.sendType = sendType;
		this.clazz = clazz;
		this.channelActor = channelActor;
		this.caller = caller;
		this.clients = null;
		this.allExcept = null;
		this.groupName = null;
	}
	
	public SenderProxy(SendType sendType, Class<?> clazz, ActorSelection channelActor, UUID caller, UUID[] clients, UUID[] allExcept, String groupName) {
		this.sendType = sendType;
		this.clazz = clazz;
		this.channelActor = channelActor;
		this.caller = caller;
		this.clients = clients;
		this.allExcept = allExcept;
		this.groupName = groupName;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Logger.debug(sendType + " - " + method.getName() + " " + args);
		channelActor.tell(new ChannelActor.ClientFunctionCall(method, clazz.getName(), caller, sendType, method.getName(), args, clients, allExcept, groupName), ActorRef.noSender());
		return null;
	}
	
	public Object createProxy() throws IllegalArgumentException {
        return Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, this);
    }
}