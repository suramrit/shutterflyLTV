package shutterIO;

import com.google.gson.*;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

import redis.clients.jedis.*;

public class ingestHandler {
	static JedisPoolConfig conf = new JedisPoolConfig();
	static JedisPool pool = new JedisPool(conf,"localhost");
	
	public void ingest(String evnt){	
		
			 JsonObject event = (JsonObject)new JsonParser().parse(evnt);
			 if(event.has("event_time")){
				 updateDuration(event.get("event_time").getAsString());
				 if(event.get("type").getAsString().equals("CUSTOMER")){
					 updateCustomer(event);
				 }
				 else if(event.get("type").getAsString().equals("ORDER")){
					 updateDuration(event.get("event_time").getAsString());
					 updateOrder(event);
				 }
				 else{
					 updateOthers(event);
				 }
			 }
			 //TODO: else store in dumps.. 
	}
	
	private void updateDuration(String eventTime){
		try (Jedis jedis = pool.getResource()) {
			if(!jedis.exists("latestTimeStamp")){
				jedis.set("latestTimeStamp", eventTime);
			}
			else{
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd':'HH:mm");
				Date dPrevStamp = sdf.parse(jedis.get("latestTimeStamp"),new ParsePosition(0));
				Date latest = sdf.parse(eventTime,new ParsePosition(0));
				if(latest.after(dPrevStamp)){
					jedis.set("latestTimeStamp", eventTime);
				}
			}
		}
	}
	
	private void updateCustomer(JsonObject event){	
		System.out.println("user found..");
		String id = event.get("key").getAsString();
		try (Jedis jedis = pool.getResource()) {
			//isEventNEW()
			jedis.sadd("users",id);
			if(event.get("verb").getAsString().equals("NEW")) {
				if(!jedis.exists("user:"+id)){
					jedis.set("user:"+id, event.toString());
					//only put a 'NEW' customer when an unordered 'UPDATE' hasnt been recieved
				}
				else{//an UPDATE was received before NEW
					jedis.sadd("dump:"+id, event.toString());
				}				
			}
			else{
				if(jedis.exists("user:"+id)){
					//check if this update is more recent than the previous one..
					JsonObject oldEvent = (JsonObject)new JsonParser().parse(jedis.get("user:"+id));
					//Note: Time zone is considered uniform for the data... can be changed to include different time zones.. 
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd':'HH:mm");
			
					Date dPrevEvent = sdf.parse(oldEvent.get("event_time").getAsString(),new ParsePosition(0));
					System.out.println("prev dat:"+dPrevEvent);
					Date dCurrEvent = sdf.parse(event.get("event_time").getAsString(),new ParsePosition(0));
					if(dCurrEvent.after(dPrevEvent)){
						jedis.sadd("dump:"+id, jedis.get("user:"+id));
						jedis.set("user:"+id, event.toString());
					}
					else jedis.sadd("dump:"+id, event.toString());
				}
				else jedis.set("user:"+id, event.toString()); //an out of order UPDATE received before NEW 
			}		
		}
	}
	
	private void updateOrder(JsonObject event){
		System.out.println("order found..");
		String id = event.get("key").getAsString();
		String customerID = event.get("customer_id").getAsString();
		try (Jedis jedis = pool.getResource()) {
			jedis.sadd("orders",id);
			if(event.get("verb").getAsString().equals("NEW")){
				if(!jedis.hexists("order:"+customerID, id)){
					jedis.hset("order:"+customerID,id, event.toString());
					//only put a 'NEW' customer when an unordered 'UPDATE' hasnt been recieved
				}
				else 
					jedis.sadd("orderDump:"+customerID,event.toString());
			}
			else{
				if(jedis.hexists("order:"+customerID,id)){
					//check if this update is more recent than the previous one..
					JsonObject oldEvent = (JsonObject)new JsonParser().parse(jedis.hget("order:"+customerID,id));
					//Note: Time zone is considered uniform for the data... can be changed to include different time zones.. 
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd':'HH:mm");
			
					Date dPrevEvent = sdf.parse(oldEvent.get("event_time").getAsString(),new ParsePosition(0));
					System.out.println("prev dat:"+dPrevEvent);
					Date dCurrEvent = sdf.parse(event.get("event_time").getAsString(),new ParsePosition(0));
					if(dCurrEvent.after(dPrevEvent)){
						jedis.sadd("orderDump:"+customerID, jedis.hget("order:"+customerID,id));
						jedis.hset("order:"+customerID, id,event.toString());
					}
					else jedis.sadd("orderDump:"+customerID, event.toString());
				}
				else jedis.hset("order:"+customerID,id, event.toString()); //an out of order UPDATE received before NEW 
			}
			
		}
	}
	
	private void updateOthers(JsonObject event){
		System.out.println("OTHER type found..");
		String id = event.get("key").getAsString();
		String customerID = event.get("customer_id").getAsString();
		String type = event.get("type").getAsString();
		try (Jedis jedis = pool.getResource()) {
			jedis.sadd(type+"s",id);
			jedis.hset(type+":"+customerID,id, event.toString());
		}	
	}
}
