package gaurang.columbia.cloud.sqs;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.alchemyapi.api.AlchemyAPI;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.util.Tables;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

public class TweetWorker implements Runnable {
	private JSONObject tweet;
	private AmazonDynamoDBClient dynamoDB;
	private AmazonSNSClient snsClient;
	
	private final String apiKey_gmail = "3c592701a603462b55abad073040d969cb9bea5c";
	private final String apiKey_lion = "ebe8dd8a9bb6217deaf297ff93f7a9afca312132";
	private final String dynamoTableName = "tweets";
	
	private final String snsArn = "arn:aws:sns:us-east-1:455518163747:twitter-feed";
	
	private DateFormat format;
	
	public TweetWorker(JSONObject tweet, AWSCredentials credentials) {
		this.tweet = tweet;
		initDynamoDB(credentials);
		initSNS(credentials);
	}
	
	private void initSNS(AWSCredentials credentials) {
		snsClient = new AmazonSNSClient(credentials);
		Region usEast1 = Region.getRegion(Regions.US_EAST_1);
		snsClient.setRegion(usEast1);
	}

	private void initDynamoDB(AWSCredentials credentials) {
		dynamoDB = new AmazonDynamoDBClient(credentials);
        Region usEast1 = Region.getRegion(Regions.US_EAST_1);
        dynamoDB.setRegion(usEast1);
	}
	
	private boolean getSentiment() {
		// hit up alchemy
		boolean alchemySuccess = true;
		try {
			AlchemyAPI alchemy = AlchemyAPI.GetInstanceFromString(apiKey_gmail);
			Document doc = alchemy.TextGetTextSentiment(tweet.getString("text"));
			if (doc != null) {
				String sentimentType = doc.getElementsByTagName("type").item(0).getTextContent();
				String sentimentScore = doc.getElementsByTagName("score").item(0).getTextContent();
				tweet.putOnce("sentimentType", sentimentType);
				tweet.putOnce("sentimentScore", sentimentScore);
				System.out.println(tweet.get("sentimentType"));
			}
			else {
				alchemySuccess = false;
			}
		} catch (JSONException | XPathExpressionException | SAXException | ParserConfigurationException e) {
			e.printStackTrace();
			alchemySuccess = false;
		} catch (IOException e) {
			System.out.println("unsupported language");
			alchemySuccess = false;
		} catch (NullPointerException e) {
			System.out.println("Alchemy didn't return sentiment");
			alchemySuccess = false;
		}
		return alchemySuccess;
	}

	@SuppressWarnings("deprecation")
	private void insertIntoDB() {
		if (Tables.doesTableExist(dynamoDB, dynamoTableName)) {
			try {
				String timestamp = tweet.getString("timestamp_ms");
				Map<String, AttributeValue> item = newItem(timestamp, tweet.toString());
				String tweetID = tweet.getString("id");
				String tweetString = tweet.toString();
				String keyword = tweet.getString("keyword");
				//Map<String, AttributeValue> item = newItem(tweetID, tweetString, timestamp, keyword);
				
				PutItemRequest putItemRequest = new PutItemRequest(dynamoTableName, item);
	            dynamoDB.putItem(putItemRequest);
	            System.out.println("Tweet added to DB");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		else {
			System.out.println("Create a new table "+ dynamoTableName +" from the Dynamo Console");
		}
	}
	
	private Map<String, AttributeValue> newItem(String timestamp, String tweet) {
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put("id", new AttributeValue().withN(timestamp));
        item.put("tweet", new AttributeValue(tweet));

        return item;
    }
	
	private void sendSNSToServer(JSONObject tweet) {
		String[] requiredFields = {"timestamp_ms", "text", "sentimentScore", "sentimentType", "coordinates"};
		JSONObject filteredTweet = new JSONObject(tweet, requiredFields);
		System.out.println(filteredTweet.toString());
		PublishRequest publishRequest = new PublishRequest(snsArn, filteredTweet.toString());
		PublishResult publishResult = snsClient.publish(publishRequest);
		 //print MessageId of message published to SNS topic
		System.out.println("MessageId - " + publishResult.getMessageId());
	}
	
	@Override
	public void run() {
		//checkTweets();
		boolean alchemySuccess = getSentiment();
		if (alchemySuccess) {
			System.out.println("Sending to Server");
			sendSNSToServer(tweet);
			// load into DB 
			System.out.println("Inserting Into DB");
			//insertIntoDB();
		}
	}

	private void checkTweets() {
		// TODO Auto-generated method stub
		try {
			String id = tweet.getString("id");
			System.out.println(id);
			String timestamp = tweet.getString("timestamp_ms");
			System.out.println(timestamp);
			String keyword = tweet.getString("keyword");
			System.out.println(keyword);
			String coordinates = tweet.getString("coordinates");
			System.out.println(coordinates);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
