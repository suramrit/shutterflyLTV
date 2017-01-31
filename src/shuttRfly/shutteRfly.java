
package shuttRfly;

import shutterIO.*;
import shutterAnalysis.*;

import java.io.*;

import com.google.gson.*;


public class shutteRfly {

	public static void main(String[] args){
		
		// Standard File IO:: 
		try(BufferedReader br = new BufferedReader(new FileReader("./input/file2.txt"))) {
		    StringBuilder sb = new StringBuilder();
		    String line = br.readLine();

		    while (line != null) {
		        sb.append(line);
		        sb.append(System.lineSeparator());
		        line = br.readLine();
		    }
		    
		    String everything = sb.toString();
		   
		    //JsonObject jsonObject = new JsonParser().parse(everything).getAsJsonObject();
		    //isJsonArray():: 
		    JsonArray jsonArray = (JsonArray)(new JsonParser().parse(everything));
		    
		    for(JsonElement e: jsonArray){
		    	//System.out.println(e.getAsJsonObject().has("event_time"));
		    	System.out.println(e.toString());
		    	ingestHandler handler = new ingestHandler();
		    	handler.ingest(e.toString());//can be overridden to handle different data structures.. 
		    }
		    
		    ltvCalculator calcLTV = new ltvCalculator();
		    calcLTV.TopXSimpleLTVCustomers(3); //Any int argument can be passed here
		}
		catch (FileNotFoundException e) {
			// TODO: handle exception
			System.out.println("file exp::"+e);
		}
		catch (IOException e) {
			// TODO: handle exception
			System.out.println("io exp::"+e);
		}
		
		
	}
}
