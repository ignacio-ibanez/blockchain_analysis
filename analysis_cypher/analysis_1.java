import org.neo4j.driver.v1.*;
import java.util.*;

//javac -cp neo4j-java-driver-1.0.6.jar analysis_1.java
//java -cp .:neo4j-java-driver-1.0.6.jar Execute

class Execute {

	public static void main (String[] args){

		Driver driver = GraphDatabase.driver( "bolt://localhost:7687", AuthTokens.basic( "neo4j", "123456" ) );
		Session session = driver.session();

		List<BlockNodes> nodesAnalysed = new ArrayList<BlockNodes>();
		List<Map<String, String>> origin = new ArrayList<Map<String, String>>();

		Map<String, Object> originBlockNode = new HashMap<String, Object>();

		String timeStamp = "49696ef7";
		int scope = 1;
		BlockNodes originBlock = new BlockNodes();
		originBlockNode = originBlock.getOriginNodes(timeStamp, session).getNodes().get(0);
		nodesAnalysed.add(0, originBlock);
		/*System.out.println("El nodo origen ha sido guardado");
		System.out.println("El hash del bloque origen es: " + originBlockNode.get("hashHeader"));
		System.out.println("El hash de la transaccion origen es: " + originBlock.getOriginNodes(timeStamp, session).getNodes().get(1).get("hashTransaction"));
		System.out.println("El indexTxOut del output es: " + originBlock.getOriginNodes(timeStamp, session).getNodes().get(2).get("indexTxOut"));
		System.out.println("El scriptLength del output es: " + originBlock.getOriginNodes(timeStamp, session).getNodes().get(2).get("scriptLength"));
		System.out.println("El id de la transaccion es: " + originBlock.getOriginNodes(timeStamp, session).getIds().get("idTx"));
		System.out.println("El id del output es: " + originBlock.getOriginNodes(timeStamp, session).getIds().get("idOut"));
		System.out.println("");*/


		// Obtención de los siguientes bloques en el seguimiento hacia adelante
		int idOut = originBlock.getIds().get("idOut");   //--- SE PONE EL DE ABAJO PARA PRUEBAS, LUEGO DESCOMENTAR
		//int idOut = 957;    // PRUEBA
		for(int i=1; i<=scope; i++){
			BlockNodes nodesBlock = new BlockNodes();
			if(nodesBlock.getIterationBlock(idOut, session) == null){
				break;
			}
			nodesAnalysed.add(i, nodesBlock);
			/*int numberNodes = nodesAnalysed.get(i).getNodes().size();
			System.out.println("Número de nodos: " + numberNodes);
			System.out.println("El hash del nodo bloque es: " + nodesAnalysed.get(i).getNodes().get(0).get("hashHeader"));
			System.out.println("El hash de la transaccion es: " + nodesAnalysed.get(i).getNodes().get(1).get("hashTransaction"));
			System.out.println("El indexTxOut del output es: " + nodesAnalysed.get(i).getNodes().get(numberNodes-1).get("indexTxOut"));
			System.out.println("El script del primer input es: " + nodesAnalysed.get(i).getNodes().get(2).get("script"));*/
		}


		// Obtención de los bloques anteriores en el seguimiento hacia atras
		//for(int i=1; i<nodesAnalysed.size(); i++){

		//}

		
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

		String queryCypher = "OPTIONAL MATCH (b:Block)<-[:TO]-(t:Transaction)<-[:TO]-(o:Output) WHERE b.timeStamp={timeStamp} RETURN b,t,o,ID(t),ID(o) LIMIT 1";
		StatementResult result = session.run(queryCypher, params);
		int timeStampModUpInt;
		String timeStampModUp = timeStamp;
		int timeStampModDownInt;
		String timeStampModDown = timeStamp; 
		boolean up = true;
		record = result.next();
		while(record.get("b").toString() == "NULL"){
			if(up){
				timeStampModUpInt = Integer.parseInt(timeStampModUp, 16) + 1;
				timeStampModUp = Integer.toHexString(timeStampModUpInt);
				params.replace("timeStamp", timeStampModUp);
				up = false;
			}else{
				timeStampModDownInt = Integer.parseInt(timeStampModDown, 16) - 1;
				timeStampModDown = Integer.toHexString(timeStampModDownInt);
				params.replace("timeStamp", timeStampModDown);
				up = true;
			}
			result = session.run(queryCypher, params);
			record = result.next();
		}

		
		addToNodes(0,record.get("b").asMap());
		addToNodes(1, record.get("t").asMap());
		addToNodes(2, record.get("o").asMap());

		double idTxd = record.get("ID(t)").asDouble();
		int idTx = (int) idTxd;
		addToIds("idTx", idTx);

		double idOutd = record.get("ID(o)").asDouble();
		int idOut = (int) idOutd;
		addToIds("idOut", idOut);

		//StatementResult result = session.run("OPTIONAL MATCH (t:Transaction)<-[:TO]-(o:Output) WHERE ID(t)={idTx} RETURN o,ID(o) LIMIT 1",
		//					params);
		//result = session.run("OPTIONAL MATCH (t:Transaction) WHERE id(t)={idTx} RETURN t LIMIT 1", params);
		/*result = session.run("START t=node({idTx}) MATCH (t)<-[:TO]-(o:Output) RETURN o,ID(o) LIMIT 1", params);
		
		double idOutd;
		if(result.hasNext()){
			record = result.next();
			addToNodes(2, record.get("o").asMap());
			idOutd = record.get("ID(o)").asDouble();
			int idOut = (int) idOutd;
			addToIds("idOut", idOut);
		}*/
		
		return this;
	}

