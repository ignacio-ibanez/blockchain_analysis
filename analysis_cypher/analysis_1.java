import org.neo4j.driver.v1.*;
import java.util.*;

//javac -cp neo4j-java-driver-1.0.6.jar analysis_1.java
//java -cp .:neo4j-java-driver-1.0.6.jar execute

class execute {
	
	public static void main (String[] args){
		System.out.println("prueba");
		Driver driver = GraphDatabase.driver( "bolt://localhost:7687", AuthTokens.basic( "neo4j", "123456" ) );
		Session session = driver.session();

		//StatementResult result = session.run( "MATCH (b:Block) WITH COUNT(b) as sumB RETURN sumB" );
		//System.out.println(result.next().get( "sumB" ));

		// Diccionarios donde se van a guardar los resultados para luego mostrarlos
		Map<Integer, Map> resultsBlocks = new HashMap<Integer, Map>();
		Map<String, Map> resultsTransactions = new HashMap<String, Map>();
		Map<String, Map> resultsInputs = new HashMap<String, Map>();
		Map<String, Map> resultsOutputs = new HashMap<String, Map>();

		Map<String, Object> params1 = new HashMap<String, Object>();

		String timeStamp = "49696ef8";
		params1.put("timeStamp", timeStamp.toString());
		StatementResult result1 = session.run("OPTIONAL MATCH (b:Block)<-[:TO]-(t:Transaction) WHERE b.timeStamp={timeStamp} RETURN b,t,ID(t) LIMIT 1",
											params1);
		if(result1.hasNext()){
			Record record1 = result1.next();
			resultsBlocks.put(1, record1.get("b").asMap());
		}
		System.out.println(resultsBlocks.get(1).get("magicId"));  

		/*while (result.hasNext()){
    		Record record = result.next();
    		System.out.println(record);
    		System.out.println(record.get("b").get("magicId").asString());
		}*/

		/*while(result.hasNext()){
			System.out.println(result.next());
		}*/
		/*for(Map<String,Object> map : result){
        	System.out.println(map.toString());
    	}*/
    	/*
		System.out.println(result.next().get("b"));  //.asMap().get("b")
		Object node = result.next().get("b");
		for (String key : node.getPropertyKeys()) {
    		System.out.println("Key: " + key + ", Value: " +  node.getProperty(key));
		}
		*/

		//Values.parameters() ---- OTRA OPCION PARA METER PARAMETROS

		session.close();
		driver.close();
	}
}