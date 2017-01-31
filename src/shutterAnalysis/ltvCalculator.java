package shutterAnalysis;

import com.google.gson.*;

import java.io.*;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.*;

import redis.clients.jedis.*;

public class ltvCalculator{
	
	static JedisPoolConfig conf = new JedisPoolConfig();
	static JedisPool pool = new JedisPool(conf,"localhost");
	
	Map<String,Double> userLTV = new TreeMap<String, Double>(); //Sorting TreeMap genric ref from : http://stackoverflow.com/questions/2864840/treemap-sort-by-value
			
	public void TopXSimpleLTVCustomers(int top){	
		try (Jedis jedis = pool.getResource()) {
			Set<String> users = jedis.smembers("users");
			
			for(String usr: users){
				double exp = calcExpend(usr,jedis);
				long visits = calcVisit(usr,jedis);
				double expPerVisit =0 ;
				if(visits!=0)
					expPerVisit = (double)exp/visits;
				
				double duration = calcDuration(usr,jedis);
		
				double valPerWeek = (expPerVisit)*(visits/((duration==0)? 1 : duration));
				System.out.println(valPerWeek);
				double ltv_user = 52*valPerWeek*10;
				userLTV.put(usr, ltv_user);
				System.out.println("LTV::"+usr+":"+ltv_user);
			}
		}
		//Alternate?:: 
		Iterator<Map.Entry<String, Double>> itr = entriesSortedByValues(userLTV).iterator();
		for(int i =0;i<top&&itr.hasNext();i++){
			Map.Entry<String, Double> e = itr.next();
			try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
		              new FileOutputStream("./output/out2.txt"), "utf-8"))) {
				writer.write("User::"+e.getKey()+"\\t"+e.getValue());
				writer.newLine(); 
				} catch (UnsupportedEncodingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				}
		}
	}
	
	private double calcExpend(String user, Jedis jedis){
		
		Map<String, String> userOrders = jedis.hgetAll("order:"+user);
		double exp = 0;
		for(String orderJSON: userOrders.values()){
			JsonObject order = (JsonObject)new JsonParser().parse(orderJSON);
			exp+=Double.parseDouble(order.get("total_amount").getAsString().split(" ")[0]);
		}
		//System.out.println(exp);
		return exp;
	}
	
	private long calcVisit(String user, Jedis jedis){
	
		return jedis.hlen("SITE_VISIT:"+user);
				
	}
	
	private double calcDuration(String user, Jedis jedis){
		Map<String, String> userOrders = jedis.hgetAll("order:"+user);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd':'HH:mm");
		Date sysUpdateTime = sdf.parse(jedis.get("latestTimeStamp"),new ParsePosition(0));
		Date earliestTime = sysUpdateTime;
		for(String orderJSON: userOrders.values()){ //check for the earliest TXN in both updated orders and dumps
			JsonObject order = (JsonObject)new JsonParser().parse(orderJSON);
			Date eventTime = sdf.parse(order.get("event_time").getAsString(),new ParsePosition(0));
			if(eventTime.before(earliestTime)){
				earliestTime = eventTime;
			}
		}
		//Now checking Dumps// 
		Set<String> userOrderDumps = jedis.smembers("orderDump:"+user);
		for(String dumpJSON: userOrderDumps){
			JsonObject order = (JsonObject)new JsonParser().parse(dumpJSON);
			Date eventTime = sdf.parse(order.get("event_time").getAsString(),new ParsePosition(0));
			if(eventTime.before(earliestTime)){
				earliestTime = eventTime;
			}
		}
		//System.out.println("user duration::"+((sysUpdateTime.getTime()-earliestTime.getTime())/(1000*60*60*24)));
		return (sysUpdateTime.getTime()-earliestTime.getTime())/(1000*60*60*24); //referred: http://stackoverflow.com/questions/3796841/getting-the-difference-between-date-in-days-in-java (and NOT PROUD of it :| )
	}
	//Need Alternative as this could undermine Redis performance... 
	static <K,V extends Comparable<? super V>>
	SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
	    SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<Map.Entry<K,V>>(
	        new Comparator<Map.Entry<K,V>>() {
	            @Override public int compare(Map.Entry<K,V> e1, Map.Entry<K,V> e2) {
	                int res = e1.getValue().compareTo(e2.getValue());
	                return res != 0 ? res : 1;
	            }
	        }
	    );
	    sortedEntries.addAll(map.entrySet());
	    return sortedEntries;
	}
	
	
	
}