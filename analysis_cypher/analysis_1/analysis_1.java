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

class Execute {

	public static void main (String[] args){

		Driver driver = GraphDatabase.driver( "bolt://localhost:7687", AuthTokens.basic( "neo4j", "123456" ) );
		Session session = driver.session();

		// Bloques analizados de los que se extrae luego la información
		List<BlockNodes> blocksAnalysed = new ArrayList<BlockNodes>();
		// Nodos del bloque origen
		List<Map<String, String>> origin = new ArrayList<Map<String, String>>();
		// Nodo Block del bloque origen
		Map<String, Object> originBlockNode = new HashMap<String, Object>();
		// Mapa de parametros para obtener el bloque inicial
		Map<String, String> initialParam = new HashMap<String, String>();

		// Mode debe ser date, address, block, transaction, transactionWithIndex, transactionAllIndexes
		String mode = "";
		// Para probar buscando desde timeStamp
		String timeStamp = "496ab951";
		initialParam.put("timeStamp",timeStamp);
		// ------------------------------------
		// Para probar buscando desde direccion
		// FALTA POR HACER
		// ------------------------------------
		// Para probar buscando desde hash de bloque
		String hashHeader = "";
		initialParam.put("hashBlock",hashHeader);
		// ------------------------------------
		// Para probar buscando desde transacción
		String hashTransaction = "";
		initialParam.put("hashTransaction",hashTransaction);
		// ------------------------------------
		// Para probar buscando desde transacción con indice output
		String hashTransaction = "";
		String indexOutput = "";
		initialParam.put("hashTransaction",hashTransaction);
		initialParam.put("indexOutput",indexOutput);
		// ------------------------------------
		// Para probar buscando desde transacción y flag para analizar todos los outputs
		// Falta por hacer
		// ------------------------------------
		int scope = 1;
		BlockNodes originBlock = new BlockNodes();
		originBlock.getOriginNodes(mode,initialParam,session);
		originBlockNode = originBlock.getNodes().get(0);
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
		int idOut = originBlock.getIds().get("idOut");   //--- SE PONE EL DE ABAJO PARA PRUEBAS, LUEGO DESCOMENTAR
		// PRUEBA
		//int idOut = 446;    // ---- Contiene [:ORIGIN_OUTPUT]-(input)
		//int idInnput = 359884
		Map<Integer, String> addressIdInput = new HashMap<Integer, String>();
		for(int i=1; i<=scope; i++){
			BlockNodes nodesBlock = new BlockNodes(addressIdInput);
			if(nodesBlock.getIterationBlock(idOut, session) == null){
				break;
			}else{
				blocksAnalysed.add(i, nodesBlock);
				while(nodesBlock.getAddresses().hasNext()){
					Map.Entry pair = (Map.Entry)nodesBlock.getAddresses().next();
					addressIdInput.put(pair.getKey(),pair.getValue());
				}
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
