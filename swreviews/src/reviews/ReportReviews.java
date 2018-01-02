/*
 * Turn mongo review docs into CSV files
 *
 * */

package reviews;

import static com.mongodb.client.model.Filters.eq;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.bson.Document;

import com.mongodb.Block;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoIterable;

public class ReportReviews {
	
	public static int docCount = 0;

	public static void main(String[] args) {
		Secrets secHolder = new Secrets();
//		Mongo connection initialize
		localMongo newMongo = new localMongo();		
		MongoCollection<Document> collectMongo = newMongo.initMongo("sw","reviews");
		System.out.println(collectMongo.count());
//		newMongo.dupeCollection("sw","reviews-backup");		
		MongoIterable<Document> docs = collectMongo.find(new org.bson.Document()).filter(eq("docType","critic"));	
		ArrayList<Document> docBuilder = new ArrayList<Document>();
		
		Block<Document> idGetter = (Document d) -> {
			Document docTo = new Document();		
			d.forEach((sKey,vObj) ->{
				if(!sKey.contains("HTML")){
					docTo.append(sKey, d.get(sKey));
				}
			});			
			docCount++;
//			System.out.println(docCount);			
			docBuilder.add(docTo);		
		};	
		docs.forEach(idGetter);
		
		Date now = new Date();
		String fileName = "critics_" + now.getTime();		
			String fileP = secHolder.filePath() + fileName + ".txt";
			writeUniformDocs(docBuilder, fileP);
	}//main
	
public static void writeUniformDocs(ArrayList<org.bson.Document> list, String filePath) {
		
		//String that will hold all key fields
		String delimChar = "\t";
		String superString = "";
		Set<String> keySet = list.get(0).keySet();
		for(String s : keySet){
			superString =  superString.concat(s + delimChar);
		}
		superString = superString.concat("\n");

		//now iterate over all the docs
		for(org.bson.Document d : list){
			StringBuilder docRow = new StringBuilder();
			Iterator<String> keyIterator = keySet.iterator();
			while(keyIterator.hasNext()){
				String keyNow = keyIterator.next();
				Object foundValue = d.get(keyNow);
				String escapedValue = "zzz";				
//				docRow = docRow.concat(foundValue + delimChar);
				docRow.append((foundValue + delimChar).replaceAll("\n",escapedValue));
//				System.out.println(docRow.toString());
			}
			docRow.append("\n");			
			superString = superString.concat(docRow.toString());
		}		
		
//		String filePath = "F:/Java/MicroPricer/parsing/" + fileName + ".txt";
		File dataNameStore = new File(filePath);
		
		try {
			FileUtils.writeStringToFile(dataNameStore, superString, "UTF-8");
		} catch (IOException e) {			
			e.printStackTrace();
		}		
	}

}//class
