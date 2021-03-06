package analysis_1;

import org.neo4j.driver.v1.*;
import java.util.*;
import common.*;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Iterator;
import org.json.simple.*;
import org.json.simple.parser.*;

//javac -cp neo4j-java-driver-1.0.6.jar analysis_1.java
//java -cp .:neo4j-java-driver-1.0.6.jar Execute
//javac -cp classes:neo4j-java-driver-1.0.6.jar:json-simple-1.1.1.jar analysis_1/analysis_1.java -d classes
//java -cp ./classes:neo4j-java-driver-1.0.6.jar:json-simple-1.1.1.jar analysis_1.Execute

// 1ª PRUEBA
//1 - START o=node(586) MATCH (o)-[r:TO]->(t:Transaction)-[r2:TO]->(b:Block) RETURN o,r,t,r2,b
//2 - START o=node(586) MATCH (o)<-[r:ORIGIN_OUTPUT]-(i:Input)-[r2:TO]->(t:Transaction)-[r3:TO]->(b:Block) RETURN o,r,i,r2,t,b,r3
//3 - START t=node(359882) MATCH (t)<-[r:TO]-(i:Input) RETURN t,r,i
//4 - START t=node(359882) MATCH (t)<-[r:TO]-(o:Output) RETURN t,r,o

// 2ª PRUEBA
// ID(t): 408 -> Transaccion con 2 outputs y 1 input
// ID(input): 409 -> Input de la anterior transaccion
// ID(t): 360 -> Transaccion origen
// ID(b): 356, b.timeStamp=496ab951 -> Bloque origen
//1 - 

// PRUEBA
/*
MATCH (t:Transaction) WHERE t.outputCount=2 AND t.inputCount=2 AND ID(t)>10000 RETURN t LIMIT 10

MATCH (b2:Block)<-[r3:TO]-(t2:Transaction)<-[r4:TO]-(o:Output)<-[r2:ORIGIN_OUTPUT]-(i:Input)-[r:TO]->(t:Transaction)-[r5:TO]->(b:Block) 
WHERE ID(t)=190707 RETURN i,o,r2,r3,t2,r4,b2,r5,b,t

MATCH (n)-[r:TO]->(t:Transaction) WHERE ID(t)=11211 RETURN n,r,t

*/

class Execute {

