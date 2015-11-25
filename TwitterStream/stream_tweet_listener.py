from tweepy.streaming import StreamListener
import json
import boto3

sqs = boto3.resource('sqs')

# Some user defined keywords for filtering
keywords = ['love','football','tech','paris','india', 'nba', 'basketball', 'thanks']
langs = ['en', 'fr', 'es', 'de', 'it', 'pt', 'ru', 'ar']

class StreamTweetListener(StreamListener):
    def on_data(self, tweet):
        json_tweet = json.loads(tweet)
        if 'coordinates' in json_tweet:
            coordinates =  json_tweet['coordinates']
            if coordinates:
                if json_tweet['lang'] in langs:
                    # Finding and Storing keywords
                    keys = [keyword for keyword in keywords if keyword in json_tweet['text'].lower()]
                    if keys:
                        json_tweet['keyword'] = keys
                        print json_tweet['text'], 'Keyword: ',json_tweet['keyword']
                        # Send to SQS Queue
                        self.send_to_queue(json_tweet)
        return True

    def on_error(self, status):
        print "Error: "+ status + "\n"
        return False

    def on_status(self, status):
        print "Status: ", status

    def send_to_queue(self, json_tweet):
        queue_name = 'tweet-queue'
        q = sqs.create_queue(QueueName = queue_name)
        response = q.send_message(MessageBody = json.dumps(json_tweet))
        print 'Message sent with ID: ', response.get('MessageId')
