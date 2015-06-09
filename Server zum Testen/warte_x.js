var http = require('http');
http.createServer(function (req, res) {
	setTimeout(function () {
		res.end("Hallo");
	}, process.argv[3]);
}).listen(process.argv[2]);