	@SuppressWarnings("unchecked")
	public static void main (String[] args){

		String mode = "";
		String date = "";
		String hashHeader = "";
		String hashTransaction = "";
		String indexOutput = "";
		//String scope = "0";
		boolean correctParams = false;

		Driver driver = GraphDatabase.driver( "bolt://localhost:7687", AuthTokens.basic( "neo4j", "123456" ) );
		Session session = driver.session();

		String pathFileConfig = "/home/ignacio/appNodeTFG/configuration_files/analysis_1_config.json";
		String pathFileResults = "/home/ignacio/appNodeTFG/configuration_files/analysis_1_results.json";
		JSONParser parser = new JSONParser();
		try{
			Object obj = parser.parse(new FileReader(pathFileConfig));

			JSONObject jsonObject = (JSONObject) obj;

			//String scopeJSON = (String) jsonObject.get("scope");
			//if(scopeJSON != null){
			//	scope = scopeJSON;
			//}

			mode = (String) jsonObject.get("mode");
			switch(mode){
				case "date":
					date = (String) jsonObject.get("date");
					break;
				case "address":
					break;
				case "block":
					hashHeader = (String) jsonObject.get("hashHeader");
					break;
				case "transaction":
					hashTransaction = (String) jsonObject.get("hashTransaction");
					break;
				case "transactionWithIndex":
					hashTransaction = (String) jsonObject.get("hashTransaction");
					indexOutput = (String) jsonObject.get("indexOutput");
					break;
				default:
					break;
			}
			correctParams = true;
		}catch (Exception e){
			correctParams = false;
		}

		if(correctParams){
			System.out.println("Mode: " + mode);
			System.out.println("date: " + date);
		}

		//String timeStamp = ; ------------- FALTA PASARLO DESDE DATE A TIMESTAMP

		// Bloques analizados de los que se extrae luego la información
		List<BlockNodes> blocksAnalysed = new ArrayList<BlockNodes>();
		// Nodos del bloque origen
		List<Map<String, String>> origin = new ArrayList<Map<String, String>>();
		// Nodo Block del bloque origen
		//Map<String, Object> originBlockNode = new HashMap<String, Object>();
		// Mapa de parametros para obtener el bloque inicial
		Map<String, String> initialParam = new HashMap<String, String>();

		// PENSAR SI AÑADIR EN EL MODELO UN FLAG QUE INDIQUE SI ES LA TRANSACCIÓN RECOMPENSA
		// Mode debe ser date, address, block, transaction, transactionWithIndex, transactionAllIndexes
		//String mode = "transactionWithIndex";
		// Falta añadir en el modo "date" para que coja la transacción recompensa
		// Para probar buscando desde timeStamp -> "date"
		//String timeStamp = "496aee57";
		//initialParam.put("timeStamp",timeStamp);
		// ------------------------------------
		// Para probar buscando desde direccion -> "address"
		// FALTA POR HACER

		// ------------------------------------
		// Para probar buscando desde hash de bloque -> "block"
		//String hashHeader = "00000000b2cde2159116889837ecf300bd77d229d49b138c55366b54626e495d";
		//hashHeader = hashHeader.substring(hashHeader.length()-7,hashHeader.length());
		//initialParam.put("hashBlock",hashHeader);
		// ------------------------------------
		// Para probar buscando desde transacción -> "transaction"
		//String hashTransaction = "4385fcf8b14497d0659adccfe06ae7e38e0b5dc95ff8a13d7c62035994a0cd79";
		//hashTransaction = hashTransaction.substring(hashTransaction.length()-7,hashTransaction.length());
		//initialParam.put("hashTransaction",hashTransaction);
		// ------------------------------------
		// Para probar buscando desde transacción con indice output -> "transactionWithIndex"
		//hashTransaction = "a87e31b0e252fecc4a487e054fbcbd2545ea8a110747ef875a59b2e3780101db";
		//hashTransaction = hashTransaction.substring(hashTransaction.length()-7,hashTransaction.length());
		//indexOutput = "00000001";
		//initialParam.put("hashTransaction",hashTransaction);
		//initialParam.put("indexOutput",indexOutput);
		// ------------------------------------
		// Para probar buscando desde transacción y flag para analizar todos los outputs -> "transactionAllIndexes"
		// Falta por hacer

		// ------------------------------------
		initialParam.put("timeStamp",date);
		int scope = 1;
		boolean analysisFinished = true;
		InitialBlock originBlock = new InitialBlock();
		// Comprobar lo que devuelve getOriginNodes antes de seguir
		originBlock.getOriginNodes(mode,initialParam,session);
		//originBlockNode = originBlock.getNodes().get(0);
		// blocksAnalysed.add(0, originBlock); --- Igual es mejor no meter el bloque origen en blocksAnalysed

		/*System.out.println("El nodo origen ha sido guardado");
		System.out.println("El hash del bloque origen es: " + originBlock.getNodes().get(0).get("hashHeader"));
		System.out.println("El hash de la transaccion origen es: " + originBlock.getNodes().get(1).get("hashTransaction"));
		System.out.println("El indexTxOut del output es: " + originBlock.getNodes().get(2).get("indexTxOut"));
		System.out.println("El scriptLength del output es: " + originBlock.getNodes().get(2).get("scriptLength"));
		System.out.println("El id de la transaccion es: " + originBlock.getIds().get("idTx"));*/
		System.out.println("El id del output origen es: " + originBlock.getIds().get("idOut"));
		System.out.println("");

		// Obtención de los siguientes bloques en el seguimiento hacia adelante
		int idOut = originBlock.getIds().get("idOut");   //--- SE PONE EL DE ABAJO PARA PRUEBAS, LUEGO DESCOMENTAR
		// PRUEBA
		//int idOut = 446;    // ---- Contiene [:ORIGIN_OUTPUT]-(input)
		//int idInnput = 359884
		Map<Integer, String> addressesUser = new HashMap<Integer, String>();
		for(int i=0; i<scope; i++){
			BlockNodes nodesBlock = new BlockNodes(addressesUser);
			if(nodesBlock.analyzeNextBlock(idOut, session) == null){
				System.out.println("El nuevo bloque no se conecta con un nuevo bloque");
				break;
			}else{
				blocksAnalysed.add(i, nodesBlock);
				System.out.println("El bloque ha sido analizado");
				System.out.println("Debería entrar aquí cuando el output se haya gastado");
				// comprobar que si no hay un output dentro de las direcciones se pare la iteración
				for(Map.Entry<Integer, String> entry : nodesBlock.getAddresses().entrySet()){
					System.out.println("El id del input es: " + entry.getKey() + ". La dirección es: " + entry.getValue());
					addressesUser.put(entry.getKey(),entry.getValue());
				}
				try{
					idOut = nodesBlock.getIds().get("idOut");
				}catch(java.lang.NullPointerException e){
					analysisFinished = false;  // PENSAR QUE HACER CON EL ANÁLISIS CUANDO NO SE HA PODIDO REALIZAR LAS ITERA.
					break;
				}
			}
		}

		System.out.println("");
		System.out.println("FIN DEL ANÁLISIS");
		System.out.println("");
		int numeroBloques = blocksAnalysed.size();
		System.out.println("Número de bloques analizados: " + numeroBloques);
		
		session.close();
		driver.close();

		// ----------------------------------------
		// GUARDADO DE LOS RESULTADOS EN EL FICHERO
		// ----------------------------------------

		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		Map<String, Object> node = new HashMap<String, Object>();
		JSONObject objW = new JSONObject();

		objW.put("end", "true");

		JSONArray blocks = new JSONArray();
		JSONObject block = new JSONObject();
		JSONObject transaction = new JSONObject();
		JSONArray inputs = new JSONArray();
		JSONArray outputs = new JSONArray();
		JSONObject input = new JSONObject();
		JSONObject output = new JSONObject();

		results = originBlock.getNodes();
		block.put("origin", "true");
		node = results.get(0);
		block.put("hashHeader", node.get("hashHeader"));
		block.put("date", node.get("timeStamp"));
		block.put("transactionsCount", node.get("transactionsCount"));

		node = results.get(1);
		transaction.put("hashTransaction", node.get("hashTransaction"));
		transaction.put("inputCount", node.get("inputCount"));
		transaction.put("outputCount", node.get("outputCount"));
		block.put("transaction", transaction);

		block.put("inputs", inputs);

		node = results.get(2);
		output.put("lockingScript", node.get("lockingScript"));
		output.put("indexTxOut", node.get("indexTxOut"));
		output.put("valueSatoshis", node.get("valueSatoshis"));
		output.put("changeOut", "true");

		outputs.add(output);
		block.put("outputs", outputs);

		blocks.add(block);
		
		List<JSONObject> blockList = new ArrayList<JSONObject>();
		List<JSONObject> transactionList = new ArrayList<JSONObject>();
		List<JSONArray> inputsArrayList = new ArrayList<JSONArray>();
		List<JSONArray> outputsArrayList = new ArrayList<JSONArray>();
		List<JSONObject> inputList = new ArrayList<JSONObject>();
		List<JSONObject> outputList = new ArrayList<JSONObject>();
		for (int i=0; i<blocksAnalysed.size(); i++){
			results = blocksAnalysed.get(i).getNodes();
			blockList.add(new JSONObject()); 
			transactionList.add(new JSONObject());
			inputsArrayList.add(new JSONArray());
			outputsArrayList.add(new JSONArray());

			blockList.get(i).put("origin", "false");
			node = results.get(0);
			blockList.get(i).put("hashHeader", node.get("hashHeader"));
			blockList.get(i).put("date", node.get("timeStamp"));
			blockList.get(i).put("transactionsCount", node.get("transactionsCount"));

			node = results.get(1);
			transactionList.get(i).put("hashTransaction", node.get("hashTransaction"));
			transactionList.get(i).put("inputCount", node.get("inputCount"));
			transactionList.get(i).put("outputCount", node.get("outputCount"));
			blockList.get(i).put("transaction", transactionList.get(i));

			for(int j=2; j<results.size()-1; j++){
				inputList.add(new JSONObject());
				node = results.get(j);
				inputList.get(j-2).put("hashPreviousTransaction", node.get("hashPreviousTransaction"));
				inputList.get(j-2).put("indexPreviousTxout", node.get("indexPreviousTxout"));
				inputsArrayList.get(i).add(inputList.get(j-2));
				blockList.get(i).put("inputs",inputsArrayList.get(i));
			}

			outputList.add(new JSONObject());
			node = results.get(results.size()-1);
			outputList.get(i).put("lockingScript", node.get("lockingScript"));
			outputList.get(i).put("indexTxOut", node.get("indexTxOut"));
			outputList.get(i).put("valueSatoshis", node.get("valueSatoshis"));
			outputList.get(i).put("changeOut", "true");

			outputsArrayList.get(i).add(outputList.get(i));
			blockList.get(i).put("outputs",outputsArrayList.get(i));

			blocks.add(blockList.get(i));
		}
	
		try(FileWriter file = new FileWriter(pathFileResults)){
			file.write(blocks.toJSONString());
			System.out.println("Successfully Copied JSON Object to File...");
		}catch(Exception e){}
	
	}

}
