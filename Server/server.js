var express = require('express');
var app = express();
var router = express.Router();

var twitter = require('twitter');
var twit = new twitter ({
  consumer_key: 'Akvjrwhus5ZWoOqBIZ3YwzBnj',
  consumer_secret: 'fmyAfDeeY97sBevcs8rVdxR5K4CyHWQ7IgFuYFh80BQZus3SVP',
  access_token_key: '4313326185-gR8yb25anRBjUfPkOLyrJURut2pbFuhKwi6ZjSw',
  access_token_secret: 'DifijsWlsxsvf3CiixfVgGUK7n6nxAvn2j0XGfEOIYJHx'
});

var aws = require('aws-sdk');
aws.config.update({
  accessKeyId: "AKIAIG4OW4BCIMCVM4EQ",
  secretAccessKey: "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",
  region: 'us-east-1'
});

var bodyParser = require('body-parser');
app.use(bodyParser.json());

var http = require('http').Server(app);
var io = require('socket.io')(http);
var iosocket = null;

app.use(express.static(__dirname + "/public"));

app.post('/newtweet', function (req, res) {
  var body = '';
  req.on('data', function(data) {
    body += data;
  });

  req.on('end', function() {
    message = JSON.parse(body);
    tweet = message['Message'];
    json_tweet = JSON.parse(tweet);
    iosocket.emit("mapdata", json_tweet);
  });

  res.status(200);
  res.send('notification received');
});

function loadFromDynamo() {
  var dynamoDB = new aws.DynamoDB();
  var params = { TableName: 'tweets' };

  dynamoDB.scan(params).eachPage(function(err, data) {
    if (err) {
      console.log(err);
    } else if (data) {
      console.log('Last Scan Processed ' + data.ScannedCount + ' items: ');
      for (var i = 0; i < data.Items.length; i++) {
        tweet = data.Items[i]['tweet']['S'];
        json_tweet = JSON.parse(tweet);
        iosocket.emit("mapdata", json_tweet);
      }
    } else {
      console.log('Finished Scan');
    }
  });
};

io.on('connection', function(socket) {
  iosocket = socket;
  console.log("connected");
  loadFromDynamo();
  socket.on('gettrends', function(message) {
    console.log(message.id);
    var params = {id: message.id};
    twit.get('trends/place', params, function(err, payload, response) {
      if (!err) {
        trend_topics = payload[0];
        trends = trend_topics.trends;
        socket.emit('foundtweets', trends);
      }
      else {
        console.log(err);
      }
    });
  });
});

var port = 5000;
http.listen(process.env.PORT || port, function() {
    console.log('listening on 5000');
});
