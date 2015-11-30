package gaurang.columbia.cloud.sqs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

public class MessageReceiver {
	
	private static AWSCredentials credentials;
	private static AmazonSQS sqs;
	private final static String queueName = "tweet-queue";
	
	public static AWSCredentials getAWSCredentials() {
		credentials = null;
		try {
			credentials = new ProfileCredentialsProvider("default").getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException("Cannot load the credentials from the credential profiles file. "
					+ "Please make sure that your credentials file is at the correct "
					+ "location (/Users/gaurang/.aws/credentials), and is in valid format.", e);
		}
		return credentials;
	}
	
	public static AmazonSQS initSQS() {
		sqs = new AmazonSQSClient(credentials);
		Region usEast1 = Region.getRegion(Regions.US_EAST_1);
		sqs.setRegion(usEast1);
		return sqs;
	}
	
	public static void processMessages (String queueName) throws JSONException, InterruptedException {
		String tweetQueueURL = sqs.getQueueUrl(queueName).getQueueUrl();

		// Receive tweets from Queue
		int maxNumberOfMessages = 10;

		while (true) {
			ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(tweetQueueURL)
					.withMaxNumberOfMessages(maxNumberOfMessages);
			List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
			Thread.sleep(2000);
			if (!messages.isEmpty()) {
				ExecutorService executor = Executors.newFixedThreadPool(maxNumberOfMessages);
				for (Message message : messages) {
					JSONObject tweet = new JSONObject(message.getBody());
					Runnable tweetWorker = new TweetWorker(tweet, credentials);
					executor.execute(tweetWorker);
				}
				try {
					System.out.println("Attempting to shut threads down");
					executor.shutdown();
					executor.awaitTermination(maxNumberOfMessages, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					System.out.println("Threads Interrupted");
				} finally {
					if (!executor.isTerminated())
						System.out.println("Finishing incomplete tasks");
					executor.shutdownNow();
					System.out.println("All threads forcefully shutdown");
				}
				deleteQueueMessagesBatch(tweetQueueURL, messages);
			}
			else {
				System.out.println("No messages, sleep the thread");
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
			}
		
		}
	}
	
	public static void deleteQueueMessagesBatch(String queueURL, List<Message> messages) {
		// Delete up to 10 messages that are processed
		List<DeleteMessageBatchRequestEntry> deleteEntries = new ArrayList<DeleteMessageBatchRequestEntry>();
		for (Message message : messages) {
			deleteEntries.add(
					new DeleteMessageBatchRequestEntry(message.getMessageId(), message.getReceiptHandle()));
		}
		DeleteMessageBatchRequest deleteRequest = new DeleteMessageBatchRequest(queueURL, deleteEntries);
		sqs.deleteMessageBatch(deleteRequest);
		System.out.println("Deleted Messages");
	}
	
	public static void main(String[] args) throws Exception {

		/*
		 * The ProfileCredentialsProvider will return your [default] credential
		 * profile by reading from the credentials file located at
		 * (/Users/gaurang/.aws/credentials).
		 */
		credentials = getAWSCredentials();
		sqs = initSQS();

		try {
			// process tweets from the queue
			processMessages(queueName);
			
		} catch (AmazonServiceException ase) {
			System.out.println("Caught an AmazonServiceException, which means your request made it "
					+ "to Amazon SQS, but was rejected with an error response for some reason.");
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
			System.out.println("Error Type:       " + ase.getErrorType());
			System.out.println("Request ID:       " + ase.getRequestId());
		} catch (AmazonClientException ace) {
			System.out.println("Caught an AmazonClientException, which means the client encountered "
					+ "a serious internal problem while trying to communicate with SQS, such as not "
					+ "being able to access the network.");
			System.out.println("Error Message: " + ace.getMessage());
		}
	}
}
