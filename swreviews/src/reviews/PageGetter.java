/*
 * request the tomato review pages
 * 
 * */
package reviews;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;

//import mParser.localMongo;
//import mParser.MicroPricer.ticker;

public class PageGetter {	

	public static void main(String[] args) throws InterruptedException {		

		Secrets rSec = new Secrets();
		String headerPath = rSec.filePath() + "headers.txt";		
		//header handler object		
		HeaderHandler hh = new HeaderHandler(headerPath);		
		String link = rSec.get(0);
		System.out.println(totalResults(link, hh));		
		
		//Mongo connection initialize
		localMongo newMongo = new localMongo();
		MongoCollection<org.bson.Document> collect = newMongo.initMongo("sw","reviews");
		
		Scanner input = new Scanner (System.in);
		String answer = null;
		List<Integer> pageList = null;
		
		do{			
			pageList = whichPages();			
			System.out.printf("Execute Request ? (y/n)");			
//			input = new Scanner (System.in);
			answer = input.next();
		}		
		while (!answer.toLowerCase().contains("y"));		

		/*		*  Execute the requests	*        			*/		
		List<String> pulledPageResults = new ArrayList<String>(); //to be printed out later
		Integer cDown = pageList.get(2); //System.out.println(pageList.get(2) + "pageList.get(2)");		
		do{
			cDown--;
			Integer pageCursor = (pageList.get(1) - cDown); 
			String linkBuilt = hyperlinkIterator(link,pageCursor); 
			Document singleReq = 
					requestOne( linkBuilt, hh, true);
			//System.out.println(pageCursor +"pageCursor value before sleep");
			Date now = new Date();
			
			System.out.println(singleReq.baseUri());
			try{
			pulledPageResults.add("Page " + pageCursor.toString());
			pulledPageResults.add(singleReq.baseUri());
			} catch (NullPointerException nPE) {System.out.println("Null Pointer on pulledPageResults");}
			
			//server time for appending to mongo doc			
			BasicDBObject serverTime = new BasicDBObject("date", now);		
			//Create mongo doc and insert the raw html
			org.bson.Document doc =
			new org.bson.Document("page", pageCursor)
				.append("uri", linkBuilt)
				.append("datePulled", now.toString())
				.append("dboDate", serverTime.get("date"))
				.append("docType", "SEARCH")
				.append("rawHTML",singleReq.toString())
				.append("request", hh.resps().get("request"))
				.append("response",hh.resps().get("response"))
			;
			hh.resps().clear();			//clear the header map
			//insert doc into mongo
			collect.insertOne(doc);
			if(cDown == 0) {
				java.awt.Toolkit.getDefaultToolkit().beep();
				break;
			}			
			//Crawler timer with random num generator
			Random randN = new java.util.Random();
			int foure = (randN.nextInt(11491)) + 20000;
			System.out.printf("%1$.2fs: ", ((float)foure/1000)); //timer to screen
			
			//instantiate ticker + wait
			Thread t = new Thread(new ticker());  t.start();			
			Thread.sleep((foure)); //wait command
			t.interrupt();			
		}
		
		while(cDown>0);
		
		//report out
		System.out.println(collect.find().first().toJson());
		System.out.printf((pulledPageResults.size()/2) + " Results");
		
	}//main method	
	
	/* Request a single http page, return jsoup document */	
	public static org.jsoup.nodes.Document	
	requestOne(String hyperlink, HeaderHandler headHand, Boolean ref){
		org.jsoup.nodes.Document doc = new org.jsoup.nodes.Document("timeout dummy");
		org.jsoup.Connection concoction = Jsoup.connect(hyperlink)	
//			.header("host", "")
			.headers(headHand.giveHeader())
			.timeout(12000);		
		if(ref != true){
			try {
		doc = concoction.get();
			} catch (SocketTimeoutException  sT) {
				System.out.println("socket timeout | return \"timeout\" doc");
				doc = new org.jsoup.nodes.Document("timeout");
			} catch (IOException e) {
				 e.printStackTrace(); //throw new RuntimeException(e);
			}		
		}else{
			//check if ref list is not empty
			if(!headHand.refs().isEmpty()){
				//get the last String in the list 
				concoction.referrer(headHand.refs().get(headHand.refs().size()-1));
			}
			try {
			//execute request
			doc = concoction.get();
				//add the requested url to the referrer list
				headHand.refs().add(hyperlink);
				//add the req/resp objects to the headHandler object... persistance?
				headHand.resps().put("request", HeaderHandler.mongoHeadMapper(concoction.request().headers()));
				headHand.resps().put("response", HeaderHandler.mongoHeadMapper(concoction.response().headers()));
			} catch (SocketTimeoutException  sT) {
				System.out.println("socket timeout | return \"timeout\" doc");
				doc = new org.jsoup.nodes.Document("timeout");
			} catch (IOException e) {
				e.printStackTrace(); //throw new RuntimeException(e);
			}
		}
System.out.println(concoction.response().statusCode() + " : server response : " + concoction.request().url().toString());
		return doc;	
	}		
	
