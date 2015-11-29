# twitter_aws

Code for Assignment 2 of Cloud and Big Data.

#### TwitterStream - Python
* Uses Twitter Stream API to get Tweets.
* Pushes tweets into an SQS Queue (The 'producer' for SQS)

#### SQSMessageWorker - Java
* Reads Tweets from the SQS Queue (The 'consumer' for SQS)
* Uses a Thread Pool & Executor to get the sentiment of each tweet using the Alchemy API.
* Adds the enriched data into DynamoDB
* Notifies the Server of Real-Time Tweets using SNS.

#### Web Server - Node.js
A Web Server to read the tweets from DynamoDB, emit new tweets as they are received from SNS and service client requests via Sockets

Developed by:

* Gaurang Sadekar - UNI: **gss2147**
* Viral Shah - UNI: **vrs2119**
