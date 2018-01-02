/*
 * Parse mongo doc html into critics,
 * then insert the critics into the database as new documents
 * */

package reviews;

import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoIterable;


public class ReviewerFactory {
	
	static MongoCollection<org.bson.Document> collect;
	static List<org.bson.Document> existingProducts;
	static List<String> existingProductsSKUs;
	static List<org.bson.Document> parsedProducts;
	static List<org.bson.Document> parsedAndFilteredProducts;
	static List<org.bson.Document> pAFPToBeUpdated;

	public static void main(String[] args) {

		/* Mongo connection initialize		 */
		localMongo newMongo = new localMongo();		
		collect = newMongo.initMongo("sw","reviews");
System.out.println(collect.count() + " : docs found in collection");
printf("connecting to mongo database...");				
		//this list is the source HTML data 
		MongoIterable<org.bson.Document> mcSearchResults = collect.find(new org.bson.Document()).filter(eq("docType","SEARCH"));
printf("querying mongo for docType SEARCH...");
		List<org.bson.Document> arrayOfMongoDocs = new ArrayList<>();
		mcSearchResults.into(arrayOfMongoDocs);
printf("inserting query results into array list object");		
		
		//get collection's documents' keys
//		localMongo.collectionKeys(mcSearchResults);
printf("execute collectionkeys method on the query results...");		
		
		//this list is for later checking if parsed products already exist in db
		MongoIterable<org.bson.Document> mongoExProducts= collect.find(new org.bson.Document()).filter(eq("docType","critic".toLowerCase()));
printf("querying mongo for docType PRODUCT... e.x. existing products");
		existingProducts = new ArrayList<>();
		mongoExProducts.into(existingProducts);
		existingProductsSKUs = new ArrayList<>(); 
printf("inserting query results into an array list object (existing products)");
			for(org.bson.Document d : existingProducts){
				if(d.containsKey("criticName")){
					existingProductsSKUs.add(d.getString("criticName"));
				}
			}			
printf("make a list of the SKUs for later comparison, of existing products: " + existingProductsSKUs.size());
		parsedProducts = new ArrayList<>();
printf("created the empty list of parsed products...");
printf("executing a for loop with each SEARCH doc returned from first query");
		//this is a boolean to show each URI and product found or not...
		boolean reportOrNot = false;
		for(org.bson.Document d : arrayOfMongoDocs){
			
			Document eachMCResultPage = Jsoup.parse(d.getString("rawHTML"), d.getString("uri"));
printf("extracting the rawHTML data from the document...");	
			parsedProducts.addAll(
					spawnDocs(eachMCResultPage, d, reportOrNot));
printf("parsing the rawHTML for any found documents");			
printf("adding all the parsed products into the initially empty list...");
		}		
//		parsedProducts.forEach(d ->{
//			System.out.println(d.toJson());
//		});		
		//report out
		System.out.println("Total Critics found: "+ parsedProducts.size());
		System.out.println("Insert? (y/n)");
		Scanner input = new Scanner(System.in);
		String answer = input.next(); 
		if( answer.contains("y")){
			
			parsedAndFilteredProducts = new ArrayList<>(); //the list to add new products (did not already exist in db)
			pAFPToBeUpdated = new ArrayList<>(); //the list for updates (existing products found in db)
			
			ListIterator<org.bson.Document> iterThruProds = parsedProducts.listIterator();
			
			while(iterThruProds.hasNext()){
				
				org.bson.Document doc2Check = iterThruProds.next();
				String productExistsTerm = "what to check ?";
				String existsField = "criticName";
				
				System.out.print(doc2Check.get("criticName"));
				//logic to verify criticName is there	
				
				boolean docsExistance = false; 
				Iterator<String> scrollOverSKUs = existingProductsSKUs.iterator();
				while(scrollOverSKUs.hasNext()){
					productExistsTerm = doc2Check.getString("criticName").toLowerCase().trim();	
					String existingSKU = scrollOverSKUs.next();
					if (existingSKU.toLowerCase().trim().contains(productExistsTerm)){						
						System.out.println("matched doc2Check and existingSKU...");
						docsExistance = true;
						scrollOverSKUs.remove();
					}
				}				
				//Get a list of existing items in DB
				//get the SKUs or (data-names of the existing items) - MicroCenter specific exists method
				//check if doc2Check sku is included in that list...				
				
				if(docsExistance == false){
					System.out.print("searchDBWith isEmpty (true)  | added to create new list");
					//create new listing for product
					parsedAndFilteredProducts.add(doc2Check);
				}
				else{
					System.out.print("criticName already exists / added to update list");
					//add to the existing product listing					
					//append to an array list for "To Be Updated"
					//update: take price data from source doc, add it to array list... same idea as ebay class
					pAFPToBeUpdated.add(doc2Check);					
				}				
				System.out.println(doc2Check.getString(existsField) + productExistsTerm);				
			}			
			System.out.println("------------------------------------------------------------------------");
			
			//method to handle the lists after iteration
			int pAFPSize = parsedAndFilteredProducts.size();
			int pAFPTBUSize = pAFPToBeUpdated.size();
			System.out.println(pAFPSize + " = size of list for new items only");
			System.out.println(pAFPTBUSize + " = size of list of ToBeUpdated");

			if(parsedAndFilteredProducts.isEmpty() && pAFPToBeUpdated.isEmpty()){
				System.out.println("Lists are empty... ");
				//no actions
			}
			else if(parsedAndFilteredProducts.isEmpty() && !pAFPToBeUpdated.isEmpty()){
//				only updates
				System.out.println(" only updates ");
//				for(org.bson.Document d : pAFPToBeUpdated){					
//				};			
			}
			else if(!parsedAndFilteredProducts.isEmpty() && pAFPToBeUpdated.isEmpty()){
				//only inserts
				System.out.println(" only inserts ");
				collect.insertMany(parsedAndFilteredProducts);
			}
			else{
				System.out.println("inserts and updates ");
				//inserts and updates
				collect.insertMany(parsedAndFilteredProducts);
				System.out.println("Inserts " + pAFPSize);
				System.out.println("Updates " + pAFPTBUSize);
			}
//			System.out.println("None Inserted.");
		}//if "yes"		
		input.close(); //close scanner	

		MongoIterable<org.bson.Document> afterDocs = collect.find(new org.bson.Document());

		List<org.bson.Document> afterDocsList = new ArrayList<>();
		afterDocs.into(afterDocsList);
		
		int cookiemungie = 0;
		for(org.bson.Document d : afterDocsList){
//			System.out.print(d.get("data-name")+"  ...");
			cookiemungie++;
		}
		System.out.println();
		System.out.println("found cookiemungies: " + cookiemungie);		
		
	}//main method
	
