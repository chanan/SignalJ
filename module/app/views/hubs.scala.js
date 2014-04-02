function getSocket() {
	var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket;
	var socket = new WS("ws://localhost:9000/signalJ/Join"); //"at sign routes.PlaySockets.join().webSocketURL(request)"
	return socket;
}

var socket = getSocket();
var uuid;
var id = 0;
var callbacks = {};

var receiveEvent = function(event) {
    var data = JSON.parse(event.data);
    uuid = data.uuid;
    
    // Handle errors
    if(data.error) {
        chatSocket.close();
        console.log("Error: " + data.error);
        return;
    }
    if(data.type === "init") {
    	uuid = data.uuid;
    }
    if(data.type === "methodReturn") {
    	var f = callbacks[data.id];
    	if(f != undefined) f(data.returnValue);
    }
    if(data.type === "clientFunctionCall") {
    	executeFunctionByName(data.function, window, data.data);
    }
    console.log("Message from server: %O", data);
};

socket.onmessage = receiveEvent;

function hubs_describe(callback) {
	var j = {type: 'describe'};
	systemsend(j, callback);
} 

function systemsend(message, callback) {
	id = id + 1;
	if(callback != undefined) {
		callbacks[id] = callback;
	}
	message.uuid = uuid;
	message.id = '' + id;
	var str = JSON.stringify(message);
	console.log("Message to server: %O", message);
	socket.send(str);
}

function executeFunctionByName(functionName, context /*, args */) {
	var args = [].slice.call(arguments).splice(2);
	var namespaces = functionName.split(".");
	var func = namespaces.pop();
	for(var i = 0; i < namespaces.length; i++) {
		context = context[namespaces[i]];
	}
	return context[func].apply(this, args);
}
