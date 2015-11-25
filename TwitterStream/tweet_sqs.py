from tweepy import API
from stream_tweet_listener import StreamTweetListener, keywords
from tweepy import OAuthHandler, Stream
import json

consumer_key = 'CLbQDiBRjNn1NGEE0wJ2yNtxb'
consumer_secret = 'sJRdATSUzFWYJ1F2Ko28iLcWAQpYOsRjEX3EE0OoWW4XHoNGIl'

access_token = '140911269-fETxwkzza6f4n0MX1j6Saf7btgV7Vd1OBWdJKjVr'
access_token_secret = 'yOkNGkoOvtqtNre0L0rsNyYhhlIltkRWHz5sWVLFsmWFj'

auth = OAuthHandler(consumer_key, consumer_secret)
auth.set_access_token(access_token, access_token_secret)
api = API(auth)
print "Got Twitter API object"

def stream_to_queue():
    l = StreamTweetListener()
    stream = Stream(auth, l)
    print "Starting streaming..."
    stream.filter(track = keywords)

if __name__== '__main__':
    stream_to_queue()
