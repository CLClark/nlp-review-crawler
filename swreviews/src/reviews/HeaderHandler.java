package reviews;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.bson.Document;
import org.jsoup.Connection;

public class HeaderHandler {
	
	//headers filepaths
	private String filePath;// = "headersRAW.txt";
//	private String tempFilePath;// = "headersTemp.txt";//headers file	
	private File headersFile;
	private static HashMap<String, String> headersMap;	
	@SuppressWarnings("unused")
	private StringBuffer headersStriBuff;
	private ArrayList<String> refUrls;
	private HashMap<String, Document> responses ;
	
	public HeaderHandler(String fileLoc){
		this.filePath = fileLoc;
		this.headersFile = new File(filePath);			
		headersMap = loadHeaders(headersFile);
		
		refUrls = new ArrayList<String>();
		responses = new HashMap<String, Document>();
	}
	
	public HashMap<String,String> giveHeader(){		
		//--------------------------------------------------------------------------------------------|	
		return headersMap;
	}
	//returns the ref object
	public ArrayList<String> refs(){
		return refUrls;
	} 
	//returns the responses map
	public HashMap<String, Document> resps(){	
		return responses;
	}	
	//returns HashMap from a file
	public static HashMap<String, String> 
	loadHeaders(File fileWithLines) {
		HashMap<String, String> headersOut = new HashMap<String, String>();	        
	       List<String>	headerRecipe = new ArrayList<String>();
		try {
			headerRecipe = FileUtils.readLines(fileWithLines, "UTF-8");
		} catch (IOException e) {		e.printStackTrace();	}	        //Arrays.asList(sBToTrim.toString().split("\\n"));
         	        
	        for (String element : headerRecipe) {	
	            String[] eleKeys = element.split(":", 2);
	            headersOut.put(eleKeys[0], eleKeys[1].trim());
	        }	        
	        return headersOut;
        }//loadHeaders method
	
	//returns mongo doc, for appending to mongo final doc
	public static Document 
	mongoHeadMapper(Map<String, String> mapToMap) {		
		Map <String, Object> map4Request = new HashMap<String, Object>();		
		map4Request.putAll(mapToMap);
		Document newDoc = new Document(map4Request);
		return newDoc;
	}//mongoHeadMapper	
	
	//not used
	public static HashMap<String, String> 
	loadCookies(String filePath) {
		File cookieStore = new File(filePath);
		List<String> cookieRecipe = null;
		try {
		cookieRecipe = FileUtils.readLines(cookieStore, "UTF-8");
		} catch (IOException e) {				e.printStackTrace();			}
		
		HashMap<String, String> cookiesOut = new HashMap<String, String>();    
		     
		for (String element : cookieRecipe) {	
			String[] eleKeys = element.split(":", 2);
			cookiesOut.put(eleKeys[0], eleKeys[1].trim());
		}
		return cookiesOut;
	}//loadCookies method
	

		
		
}
