import boto3

# Get sqs resource from boto3
sqs = boto3.resource('sqs')
# Default region is US-East-1 (N. Virginia)

queue_name = 'tweet-queue'
try:
    q = sqs.get_queue_by_name(QueueName = queue_name)
    print 'Queue already exists'
except Exception as e:
    q = sqs.create_queue(QueueName = queue_name)
    print 'Created new queue'

response = q.send_message(MessageBody = 'Hello World')
print response.get('MessageId'), response.get('MessageBody')

for i in range(10):
    for m in q.receive_messages(i):
        if m.body:
            print i, m.message_id, m.receipt_handle
