function getSocket() {
	var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket;
    var url;
    if(window.location.protocol === "https:") url = "wss://";
    else url = "ws://"
    url = url + window.location.host;
    //if(window.location.port != 80) url = url + ":" + window.location.port;
    url = url + "/signalJ/Join";
    var socket = new WS(url);
	return socket;
}

var socket = getSocket();
var uuid;
var id = 0;
var callbacks = {};

var receiveEvent = function(event) {
    var data = JSON.parse(event.data);
    if (uuid == null) uuid = data.uuid;
    
    // Handle errors
    if(data.error) {
        chatSocket.close();
        console.log("Error: " + data.error);
        return;
    }
    if(data.type === "init") {
    	uuid = data.uuid;
    	oninit(uuid);  //TODO: This should be done with promises. currently this causes an error when oninit is not defined
    }
    if(data.type === "methodReturn") {
    	var f = callbacks[data.id];
    	if(f != undefined) f(data.returnValue);
    }
    if(data.type === "clientFunctionCall") {
    	executeFunctionByName(data.function, window, data.args);
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
	var vals = new Array();
	var k = 0;
	var args = [].slice.call(arguments).splice(2);
	for(var i = 0; i < args.length; i++) {
		for(var j = 0; j < args[i].length; j++) {
			vals[k] = args[i][j].value;
			k++;
		}
	}
	var namespaces = functionName.split(".");
	var func = namespaces.pop();
	for(var i = 0; i < namespaces.length; i++) {
		context = context[namespaces[i]];
	}
	return context[func].apply(this, vals);
}

function groupAdd(group) {
	var j = {type: 'groupAdd', group: group};
	systemsend(j);
}

function groupRemove(group) {
	var j = {type: 'groupRemove', group: group};
	systemsend(j);
}