package parsing;
import static com.mongodb.client.model.Filters.eq;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.bson.Document;
import org.jsoup.Jsoup;

import com.mongodb.Block;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoIterable;

import reviews.HeaderHandler;
import reviews.Secrets;
import reviews.localMongo;

public class XMLToText {
	
	public static int docCount = 0;
	private static MongoCollection<Document> collectMongo;
	private static MongoIterable<Document> docs;		
	private static FileWriter fw;
	private static BufferedWriter bw;

	public static void main(String[] args) {

		Secrets secHolder = new Secrets();
//		Mongo connection initialize
		localMongo newMongo = new localMongo();		
		collectMongo = newMongo.initMongo("sw","reviews");		
		docs = collectMongo.find(new org.bson.Document()).filter(eq("docType","critic"));
		System.out.println(collectMongo.count());
		
		List<org.jsoup.nodes.Document> xmlDocs = new ArrayList<>();
		
		Block<Document> xmlGetter = (Document d) -> {
			ArrayList<Document> rArray = d.get("reviews", ArrayList.class);
			Document reviewDoc = null;			
			//check for null first, then if empty
			if (rArray != null && !rArray.isEmpty()) {				
				reviewDoc = rArray.get(0);
				//parse the rawHTML field into a jsoup doc
				org.jsoup.nodes.Document docTo = Jsoup.parse(reviewDoc.getString("rawHTML"), reviewDoc.getString("uri"));				
				docCount++;			
				xmlDocs.add(docTo);
			}						
		};	
		
		docs.forEach(xmlGetter);
		System.out.println(xmlDocs.size() + " : xml docs in list");
		File place = new File(secHolder.filePath() + "line-by-line.txt"); 
		
		try {
			fw = new FileWriter(place);
			bw = new BufferedWriter(fw);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		xmlDocs.forEach((org.jsoup.nodes.Document xml) -> {
			StringBuilder sb = new StringBuilder();
			String raw = "";
			xml.text().length();			
			BufferedReader bf = new BufferedReader(new StringReader(xml.text()));
			String add = "";
		    	try {		    		
		    		do{
		    			add = bf.readLine();
//		    			sb.append(add);
		    			if(add!=null){
		    				raw = raw.concat(add);}
		    		}while(bf.ready() && add != null);
		    		bf.close();
		    		bw.write(raw);
		    		bw.newLine();
		    		bw.flush();
			} catch (IOException e) {			try {
				throw new IOException();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} }		    	
//		    	System.out.println(sb.toString());	
//		    	System.out.println(raw);	    	
			
		});
		
		try {			
			bw.close();
//			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}//main method
	
	
}//class
















