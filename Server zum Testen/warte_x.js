var http = require('http');
http.createServer(function (req, res) {
	setTimeout(function () {
		console.log("DEBUG");
		res.end("Hallo");
	}, process.argv[3]);
}).listen(process.argv[2]);
