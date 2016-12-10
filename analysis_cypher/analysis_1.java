import org.neo4j.driver.v1.*;
import java.util.*;

//javac -cp neo4j-java-driver-1.0.6.jar analysis_1.java
//java -cp .:neo4j-java-driver-1.0.6.jar Execute

// 1ª PRUEBA
//1 - START o=node(586) MATCH (o)-[r:TO]->(t:Transaction)-[r2:TO]->(b:Block) RETURN o,r,t,r2,b
//2 - START o=node(586) MATCH (o)<-[r:ORIGIN_OUTPUT]-(i:Input)-[r2:TO]->(t:Transaction)-[r3:TO]->(b:Block) RETURN o,r,i,r2,t,b,r3
//3 - START t=node(359882) MATCH (t)<-[r:TO]-(i:Input) RETURN t,r,i
//4 - START t=node(359882) MATCH (t)<-[r:TO]-(o:Output) RETURN t,r,o

// 2ª PRUEBA
//1 - ID(t):

class Execute {

	public static void main (String[] args){

		Driver driver = GraphDatabase.driver( "bolt://localhost:7687", AuthTokens.basic( "neo4j", "123456" ) );
		Session session = driver.session();

		List<BlockNodes> blocksAnalysed = new ArrayList<BlockNodes>();
		List<Map<String, String>> origin = new ArrayList<Map<String, String>>();

		Map<String, Object> originBlockNode = new HashMap<String, Object>();

		//String timeStamp = "49696ef7";
		String timeStamp = "496aee56";
		int scope = 1;
		BlockNodes originBlock = new BlockNodes();
		originBlockNode = originBlock.getOriginNodes(timeStamp, session).getNodes().get(0);
		blocksAnalysed.add(0, originBlock);
		/*System.out.println("El nodo origen ha sido guardado");
		System.out.println("El hash del bloque origen es: " + originBlockNode.get("hashHeader"));
		System.out.println("El hash de la transaccion origen es: " + originBlock.getOriginNodes(timeStamp, session).getNodes().get(1).get("hashTransaction"));
		System.out.println("El indexTxOut del output es: " + originBlock.getOriginNodes(timeStamp, session).getNodes().get(2).get("indexTxOut"));
		System.out.println("El scriptLength del output es: " + originBlock.getOriginNodes(timeStamp, session).getNodes().get(2).get("scriptLength"));
		System.out.println("El id de la transaccion es: " + originBlock.getOriginNodes(timeStamp, session).getIds().get("idTx"));
		System.out.println("El id del output es: " + originBlock.getOriginNodes(timeStamp, session).getIds().get("idOut"));
		System.out.println("");*/

		// Obtención de los siguientes bloques en el seguimiento hacia adelante
		//int idOut = originBlock.getIds().get("idOut");   //--- SE PONE EL DE ABAJO PARA PRUEBAS, LUEGO DESCOMENTAR
		// PRUEBA
		int idOut = 586;    // ---- Contiene [:ORIGIN_OUTPUT]-(input)
		//int idInnput = 359884
		for(int i=1; i<=scope; i++){
			BlockNodes nodesBlock = new BlockNodes();
			if(nodesBlock.getIterationBlock(idOut, session) == null){
				break;
			}else{
				blocksAnalysed.add(i, nodesBlock);
			}
			/*int numberNodes = blocksAnalysed.get(i).getNodes().size();
			System.out.println("Número de nodos: " + numberNodes);
			System.out.println("El hash del nodo bloque es: " + blocksAnalysed.get(i).getNodes().get(0).get("hashHeader"));
			System.out.println("El hash de la transaccion es: " + blocksAnalysed.get(i).getNodes().get(1).get("hashTransaction"));
			System.out.println("El indexTxOut del output es: " + blocksAnalysed.get(i).getNodes().get(numberNodes-1).get("indexTxOut"));
			System.out.println("El script del primer input es: " + blocksAnalysed.get(i).getNodes().get(2).get("script"));*/
		}


		// Obtención de los bloques anteriores en el seguimiento hacia atras
		//for(int i=1; i<blocksAnalysed.size(); i++){

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
	// FALTA METER LAS DIRECCIONES EN EL MAP DE ABAJO
	private Map<Integer, String> addressIdInput = new HashMap<Integer, String>();

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
		StatementResult result;
		Record record;
		CypherQuery query = new CypherQuery();

		int idTx;
		result = query.getNextBlockFromOutput(idOut,session);
		if(result.hasNext()){
			record = result.next();
			addToNodes(0,record.get("b").asMap());
			addToNodes(1,record.get("t").asMap());
			double idTxd = record.get("ID(t)").asDouble();
			idTx = (int) idTxd;
		}else{
			return null;
		}

		result = query.getInputsOfTx(idTx,session);
		int indexMap = 2;
		while(result.hasNext()){
			record = result.next();
			addToNodes(indexMap, record.get("i").asMap());
			double idInd = record.get("ID(i)").asDouble();
			int idIn = (int) idInd;
			addToIds("idIn"+(indexMap-1), idIn);
			try{
				addToAddressIdInput(idIn,getAddress(getOriginOutput(idIn,session)));
			}catch(Exception e){}
			indexMap++;
		}

		System.out.println("El numero de addresses guardadas es: " + addressIdInput.size());

		result = query.getOutputsOfTx(idTx,session);
		storeChangeOutput(result,session);

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

		return this;
	}

