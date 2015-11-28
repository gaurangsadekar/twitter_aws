var express = require('express');
var bodyParser = require('body-parser');
var app = express();

var http = require('http').Server(app);
var io = require('socket.io')(http);

// setup static content
app.use(express.static(__dirname + "/public"));
app.use(bodyParser.urlencoded({ extended: true }));

// parse application/json
app.use(bodyParser.json());

// get express router object
var router = express.Router();

app.get('/', function(req, res) {
  console.log('Hit from client')
  res.send("Hello From Server")
});

app.post('/newtweet', function (req, res) {
  console.log('New tweet from SNS. Yay!')
  // parse the requesst, get the tweet and emit it to the client, then send a thank you to SNS
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

// start listening
var port = 5000;
// start listening
http.listen(process.env.PORT || port, function() {
    console.log('listening on 5000');
});
