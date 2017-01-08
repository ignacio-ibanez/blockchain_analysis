package analysis_1;

import org.neo4j.driver.v1.*;
import java.util.*;
import common.*;

//javac -cp neo4j-java-driver-1.0.6.jar analysis_1.java
//java -cp .:neo4j-java-driver-1.0.6.jar Execute
//javac -cp classes:neo4j-java-driver-1.0.6.jar analysis_1/analysis_1.java -d classes
//java -cp ./classes:neo4j-java-driver-1.0.6.jar analysis_1.Execute

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

	public static void main (String[] args){

		Driver driver = GraphDatabase.driver( "bolt://localhost:7687", AuthTokens.basic( "neo4j", "123456" ) );
		Session session = driver.session();

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
		String mode = "transactionWithIndex";
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
		String hashTransaction = "4f829f34a47c5967a437e556a0456cd72b6ef2bf9f4fe0222242cf64dd5b6ceb";
		hashTransaction = hashTransaction.substring(hashTransaction.length()-7,hashTransaction.length());
		String indexOutput = "00000000";
		initialParam.put("hashTransaction",hashTransaction);
		initialParam.put("indexOutput",indexOutput);
		// ------------------------------------
		// Para probar buscando desde transacción y flag para analizar todos los outputs -> "transactionAllIndexes"
		// Falta por hacer

		// ------------------------------------
		int scope = 2;
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