	/* totalResults finder	 * Request page 1 and parse for "last page" link, outputs it	 */
	public static int 
	totalResults (String hyperlink, HeaderHandler headHand){
		
		Document totalResultFinder = null;
		totalResultFinder = requestOne(
				hyperlink,
				headHand, false);
		
		Element foundResultNode = totalResultFinder.select(".pageInfo").first();
		
		String[] toParse = foundResultNode.text().split("\\s", 20);
		List<Integer> pageNums = new ArrayList<Integer>();
		for(String s : toParse){
			if (isParsable(s)){
				pageNums.add(Integer.parseInt(s));	
			}			
		}				
		pageNums.sort((a,b) -> b.compareTo(a));				
		//parsing logic 
		int foundResult = pageNums.get(0);
		return foundResult;		
	}
	
	/*	 * Request Page Inputs	 *		 */
	public static List<Integer>	whichPages(){		
	/*	 * Logic for inputs	 */
		boolean errorFlag = false;
		int lowerBound 	= 1;
		int upperBound 	= 0;
		int diffBound = 0;		
		List<Integer> innerList = new ArrayList<Integer>(3);
		
		do {
				errorFlag = false; //reset flag after loop
				try {
					Scanner input = new Scanner(System.in);
					System.out.printf("Enter FIRST page to request: ");
					String firstPageReq = input.next();

					System.out.printf("Enter LAST page to request: ");
					String lastPageReq = input.next();

					if (!(Integer.parseInt(firstPageReq) > 0) || !(Integer.parseInt(lastPageReq) > 0)) {
						System.out.println("Must enter postive integer...");
						throw new java.lang.Error("ERROR: Must enter postive integer...");
					}

					lowerBound = Integer.parseInt(firstPageReq);
					upperBound = Integer.parseInt(lastPageReq);
					diffBound = Math.abs(upperBound - lowerBound) + 1;

					if (upperBound < lowerBound) {
						System.out.println("...");
						System.out.println("Swapping Pages");
						upperBound = Integer.parseInt(firstPageReq);
						lowerBound = Integer.parseInt(lastPageReq);
					}

					innerList.add(0, lowerBound);
					innerList.add(1, upperBound);
					innerList.add(2, diffBound);

					if (diffBound > 50) {
						System.out.println(diffBound + " pages requested. PLEASE CONFIRM... (y/n)");
						Scanner input2 = new Scanner(System.in);
						String confirmDiff = input2.nextLine();
						if (!(confirmDiff.toLowerCase().contains("y"))){
							errorFlag = true;
						}
					}		
				} catch (NumberFormatException e) {
						System.out.println();
					System.out.println("***INPUT ERROR: ... RETRY***");
						System.out.println();
						System.out.println();
					errorFlag = true; //flag the error
				} 
			} while (errorFlag); //loop if error was flagged
		
		System.out.printf(diffBound + " Page(s)");
		System.out.println(": " + lowerBound + " through " + upperBound);
		
		return innerList;
	}
	
/* * Pass in an integer, get back hyperlink (page number)* */
public static String 
hyperlinkIterator(String baseLink, Integer whichPage){
	URIBuilder uriB = null;
	try {
		uriB = new URIBuilder(baseLink);
	} catch (URISyntaxException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	uriB.setParameter("page", whichPage.toString());
	uriB.setParameter("sort", "");
	return uriB.toString();		
}
	
	/* Number checker - helper method for totalResultFinder 	 */
	public static boolean 
	isParsable(String input){
	    boolean parsable = true;
	    try{
	        Integer.parseInt(input);
	    }catch(NumberFormatException e){
	        parsable = false;
	    }
	    return parsable;
	}

	//ticker method + class
	private static class 
	ticker	implements Runnable {
		public void run() {
			String importantInfo = "[]";
			try { //double for loop
				for (int z = 0; z < 60; z++) {			    	
					// Pause for 1 second
					Thread.sleep(1000);
					// Print a message
					System.out.print(importantInfo);
				}
				System.out.println("60 seconds waiting...");
			} catch (InterruptedException e) {		System.out.println("[tock!]");		}
		}
	}	
	
}//PageGetter class




