	public BlockNodes getIterationBlock(int idOutStart , Session session){
		Map<String, Object> params = new HashMap<String, Object>();

		Record record;

		params.put("idOut", idOutStart);

		StatementResult result = session.run("START o=node({idOut}) MATCH (o)<-[:ORIGIN_OUTPUT]-(i:Input)-[:TO]->(t:Transaction)-[:TO]->(b:Block) RETURN b,t,ID(t) LIMIT 1",
										params);

		if(result.hasNext()){
			record = result.next();
			addToNodes(0,record.get("b").asMap());
			addToNodes(1,record.get("t").asMap());
			double idTxd = record.get("ID(t)").asDouble();
			int idTx = (int) idTxd;
			params.put("idTx", idTx);
		}else{
			return null;
		}

		result = session.run("START t=node({idTx}) MATCH (t)<-[:TO]-(i:Input) RETURN i,ID(i)", params);

		int indexMap = 2;
		while(result.hasNext()){
			record = result.next();
			addToNodes(indexMap, record.get("i").asMap());
			double idInd = record.get("ID(i)").asDouble();
			int idIn = (int) idInd;
			addToIds("idIn"+(indexMap-1), idIn);
			indexMap++;
		}

		result = session.run("START t=node({idTx}) MATCH (t)<-[:TO]-(o:Output) RETURN o,ID(o)", params);

		// LA PARTE DE ABAJO, EN PRINCIPIO SE HACE CON EL METODO storeChangeOutput
		/*
		int indexOutHigher = 0; // index dentro de la lista donde se encuentra el output a devolver
		int indexObjOuts = 0; 
		double higherIndex = 0;  // index mas alto encontrado (el almacenado en hex)
		List<Map<String, Object>> outputs = new ArrayList<Map<String, Object>>();
		while(result.hasNext()){
			record = result.next();
			outputs.add(indexObjOuts, record.get("o").asMap());
			int indexTxOutInt = Integer.parseInt(record.get("o").get("indexTxOut").asString(),16);
			if(indexTxOutInt > higherIndex){
				higherIndex = indexTxOutInt;
				indexOutHigher = indexObjOuts;
			}
			outputs.add(indexObjOuts, record.get("o").asMap());
			indexObjOuts++;
		}
		if(indexObjOuts > 1){
			addToNodes(indexMap, outputs.get(indexOutHigher));
		}*/
		storeChangeOutput(result, session);


		return this;
	}

