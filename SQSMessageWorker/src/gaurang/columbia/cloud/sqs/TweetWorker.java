package gaurang.columbia.cloud.sqs;

import com.alchemyapi.api.AlchemyAPI;

import java.io.IOException;
import java.io.StringWriter;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

public class TweetWorker implements Runnable {
	private JSONObject tweet;
	
	public TweetWorker(JSONObject tweet) {
		this.tweet = tweet;
	}
	
	private void getSentiment() {
		// make a URL and hit up alchemy
		try {
			String apiKey_gmail = "3c592701a603462b55abad073040d969cb9bea5c";
			String apiKey_lion = "ebe8dd8a9bb6217deaf297ff93f7a9afca312132";
			AlchemyAPI alchemy = AlchemyAPI.GetInstanceFromString(apiKey_lion);
			Document doc = alchemy.TextGetTextSentiment(tweet.getString("text"));
			System.out.println(getStringFromDocument(doc));
		} catch (JSONException | XPathExpressionException | IOException | SAXException | ParserConfigurationException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		getSentiment();
	}
	
	private String getStringFromDocument(Document doc) {
        try {
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);

            return writer.toString();
        } catch (TransformerException ex) {
            ex.printStackTrace();
            return null;
        }
    }

}