package hubs;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

import play.Logger;
import services.ChannelActor;
import services.ChannelActor.ClientFunctionCall.SendType;
import akka.actor.ActorRef;

class SenderProxy implements InvocationHandler {
	private final SendType sendType;
	private final Class<?> clazz;
	private final ActorRef channelActor;
	private final UUID caller;

	public SenderProxy(SendType sendType, Class<?> clazz, ActorRef channelActor, UUID caller) {
		this.sendType = sendType;
		this.clazz = clazz;
		this.channelActor = channelActor;
		this.caller = caller;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Logger.debug(sendType + " - " + method.getName() + " " + args);		
		channelActor.tell(new ChannelActor.ClientFunctionCall(method, clazz.getName(), caller, sendType, method.getName(), args), channelActor);
		return null;
	}
	
	public Object createProxy() throws IllegalArgumentException {
        return Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, this);
    }
}