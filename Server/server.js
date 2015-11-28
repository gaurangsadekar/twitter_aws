var express = require('express');
var bodyParser = require('body-parser');

var app = express();

var http = require('http').Server(app);
var io = require('socket.io')(http);

var config = {
    PORT: 3000
}

var aws = require('aws-sdk');
aws.config.update({
    accessKeyId: "AKIAIG4OW4BCIMCVM4EQ",
    secretAccessKey: "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"
    region: 'us-east-1'
});


// setup static content
app.use(express.static(__dirname + "/public"));

// parse application/json
app.use(bodyParser.json());


app.post('/newtweet', function (req, res) {
  console.log('New tweet from SNS. Yay!')
  // parse the request, get the tweet and emit it to the client, then send a thank you to SNS
  body = '';
  req.on('data', function(data) {
    body += data;
  });

  req.on('end', function() {
    message = JSON.parse(body);
    tweet = message['Message'];
    json_tweet = JSON.parse(tweet);
    console.log(json_tweet.coordinates.coordinates);
    console.log(json_tweet['text']);
  });

  // socket goes here
  res.status(200);
  res.send('thanks');
});



function loadFromDynamo() {
  var dynamoDB = new aws.DynamoDB();
  var params = { TableName: 'tweets' };

  dynamoDB.scan(params).eachPage(function(err, data) {
    if (err) {
      console.log(err);
    } else if (data) {
      console.log('Last scan processed ' + data.ScannedCount + ' items: ');
      for (var i = 0; i < data.Items.length; i++) {
        tweet = data.Items[i]['tweet']['S'];
        tweet = JSON.parse(tweet);
        console.log(tweet['text']);
        console.log(tweet.coordinates.coordinates);
      }
    } else {
      console.log('*** Finished Scan ***');
    }
  });
}

io.on('connection', function(socket) {
    console.log('new user connected');
    socket.emit("tweets:connected", {msg: "hello world from server"});
    loadFromDynamo(socket);
    //startListeningOnSNS(socket);
});

// start listening
http.listen(5000, function() {
    console.log('listening on 5000');
});
