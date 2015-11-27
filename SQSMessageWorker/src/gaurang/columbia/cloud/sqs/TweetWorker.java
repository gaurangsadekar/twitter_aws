package gaurang.columbia.cloud.sqs;

import com.alchemyapi.api.AlchemyAPI;

import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.util.Tables;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

public class TweetWorker implements Runnable {
	private JSONObject tweet;
	private AmazonDynamoDBClient dynamoDB;
	
	private final String apiKey_gmail = "3c592701a603462b55abad073040d969cb9bea5c";
	private final String apiKey_lion = "ebe8dd8a9bb6217deaf297ff93f7a9afca312132";
	private final String dynamoTableName = "tweets";
	private final String TWITTER_FORMAT = "EEE MMM dd HH:mm:ss Z yyyy";
	
	private DateFormat format;
	
	public TweetWorker(JSONObject tweet, AWSCredentials credentials) {
		this.tweet = tweet;
		this.dynamoDB = initDynamoDB(credentials);
		format = new SimpleDateFormat(TWITTER_FORMAT);
		format.setLenient(true);
	}
	
	private AmazonDynamoDBClient initDynamoDB(AWSCredentials credentials) {
		dynamoDB = new AmazonDynamoDBClient(credentials);
        Region usEast1 = Region.getRegion(Regions.US_EAST_1);
        dynamoDB.setRegion(usEast1);
        return dynamoDB;
	}
	
	private boolean getSentiment() {
		// hit up alchemy
		boolean alchemySuccess = true;
		try {
			AlchemyAPI alchemy = AlchemyAPI.GetInstanceFromString(apiKey_lion);
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
		} catch (IOException e) {
			System.out.println("unsupported language");
			alchemySuccess = false;
		}
		return alchemySuccess;
	}

	@SuppressWarnings("deprecation")
	private void insertIntoDB() {
		if (Tables.doesTableExist(dynamoDB, dynamoTableName)) {
			try {
				String createdAt = tweet.getString("created_at");
				String timestamp = String.valueOf(format.parse(createdAt).getTime());
				Map<String, AttributeValue> item = newItem(timestamp, tweet.toString());
				PutItemRequest putItemRequest = new PutItemRequest(dynamoTableName, item);
	            dynamoDB.putItem(putItemRequest);
	            System.out.println("Tweet added to DB");
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (ParseException e)  {
				System.out.println("Problem with parsing created at date");
			}
		}
		else {
			System.out.println("Create a new table "+ dynamoTableName +" from the Dynamo Console");
		}
	}
	
	private Map<String, AttributeValue> newItem(String timestamp, String tweet) {
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put("timestamp", new AttributeValue().withN(timestamp));
        item.put("tweet", new AttributeValue(tweet));

        return item;
    }
	
	private void sendSNSToServer() {
		// Take the tweet, make it a string and send it to the server.
	}
	
	@Override
	public void run() {
		boolean alchemySuccess = getSentiment();
		if (alchemySuccess) {
			System.out.println("Sending to Server");
			sendSNSToServer();
			// load into DB 
			System.out.println("Inserting Into DB");
			insertIntoDB();
		}
	}
}
