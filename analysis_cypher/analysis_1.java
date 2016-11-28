import org.neo4j.driver.v1.*;
import java.util.*;

//javac -cp neo4j-java-driver-1.0.6.jar analysis_1.java
//java -cp .:neo4j-java-driver-1.0.6.jar Execute

class Execute {
/*
	public static List originNodes(String timeStamp, Session session){
		Map<String, String> ids = new HashMap<String, String>();

		List<Map<String, Object>> originNodes = new ArrayList<Map<String, Object>>();

		Map<String, Object> params = new HashMap<String, Object>();

		Record record;

		params.put("timeStamp", timeStamp);

		StatementResult result = session.run("OPTIONAL MATCH (b:Block)<-[:TO]-(t:Transaction) WHERE b.timeStamp={timeStamp} RETURN b,t,ID(t) LIMIT 1",
											params);
		if(result.hasNext()){
			record = result.next();
			originNodes.add(0, record.get("b").asMap());
			originNodes.add(1, record.get("t").asMap());
			double idTxd = record.get("ID(t)").asDouble();
			int idTx = (int) idTxd;
			params.put("idTx", idTx);
			ids.put("idTx1", idTx.toString());
		}

		//StatementResult result = session.run("OPTIONAL MATCH (t:Transaction)<-[:TO]-(o:Output) WHERE ID(t)={idTx} RETURN o,ID(o) LIMIT 1",
		//					params);
		//result = session.run("OPTIONAL MATCH (t:Transaction) WHERE id(t)={idTx} RETURN t LIMIT 1", params);
		result = session.run("START t=node({idTx}) MATCH (t)<-[:TO]-(o:Output) RETURN o,ID(o) LIMIT 1", params);
		
		double idOut;
		if(result.hasNext()){
			record = result.next();
			originNodes.add(2, record.get("o").asMap());
			idOutd = record.get("ID(o)").asDouble();
			int idOut = (int) idOutd;
			ids.put("idOut1", idOut.toString());
			originNodes.add(3, ids);
		}
		
		return originNodes;
	}

*/
	public static void main (String[] args){

		Driver driver = GraphDatabase.driver( "bolt://localhost:7687", AuthTokens.basic( "neo4j", "123456" ) );
		Session session = driver.session();

		//StatementResult result = session.run( "MATCH (b:Block) WITH COUNT(b) as sumB RETURN sumB" );
		//System.out.println(result.next().get( "sumB" ));

		// Diccionarios donde se van a guardar los resultados para luego mostrarlos
		/*Map<Integer, Map> resultsBlocks = new HashMap<Integer, Map>();
		Map<Integer, Map> resultsTransactions = new HashMap<Integer, Map>();
		Map<Integer, Map> resultsInputs = new HashMap<Integer, Map>();
		Map<Integer, Map> resultsOutputs = new HashMap<Integer, Map>();*/

		List<Map<String, String>> origin = new ArrayList<Map<String, String>>();

		//Map<String, Object> params1 = new HashMap<String, Object>();
		Map<String, Object> originBlock = new HashMap<String, Object>();

		String timeStamp = "49696ef8";
		BlockNodes originObj = new BlockNodes();
		originBlock = originObj.getOriginNodes(timeStamp, session).getNodes().get(0);
		System.out.println(originBlock.get("magicId"));
		//origin = originNodes(timeStamp, session);
		//params1.put("timeStamp", timeStamp.toString());
		//StatementResult result = session.run("OPTIONAL MATCH (b:Block)<-[:TO]-(t:Transaction) WHERE b.timeStamp={timeStamp} RETURN b,t,ID(t) LIMIT 1",
		//									params1);
		//if(result.hasNext()){
		//	record = result.next();
		//	resultsBlocks.put(1, record.get("b").asMap());
		//	resultsTransactions.put(1, record.getProperty("t").asMap());
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

class BlockNodes{
	private Map<String, Integer> ids = new HashMap<String, Integer>();
	private List<Map<String, Object>> nodes = new ArrayList<Map<String, Object>>();

	public BlockNodes getOriginNodes(String timeStamp, Session session){
		Map<String, Object> params = new HashMap<String, Object>();

		Record record;

		params.put("timeStamp", timeStamp);

		StatementResult result = session.run("OPTIONAL MATCH (b:Block)<-[:TO]-(t:Transaction) WHERE b.timeStamp={timeStamp} RETURN b,t,ID(t) LIMIT 1",
											params);
		if(result.hasNext()){
			record = result.next();
			//nodes.add(0, record.get("b").asMap());
			addToNodes(0,record.get("b").asMap());
			//nodes.add(1, record.get("t").asMap());
			addToNodes(1, record.get("t").asMap());
			double idTxd = record.get("ID(t)").asDouble();
			int idTx = (int) idTxd;
			params.put("idTx", idTx);
			addToIds("idTx", idTx);
		}

		//StatementResult result = session.run("OPTIONAL MATCH (t:Transaction)<-[:TO]-(o:Output) WHERE ID(t)={idTx} RETURN o,ID(o) LIMIT 1",
		//					params);
		//result = session.run("OPTIONAL MATCH (t:Transaction) WHERE id(t)={idTx} RETURN t LIMIT 1", params);
		result = session.run("START t=node({idTx}) MATCH (t)<-[:TO]-(o:Output) RETURN o,ID(o) LIMIT 1", params);
		
		double idOutd;
		if(result.hasNext()){
			record = result.next();
			//nodes.add(2, record.get("o").asMap());
			addToNodes(2, record.get("o").asMap());
			idOutd = record.get("ID(o)").asDouble();
			int idOut = (int) idOutd;
			//ids.put("idOut1", idOut.toString());
			//nodes.add(3, ids);
			addToIds("idOut", idOut);
		}
		
		return this;
	}

	public void addToNodes(int position, Map<String, Object> newNode){
		this.nodes.add(position, newNode);
	}

	public void addToIds(String key, int value){
		this.ids.put(key, value);
	}

	public List<Map<String, Object>> getNodes(){
		return this.nodes;
	}

	public Map<String, Integer> getIds(){
		return this.ids;
	}
}