	private void storeChangeOutput(StatementResult result, Session session){
		List<Map<String, Object>> outputs = new ArrayList<Map<String, Object>>();
		Map<Integer, Integer> idsOutputs = new HashMap<Integer, Integer>();
		
		Record record;
		int indexOutput = 0;
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
		System.out.println("El número de outputs en storeChange es: " + numberOutputs);
		System.out.println("El número de inputs en storeChange es: " + numberInputs);
		if(numberInputs==1){
			if(numberOutputs == 1){
				// Abandonar este camino
				return;
			}else{
				// Seguimiento
				System.out.println("Entra aqui porque hay varios outs y 1 input");
				for(int i=0; i<numberOutputs; i++){
					if(followOutput(idsOutputs.get(i), session, 10)){
						addToNodes(this.nodes.size(), outputs.get(i));
						addToIds("idOut", idsOutputs.get(i));
					}
				}
			}
		}else{
			if(numberOutputs == 1){
				// Abandonar este camino
				return;
			}else if(numberOutputs == 2){
				// Buscar combinaciones
				int changeOutputIndex = combinationXIn2Out(outputs,session);
				if(changeOutputIndex == -1){
					for(int i=0; i<numberOutputs; i++){
						if(followOutput(idsOutputs.get(i), session, 10)){
							addToNodes(this.nodes.size(), outputs.get(i));
							addToIds("idOut", idsOutputs.get(i));
						}
					}
				}
				addToNodes(this.nodes.size(),outputs.get(changeOutputIndex));
				addToIds("idOut", idsOutputs.get(changeOutputIndex));
			}else{
				if(numberOutputs > 6){  // PENSAR ESTE VALOR (PARA CONSIDERAR POOLS)
					return;
				}else{
					// Seguimiento
					for(int i=0; i<numberOutputs; i++){
						if(followOutput(idsOutputs.get(i), session, 10)){
							addToNodes(this.nodes.size(), outputs.get(i));
							addToIds("idOut", idsOutputs.get(i));
						}
					}
				}
			}
		}

	}

	private boolean followOutput(int idOutput, Session session, int iterationFollow){
		Map<String, Object> params = new HashMap<String, Object>();
		Map<Integer, String> candidateAddressIdOutput = new HashMap<Integer, String>();

		if(iterationFollow == 0 || idOutput == -1){
			return false;
		}

		CypherQuery query = new CypherQuery();
		Record record;

		int idTx;
		result = query.getNextBlockFromOutput(idOutput,session);
		if(result.hasNext()){
			record = result.next();
			double idTxd = record.get("ID(t)").asDouble();
			idTx = (int) idTxd;
		}else{
			return false;
		}

		result = query.getOutputsOfTx(idTx,session);
		int idOut = -1;
		while(result.hasNext()){
			record = result.next();
			double idOutd = record.get("ID(o)").asDouble();
			idOut = (int) idOutd;
			String address = getAddress(record.get("o").asMap());
			if(addressIdInput.values().contains(address)){
				return true;
			}
		}

		return followOutput(idOut, session, iterationFollow-1);
	} 

