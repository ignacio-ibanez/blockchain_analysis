import org.neo4j.driver.v1.*;
import java.util.*;

//javac -cp neo4j-java-driver-1.0.6.jar analysis_1.java
//java -cp .:neo4j-java-driver-1.0.6.jar Execute

class Execute {

	public static void main (String[] args){

		Driver driver = GraphDatabase.driver( "bolt://localhost:7687", AuthTokens.basic( "neo4j", "123456" ) );
		Session session = driver.session();

		//StatementResult result = session.run( "MATCH (b:Block) WITH COUNT(b) as sumB RETURN sumB" );
		//System.out.println(result.next().get( "sumB" ));

		List<BlockNodes> nodesAnalysed = new ArrayList<BlockNodes>();
		List<Map<String, String>> origin = new ArrayList<Map<String, String>>();

		Map<String, Object> originBlockNode = new HashMap<String, Object>();

		String timeStamp = "49696ef8";
		int scope = 10;
		BlockNodes originBlock = new BlockNodes();
		originBlockNode = originBlock.getOriginNodes(timeStamp, session).getNodes().get(0);
		//System.out.println(originBlockNode.get("magicId"));
		nodesAnalysed.add(0, originBlock);

		int idOut = originBlock.getIds().get("idOut");
		for(int i=0; i<scope; i++){
			BlockNodes nodesBlock = new BlockNodes();
			nodesBlock.getIterationBlock(idOut, session);
			nodesAnalysed.add(i+1, nodesBlock);

		}

		
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
			addToNodes(0,record.get("b").asMap());
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
			addToNodes(2, record.get("o").asMap());
			idOutd = record.get("ID(o)").asDouble();
			int idOut = (int) idOutd;
			addToIds("idOut", idOut);
		}
		
		return this;
	}

	public BlockNodes getIterationBlock(int idOutStart , Session session){
		Map<String, Object> params = new HashMap<String, Object>();

		Record record;

		params.put("idOut", idOutStart);

		// MIRAR QUERY MÁS COMLEJA
		StatementResult result = session.run("START o=node({idOut}) MATCH (o)<-[:ORIGIN_OUTPUT]-(i:Input)-[:TO]->(t:Transaction)-[:TO]->(b:Block) RETURN b,t,ID(t) LIMIT 1",
										params);

		if(result.hasNext()){
			record = result.next();
			addToNodes(0,record.get("b").asMap());
			addToNodes(1,record.get("t").asMap());
			double idTxd = record.get("ID(t)").asDouble();
			int idTx = (int) idTxd;
			params.put("idTx", idTx);
		}

		result = session.run("START t=node({idTx}) MATCH (t)<-[:TO]-(i:Input) RETURN i,ID(i)", params);

		int indexMap = 2;
		while(result.hasNext()){
			record = result.next();
			addToNodes(indexMap, record.get("i").asMap());
			double idInd = record.get("ID(i)").asDouble();
			int idIn = (int) idInd;
			addToIds("idIn"+(indexMap-1).toString(), idIn);
			indexMap++;
		}

		result = session.run("START o=node({idTx}) MATCH (t)<-[:TO]-(o:Output) RETURN o,ID(o)", params);

		int indexOutHigher = 0; // index dentro de la lista donde se encuentra el output a devolver
		indexObjOuts = 0; 
		double higherIndex = 0;  // index mas alto encontrado (el almacenado en hex)
		List<Map<String, Object>> outputs = new ArrayList<Map<String, Object>>();
		while(result.hasNext()){
			record = result.next();
			outputs.add(indexObjOuts, record.get("o").asMap());
			indexTxOutInt = Integer.parseInt(record.get("o").get("indexTxOut").asString(),16);
			if(indexTxOutInt > higherIndex){
				higherIndex = indexTxOutInt;
				indexOutHigher = indexObjOuts;
			}
			outputs.add(indexObjOuts, record.get("o").asMap());
			indexObjOuts++;
		}
		addToNodes(indexMap, outputs.get(indexOutHigher));

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