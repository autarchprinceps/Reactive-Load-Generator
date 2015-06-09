var http = require('http');
http.createServer(function (req, res) {
	console.log("DEBUG");
	res.end("Hallo");
}).listen(process.argv[2]);
