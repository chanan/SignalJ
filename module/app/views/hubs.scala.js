function getSocket() {
	var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket;
	var socket = new WS("ws://localhost:9000/signalJ/Join"); //"at sign routes.PlaySockets.join().webSocketURL(request)"
	return socket;
}

var socket = getSocket();
var uuid;

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
    console.log("Message: " + data.message);
};

function systemsend(message) {
	message.uuid = uuid;
	var str = JSON.stringify(message);
	console.log(str);
	socket.send(str);
}