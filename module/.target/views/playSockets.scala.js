function getSocket() {
	var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket;
	var socket = new WS("ws://localhost:9000/signalJ/Join"); //"at sign routes.PlaySockets.join().webSocketURL(request)"
	return socket;
}

var socket = getSocket();
var uuid;
var once = false;

var receiveEvent = function(event) {
    var data = JSON.parse(event.data);
    uuid = data.uuid;
    
    // Handle errors
    if(data.error) {
        chatSocket.close();
        console.log("Error: " + data.error);
        return;
    }
    console.log("Message: " + data.message);
    if(!once) {
    	joinChannel('test');
    	send('Hello to all');
    	//send('Hello to test channel!', 'test');
    	once = true;
    }
};

function joinChannel(channelName) {
	var str = JSON.stringify({ type: 'ChannelJoin', uuid: uuid, channel: channelName });
	console.log(str);
	socket.send(str);
}

function systemsend(message) {
	message.uuid = uuid;
	var str = JSON.stringify(message);
	console.log(str);
	socket.send(str);
}

function send(message, channel) {
	var payload = { uuid: uuid, message: message };
	if(channel != undefined ){
		payload.type = 'SendToChannel';
		payload.channel = channel;
	} else {
		payload.type = 'SendToAll';
	}
	var str = JSON.stringify(payload);
	console.log(str);
	socket.send(str);
}

socket.onmessage = receiveEvent;

socket.onopen = function() {
	
};