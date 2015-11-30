# Twitter Sentiment Analysis and Trends

###### Code for Assignment 2 of Cloud Computing and Big Data.

#### TwitterStream - Python
* Uses Twitter Stream API to get Tweets.
* Pushes tweets into an SQS Queue (The 'producer' for SQS)

#### SQSMessageWorker - Java
* Reads Tweets from the SQS Queue (The 'consumer' for SQS)
* Uses a Thread Pool & Executor to get the sentiment of each tweet using the Alchemy API.
* Adds the enriched data into DynamoDB
* Notifies the Server of Real-Time Tweets using SNS.

#### Application Server - Node.js
* The Server queries Dynamo for stored tweets and emits them to the client.
* It handles new tweets from the queue sent to it by SNS.
* It also handles making a REST call to the Twitter Trends API to show trending topics in a particular location, as selected by the client.
* Deployed on AWS Elastic Beanstalk

#### Front End
* Developed in JavaScript and HTML, uses the Google Maps API to render the heat map and plot the tweets by their location.


Developed by:

* Gaurang Sadekar - UNI: **gss2147**
* Viral Shah - UNI: **vrs2119**
