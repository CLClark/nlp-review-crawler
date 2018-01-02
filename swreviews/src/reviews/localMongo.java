package reviews;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.push;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bson.Document;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;

public class localMongo {
		
		public static void collectionKeys(MongoIterable<org.bson.Document> dbQuery) {
			//iterate over each doc in the collection
			dbQuery.forEach( (Block<org.bson.Document>) d -> {
			//iterate over each doc's keyset
				Iterator<String> diterator = d.keySet().iterator();
				while(diterator.hasNext()){
					//do something with each key
					String keyD = diterator.next().toString();
					
					System.out.print(keyD+": ");
					
//					if(keyD.startsWith("date")||keyD.startsWith("page")){						
						System.out.print(d.get(keyD));//						
//					}
					System.out.println("");
					
				}			
			System.out.println("--------------");
			});
		}
//	}
	
	public String dbString;
	public String collString;
	public Logger mongoLogger;
	
	public MongoCollection<org.bson.Document> collection;
	public MongoDatabase database;
	public MongoClient mongoClient;
	public localMongo(){ //constructor		
	};	
	
	public String closeMongo(){
		//mongoClient.close();
		//System.out.println("called close mongo");
		return ("called close mongo?");
	}
@SuppressWarnings("resource")
public MongoCollection<org.bson.Document> initMongo(String dbName, String colName){	
	 	mongoLogger = Logger.getLogger("org.mongodb.driver");
	 	mongoLogger.setLevel(Level.SEVERE);
	 	
	 	Secrets secretStore = new Secrets();
	 	
	 	MongoClient mongoClient;
	    MongoClientURI connectionString = new MongoClientURI(secretStore.mongoUri());
	    mongoClient = new MongoClient(connectionString);  
	    System.out.println("Don't forget to mongoClient.close()");
	    dbString = dbName;
	    collString = colName;
	    System.out.printf("db = " + dbString + ", ");
	    System.out.println("collection = " + collString);    
	    
	    // get handle to "mydb" database
	    database = mongoClient.getDatabase(dbName);

	    // get a handle to the "test" collection
	    MongoCollection<org.bson.Document> collection = database.getCollection(colName);
	    			return collection;		
	}

public void dupeCollection(String fromColl, String newCollName){
	MongoCollection<Document> checkExisting = database.getCollection(newCollName);
	MongoCollection<Document> newColl;
	if(checkExisting == null){
		System.out.println("found no coll");
		database.createCollection(newCollName);		
	}
	else{
		System.out.println("found backup collection");
	}
	
	newColl = database.getCollection(newCollName);
	collection = database.getCollection(fromColl);
	
	FindIterable<Document> oldDocs = collection.find();		
	List<Document> listOfDocs = new ArrayList<>();
	oldDocs.into(listOfDocs);
	
	newColl.insertMany(listOfDocs);
	System.out.println(newColl.count() + ": docs in new collection " + newColl.getNamespace().getCollectionName());
}
	
public void listMongo(){
		
		MongoClient mongoClient;
	    MongoClientURI connectionString = new MongoClientURI("mongodb://127.0.0.1:3099");
	    mongoClient = new MongoClient(connectionString);
	    
        MongoIterable<String> dbNames = mongoClient.listDatabaseNames();
        for (String e : dbNames){
        	System.out.println("mongodb: " + e);        	
        }
        System.out.println("No collection specified. Connection closing");
        mongoClient.close();
        
	    // get handle to "mydb" database
//	    MongoDatabase database = mongoClient.getDatabase(dbName);
	    
	    // get a handle to the "test" collection
//	    MongoCollection<org.bson.Document> collection = database.getCollection(colName);
			
		
	}

	public static void findByIdAndPush(MongoCollection<org.bson.Document> collect, org.bson.Document existingMongoDoc, String arrayFieldToPush, Object objectToPush) {
		//collection name, 
		//old doc, new doc,
		//field to push, object to add to the array		
		collect.updateOne(
				eq(
						"_id",
						existingMongoDoc.getObjectId("_id")),
				push(
						arrayFieldToPush,
						objectToPush)					
		);		
	}
	
	//ticker method + class
	public static class 
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
			} catch (InterruptedException e) {		
				System.out.println("[tock!]");
			}
		}
	}	
	
}
