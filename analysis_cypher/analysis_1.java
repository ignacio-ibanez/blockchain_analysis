import org.neo4j.driver.v1.*;
import java.util.*;

//javac -cp neo4j-java-driver-1.0.6.jar analysis_1.java
//java -cp .:neo4j-java-driver-1.0.6.jar execute

class execute {

	public HashMap originNodes(String timeStamp, Session session){
		Map<Integer, Map> resultsBlocks = new HashMap<Integer, Map>();
		Map<Integer, Map> resultsTransactions = new HashMap<Integer, Map>();
		Map<Integer, Map> resultsOutputs = new HashMap<Integer, Map>();

		Map<String, Object> params = new HashMap<String, Object>();

		Record record;

		params.put("timeStamp", timeStamp);

		StatementResult result = session.run("OPTIONAL MATCH (b:Block)<-[:TO]-(t:Transaction) WHERE b.timeStamp={timeStamp} RETURN b,t,ID(t) LIMIT 1",
											params);
		if(result.hasNext()){
			record = result.next();
			resultsBlocks.put(1, record.get("b").asMap());
			resultsTransactions.put(1, record.get("t").asMap());
			double idTx = record.get("ID(t)").asDouble();
			params.put("idTx", idTx);
			System.out.println(idTx);
		}

		result = session.run("OPTIONAL MATCH (t:Transaction)<-[:TO]-(o:Output) WHERE ID(t)={idTx} RETURN o,ID(o) LIMIT 1",
							params);

		double idOut;
		if(result.hasNext()){
			record = result.next();
			resultsOutputs.put(1, record.get("o").asMap());
			idOut = record.get("ID(o)").asDouble();
		}

		Map<String, Object> originNodes = new HashMap<String, Object>();
		originNodes.put("b", resultsBlocks);
		originNodes.put("t", resultsTransactions);
		originNodes.put("o", resultsOutputs);
		originNodes.put("ID(o)", idOut);
		return originNodes;
	} 

	
	public static void main (String[] args){
		System.out.println("prueba");
		Driver driver = GraphDatabase.driver( "bolt://localhost:7687", AuthTokens.basic( "neo4j", "123456" ) );
		Session session = driver.session();

		//StatementResult result = session.run( "MATCH (b:Block) WITH COUNT(b) as sumB RETURN sumB" );
		//System.out.println(result.next().get( "sumB" ));

		// Diccionarios donde se van a guardar los resultados para luego mostrarlos
		Map<Integer, Map> resultsBlocks = new HashMap<Integer, Map>();
		Map<Integer, Map> resultsTransactions = new HashMap<Integer, Map>();
		Map<Integer, Map> resultsInputs = new HashMap<Integer, Map>();
		Map<Integer, Map> resultsOutputs = new HashMap<Integer, Map>();

		Map<String, Object> origin = new HashMap<String, Object>();

		//Map<String, Object> params1 = new HashMap<String, Object>();


		String timeStamp = "49696ef8";
		origin = originNodes(timeStamp, session);
		//params1.put("timeStamp", timeStamp.toString());
		//StatementResult result = session.run("OPTIONAL MATCH (b:Block)<-[:TO]-(t:Transaction) WHERE b.timeStamp={timeStamp} RETURN b,t,ID(t) LIMIT 1",
		//									params1);
		//if(result.hasNext()){
		//	record = result.next();
		//	resultsBlocks.put(1, record.get("b").asMap());
		//	resultsTransactions.put(1, record.get("t").asMap());
		//	double idTx = record.get("ID(t)").asDouble();
		//	System.out.println(idTx);
		//}
		//System.out.println(resultsTransactions.get(1).get("hashTransaction"));
		//System.out.println(resultsBlocks.get(1).get("magicId"));  
		//params1.put("idTx", idTx.toString());
		//result = session.run("OPTIONAL MATCH (t:Transaction)<-[:TO]-(o:Output) WHERE ID(t)={idTx} RETURN o,ID(o) LIMIT 1",
		//									params1);
		
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