	private String getAddress(Map<String, Object> output){
		double scriptLengthD = Double.parseDouble(output.get("scriptLength").toString());
		int scriptLength = (int) scriptLengthD;
		String script = output.get("lockingScript").toString();
		// pensar como tratar si en el mismo seguimiento se ven dos tipos diferentes de addresses
		switch (scriptLength){
			case 25:
				if(script.substring(46,47) == "88"){
					return script.substring(5,46);
				}else{
					return script.substring(5);
				}
				
			case 67:
				return script.substring(2,130);

			case 66:
				return script.substring(0,128);

			default:
				return null;
		}
	}

	// Devuelve los satoshis de los outputs de una transacción
	private int getSatoshisOut(List<Map<String, Object>> outputs){
		int satoshisOut = 0;
		for (Map<String, Object> output : outputs){
			double satoshisOutD = Double.parseDouble(output.get("valueSatoshis").toString());
			satoshisOut += (int) satoshisOutD;
		} 
		return satoshisOut;
	}

	// Devuelve el valor de la propina en una transacción en satoshis de esta transacción
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
		CypherQuery query = new CypherQuery();

		StatementResult result = query.getOriginOutput(idInput,session);
		if(result.hasNext()){
			Record record = result.next();
			double satoshisD = record.get("o").get("valueSatoshis").asDouble();
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
			int valueSatoshis0 = Integer.parseInt(outputs.get(0).get("valueSatoshis").toString());
			int valueSatoshis1 = Integer.parseInt(outputs.get(1).get("valueSatoshis").toString());
			if((shatoshisInput>valueSatoshis0) && (shatoshisInput<valueSatoshis1)){
				return 0;
			}else if((shatoshisInput>valueSatoshis1) && (shatoshisInput<valueSatoshis0)){
				return 1;
			}else{
				int satoshisCombination = 0;
				for(int j=0; j<numberInputs; j++){
					if(i==j) continue;
					else{
						satoshisCombination += getSatoshis(this.ids.get("idIn"+(j+1)),session);
					}
				}
				if((satoshisCombination>valueSatoshis0) && (satoshisCombination<valueSatoshis1)){
					return 0;
				}else if((satoshisCombination<valueSatoshis0) && (satoshisCombination>valueSatoshis1)){
					return 1;
				}else{
					return -1;
				}
			}
		}
		return -1;
	}

	private Map<String, Object> getOriginOutput(int idInput, Session session){
		Map<String, Object> params = new HashMap<String, Object>();
		CypherQuery query = new CypherQuery();
		StatementResult result = query.getOriginOutput(idInput,session);
		if(result.hasNext()){
			Record record = result.next();
			return record.get("o").asMap();
		}else{
			return null;
		}
	}

	public void addToNodes(int position, Map<String, Object> newNode){
		this.nodes.add(position, newNode);
	}

	public void addToIds(String key, int value){
		this.ids.put(key, value);
	}

	public void addToAddressIdInput(int idInput, String address){
		this.addressIdInput.put(idInput,address);
	}

	public List<Map<String, Object>> getNodes(){
		return this.nodes;
	}

	public Map<String, Integer> getIds(){
		return this.ids;
	}
}

class CypherQuery{

	public StatementResult getNextBlockFromOutput(int idOut, Session session){
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("idOut",idOut);
		StatementResult result = session.run("START o=node({idOut}) MATCH (o)<-[:ORIGIN_OUTPUT]-(i:Input)-[:TO]->(t:Transaction)-[:TO]->(b:Block) RETURN b,t,ID(t) LIMIT 1",
					params);
		return result;
	}

	public StatementResult getInputsOfTx(int idTx, Session session){
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("idTx",idTx);
		StatementResult result = session.run("START t=node({idTx}) MATCH (t)<-[:TO]-(i:Input) RETURN i,ID(i)", params);
		return result;
	}

	public StatementResult getOutputsOfTx(int idTx, Session session){
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("idTx",idTx);
		StatementResult result = session.run("START t=node({idTx}) MATCH (t)<-[:TO]-(o:Output) RETURN o,ID(o)", params);
		return result;
	}

	public StatementResult getOriginOutput(int idInput, Session session){
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("idInput",idInput);
		StatementResult result = session.run("START i=node({idInput}) MATCH (i)-[:ORIGIN_OUTPUT]->(o:Output) RETURN o", params);
		return result;
	}

}