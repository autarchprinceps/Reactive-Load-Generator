var http = require('http');
http.createServer(function (req, res) {
	res.end("Hallo");
}).listen(process.argv[2]);