	private void storeChangeOutput(StatementResult result, Session session){
		// HAY QUE PENSAR QUE HACER CON ESTE TEMA SI EL NUMERO DE OUTPUTS ES MAYOR QUE 2
		List<Map<String, Object>> outputs = new ArrayList<Map<String, Object>>();
		Map<Integer, Integer> idsOutputs = new ArrayList<Integer, Integer>();
		
		Record record;
		int indexOutput = 1;
		while(result.hasNext()){
			record = result.next();
			outputs.add(indexOutput,record.get("o").asMap());
			double idOutd = record.get("ID(o)").asDouble();
			int idOut = (int) idOutd;
			idsOutputs.put(indexOutput, idOut);
			indexOutput++;
		}

		// Sacamos el numero de inputs y de outputs para realizar diferentes acciones dependiendo del caso
		int numberOutputs = outputs.size();
		int numberInputs = this.nodes.size()-2;
		if(numberInputs==1){
			if(numberOutputs == 1){
				// Abandonar este camino
				return;
			}else{
				// Seguimiento
			}
		}else{
			if(numberOutputs == 1){
				// Abandonar este camino
				return;
			}else if(numberOutputs == 2){
				// Buscar combinaciones
				combinationXIn2Out(outputs,session);
			}else{
				if(numberOutputs > 6){  // PENSAR ESTE VALOR (PARA CONSIDERAR POOLS)
					return;
				}else{
					// Seguimiento
				}
			}
		}

	}

	// Devuelve los satoshis de los outputs de una transacción
	private int getSatoshisOut(List<Map<String, Object>> outputs){
		int satoshisOut = 0;
		for (Map<String, Object> output : outputs){
			satoshisOut += output.get("valueSatoshis");
		} 
		return satoshisOut;
	}

	// Devuelve el valor de la propina en una transacción en satoshis
	private int getTransactionFee(int satoshisOut, Session session){
		int numberInputs = this.nodes.size()-2;
		int satoshisIn = 0;
		int idInput;
		for(int i=0; i<numberInputs; i++){
			idInput = this.ids.get("idIn"+(i+1));
			satoshisIn += getSatoshis(idInput, session);
		}
		return satoshisIn-satoshisOut;
	}

	// Devuelve el numero de satoshis que se estan gastando en un input determinado
	private int getSatoshis(int idInput, Session session){
		Map<String, Object> params = new HashMap<String, Object>();
		Statement result = session.run("START i=node({idInput}) MATCH (i)-[:ORIGIN_OUTPUT]->(o:Output) RETURN o", params);
		if(result.hasNext()){
			Record record = result.next();
			double satoshisD record.get("o").get("valueSatoshis").asDouble();
			int satoshis = (int) satoshisD;
			return satoshis;
		}else{
			return 0;
		}
	}

	// Sirve para las transacciones con más de un input y dos outputs.
	// Devuelve el indice de la lista de outputs que se le pasa. Ese indice señala al output cambio.
	private int combinationXIn2Out(List<Map<String, Object>> outputs, Session session){
		int numberInputs = this.nodes.size()-2;
		Map<Integer, Integer> inputsValues = new HashMap<Integer, Integer>();
		
		for(int i=0; i<numberInputs; i++){
			int shatoshisInput = getSatoshis(this.ids.get("idIn"+(i+1)),session);
			if((shatoshisInput>outputs.get(0).get("valueSatoshis")) && (shatoshisInput<outputs.get(1).get("valueSatoshis"))){
				return 0;
			}else if((shatoshisInput>outputs.get(1).get("valueSatoshis")) && (shatoshisInput<outputs.get(0).get("valueSatoshis"))){
				return 1;
			}else{
				int satoshisCombination = 0;
				for(int j=0; j<numberInputs; j++){
					if(i==j) continue;
					else{
						satoshisCombination += getSatoshis(this.ids.get("idIn"+(j+1)),session);
					}
				}
				if((satoshisCombination>outputs.get(0).get("valueSatoshis")) && (satoshisCombination<outputs.get(1).get("valueSatoshis"))){
					return 0;
				}else if((satoshisCombination<outputs.get(0).get("valueSatoshis")) && (satoshisCombination>outputs.get(1).get("valueSatoshis"))){
					return 1;
				}
			}
		}

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