var express = require("express");
var app = express();
var server = require("http").createServer(app);
var io = require("socket.io")(5000);

io.attach(server,{
  pingInterval: 10000,
  pingTimeout: 5000,
  cookie: false
});

console.log("Server Running");

io.sockets.on('connection', function (socket) {
	
    console.log("co thiet bi ket noi");    
    socket.on('getLocation', function (data) {
    console.log("Location: "+data);
      socket.emit('server-send-data', { "serversend": data });
    });
    
  });
  