	@SuppressWarnings("unchecked")
	private static List<org.bson.Document> spawnDocs(Document doc, org.bson.Document sourceDoc, boolean askToReport) {		

		String sourceURI = sourceDoc.getString("uri"); // 
		Date sourceDocDate = extractSourceDate(sourceDoc);	
		
		List<org.bson.Document>	
			productsList = new ArrayList<>(); //destination list of documents 
		Elements 
			pWrapper = doc.getElementsByClass("row review_table_row"); //list of products from source document
		
System.out.println(pWrapper.size() + " : size of pwrapper");
		//product nodes to products	
		for(Element produce : pWrapper){		// <div> row review_table_row
System.out.println(produce.wholeText());
			org.bson.Document criticDetails = new org.bson.Document(); //declaring each product as a new document						
			
			Element nameNode = produce.getElementsByClass("critic_name").first();
			Elements nameAndMovie = nameNode.select("a");
			nameAndMovie.forEach(node -> {
				CharSequence cs = "critic";
				if(node.attr("href").contains(cs)){
					criticDetails.append("criticName", node.html());
					criticDetails.append("criticNameHref", node.attr("href"));
				}
				else if(node.childNodeSize() == 0){
					criticDetails.append("groupName", node.html());
				}
			});
			//date node
			Element dateNode = produce.getElementsByClass("review_date").first();
			if(dateNode.hasText()){
				criticDetails.append("reviewDate", dateNode.html());			
			}else{
				criticDetails.append("reviewDate", "no data");			}
			
			//review shortText
			Element descNode = produce.getElementsByClass("the_review").first();
			if(descNode.hasText()){
				criticDetails.append("shortText", descNode.html());			
			}else{
				criticDetails.append("shortText", "no data");			}
			
			//full review link
			Element revNode = produce.getElementsByClass("review_desc").first();			
			if(revNode.childNodeSize()>0){
				Element nowNode = revNode.getElementsByTag("a").first();				
				criticDetails.append("reviewLink", nowNode.attr("href"));
			}else{
				criticDetails.append("reviewLink", "no data");			}
			
			//review shortText
			Element originalScore = revNode.getElementsByClass("small subtle").first();
			if(originalScore.hasText()){
				criticDetails.append("scoreText", originalScore.html());
			}else{
				criticDetails.append("scoreText", "no data");			}			
			
			criticDetails
			//raw product_details html
				.append(produce.className(),produce.toString())
				.append("docType", "critic")
			//source of HTML (uri)
				.append("uri", sourceURI)	;			
			
			printf(criticDetails.toJson());
			//ADD ProductDetails Object to Products LIST<>	
			productsList.add(criticDetails);
		}	
		
//		if(askToReport==true){
//			System.out.println("Products found: "+productsList.size());
//			System.out.println("From URI: "+ sourceURI);
//		}		
		
		return productsList;
	}//spawnDocs method
	
	/* * Try to extract a date from the source doc*  */
	private static Date extractSourceDate(org.bson.Document sourceDoc) {
	Date sourceDocDate = new Date(); //
		if (sourceDoc.containsKey("dboDate")){
			sourceDocDate = sourceDoc.getDate("dboDate");
		} else{
			try{
				sourceDocDate = (Date) sourceDoc.get("datePulled");				
			}catch(NullPointerException e){
				System.out.println("***NullPointer on singlePriceDoc append - \"datePulled\"");	
			}
		}
		return sourceDocDate;
	}//extractDate method
	
	
	
	
	public static void
	printf(String s){
		System.out.println(s);	
	}
}//RF class



























