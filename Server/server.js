var express = require('express');
var aws = require('aws-sdk');
var bodyParser = require('body-parser');

var app = express();
var http = require('http').Server(app);
var io = require('socket.io')(http);

// setup static content
app.use(express.static(__dirname + "/public"));

// parse application/json
app.use(bodyParser.json());

// get express router object
var router = express.Router();
var iosocket = null;
aws.config.update({
    accessKeyId: "AKIAIG4OW4BCIMCVM4EQ",
    secretAccessKey: "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",
    region: 'us-east-1'
});

app.get('/', function(req, res) {
  console.log('Hit from client')
  res.send("Hello From Server")
});

app.post('/newtweet', function (req, res) {
  console.log('New tweet from SNS')
  // parse the request, get the tweet and emit it to the client, then send a thank you to SNS
  var body = '';
  req.on('data', function(data) {
    body += data;
  });

  req.on('end', function() {
    message = JSON.parse(body);
    tweet = message['Message'];
    json_tweet = JSON.parse(tweet);
    socket.emit("mapdata", json_tweet);
  });

  res.status(200);
  res.send('notification received');
});

function loadFromDynamo(socket) {
  var dynamoDB = new aws.DynamoDB();
  var params = { TableName: 'tweets' };

  dynamoDB.scan(params).eachPage(function(err, data) {
    if (err) {
      console.log(err);
    } else if (data) {
      console.log('Last scan processed ' + data.ScannedCount + ' items: ');
      for (var i = 0; i < data.Items.length; i++) {
        tweet = data.Items[i]['tweet']['S'];
        json_tweet = JSON.parse(tweet);
        socket.emit("mapdata", json_tweet);
      }
    } else {
      console.log('*** Finished Scan ***');
    }
  });
}

io.on('connection', function(socket) {
  iosocket = socket;
  console.log("connected");
  loadFromDynamo(socket);
});

var port = 5000;
// start listening
http.listen(process.env.PORT || port, function() {
    console.log('listening on 5000');
});
