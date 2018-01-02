/*
 * 1. connect to mongo db
 * 2. 
 * */
package reviews;

import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoIterable;


public class ReviewRequester {
	
	public static int docCount = 0;
	private static MongoCollection<Document> collectMongo;
	private static MongoIterable<Document> docs; 
	private static HeaderHandler hh; 
	private static List<String> timeoutReqs;

	public static void main(String[] args) {
		Secrets rSec = new Secrets();
//		Mongo connection initialize
		localMongo newMongo = new localMongo();		
		collectMongo = newMongo.initMongo("sw","reviews"); System.out.println(collectMongo.count() + " : docs in collection");
		docs = collectMongo.find(new org.bson.Document()).filter(eq("docType","critic"));			
		
		//header init
		String headerPath = rSec.filePath() + "headers.txt";		
		//header handler object		
		hh = new HeaderHandler(headerPath);		
		
		timeoutReqs = new ArrayList<String>();  
		//block on each document
		docs.forEach((Block<Document>) d -> {			
			requesterBlock(d);			
		});
		
		System.out.println(timeoutReqs.size() + " : requests timed out, trying again...");	
		List<Document> timeoutList = new ArrayList<>();				
		
		timeoutList.forEach((Document tOut)-> {
			System.out.println("*************timeout block*****************");
			requesterBlock(tOut);
		});
	}//main

	@SuppressWarnings("unchecked")
	private static void requesterBlock(Document d) {
		String reviewLink = d.getString("reviewLink");		
		ArrayList<Document> reviewsArray = new ArrayList<>();
		//check if "reviews"array exists in mongo doc -- prevent nullpointer e
		if(d.containsKey("reviews")){
			reviewsArray.addAll(d.get("reviews", ArrayList.class));
			if(!reviewsArray.isEmpty()){
				//check if the first review obj is longer than 10,000 chars
				if(reviewsArray.get(0).getString("rawHTML").length() < 10000){
					Document updated = localMongo.findByIdAndClearArray(collectMongo, d, "reviews");
					reviewsArray.clear();
					reviewsArray.addAll(updated.get("reviews", ArrayList.class));
				}
			}else{
				System.out.println("empty : review array");
			}
		}			
		System.out.println(reviewsArray.size() + "    : reviews :  " + reviewLink);		
		if(reviewLink.contains("http") && reviewsArray.isEmpty()){
			org.jsoup.nodes.Document
			//get the review link from the mongo doc
			singleReq = 
					PageGetter.requestOne( reviewLink, hh, false);
			//check if response document is not a "timeout" doc
			if(singleReq.baseUri().contains("http")){
				Document docTo = new Document();
				Date now = new Date();			BasicDBObject serverTime = new BasicDBObject("date", now);
				docTo
					.append("uri", reviewLink)
					.append("datePulled", now.toString())
					.append("dboDate", serverTime.get("date"))
					.append("docType", "REVIEW")
					.append("rawHTML",singleReq.toString())
					.append("request", hh.resps().get("request"))
					.append("response",hh.resps().get("response"))
					;				
				//find and update
				localMongo.findByIdAndPush(collectMongo, d, "reviews", docTo);
				ArrayList<Document> listCheck  = collectMongo.find(eq("_id", d.getObjectId("_id"))).first()
				.get("reviews", ArrayList.class);
				System.out.println(listCheck.size() + " : reviews found in updated doc : html length = " + listCheck.get(0).getString("rawHTML").length());
				docCount++;	
			//check timeout doc
			} else {
				timeoutReqs.add(reviewLink);
				System.out.println("timeout request url: " + singleReq.baseUri());
			}
			//instantiate ticker + wait
			Thread t = new Thread(new localMongo.ticker());  t.start();			
			try {
				Thread.sleep(4000);
			} catch (InterruptedException e) { } //wait command
			t.interrupt();
			try {			Thread.sleep(250);		} catch (InterruptedException e) {		}
		}
	}
	
	
	
	
	
}//class













