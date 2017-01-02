package common;

import java.util.*;
import org.neo4j.driver.v1.*;
import common.CypherQuery;

//javac -cp classes:neo4j-java-driver-1.0.6.jar common/BlockNodes.java -d classes

public class BlockNodes{
	private Map<String, Integer> ids = new HashMap<String, Integer>();
	private List<Map<String, Object>> nodes = new ArrayList<Map<String, Object>>();
	// FALTA METER LAS DIRECCIONES EN EL MAP DE ABAJO
	private Map<Integer, String> addressIdInput = new HashMap<Integer, String>();

	public BlockNodes getOriginNodes(String mode, Map<String,String> param, Session session){
		switch(mode){
			case "date":
			searchByTimeStamp(param,session);
			return this;

			case "address":
			searchByAddress(param,session);
			return this;

			case "block":
			searchByHashBlock(param,session);
			return this;

			case "transaction":
			searchByTransaction(param,session);
			return this;

			case "transactionWithIndex":
			searchByTransactionWithIndex(param,session);
			return this;

			case "transactionAllIndexes":
			searchByTransactionAllIndexes(param,session);
			return this;
		}
	}

	public void searchByTimeStamp(Map<String,String> timeStampMap, Session session){
		CypherQuery query = new CypherQuery();
		Record record;
		String timestamp = timeStampMap.get("timeStamp");
		StatementResult result = query.getOriginBlockByTimeStamp(timestamp,session);

		// Busca el bloque que tenga el timestamp más parecido al introducido por el usuario
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
				result = query.getOriginBlockByTimeStamp(timeStampModUp,session);
				up = false;
			}else{
				timeStampModDownInt = Integer.parseInt(timeStampModDown, 16) - 1;
				timeStampModDown = Integer.toHexString(timeStampModDownInt);
				result = query.getOriginBlockByTimeStamp(timeStampModDown,session);
				up = true;
			}
			record = result.next();
		}
		saveOriginNodes(record);
	}

	public void searchByAddress(Map<String,String> addressMap, Session session){
		CypherQuery query = new CypherQuery();
		Record record;
		StatementResult result = query.getOriginBlockByTimeStamp(timestamp,session);	
	}

	public void searchByHashBlock(Map<String,String> hashBlockMap, Session session){
		CypherQuery query = new CypherQuery();
		Record record;
		String hashBlock = hashBlockMap.get("hashBlock");
		StatementResult result = query.getOriginBlockByHashBlock(hashBlock,session);

		if(result.hasNext()){
			record = result.next();
			saveOriginNodes(record);
		}
	}

	public void searchByTransaction(Map<String,String> hashTransactionMap, Session session){
		CypherQuery query = new CypherQuery();
		Record record;
		String hashTransaction = hashTransactionMap.get("hashTransaction");
		StatementResult result = query.getOriginBlockByTransaction(hashTransaction,session);

		if(result.hasNext()){
			record = result.next();
			saveOriginNodes(record);
		}
	}

	public void searchByTransactionWithIndex(Map<String,String> hashTransactionMap, Session session){
		CypherQuery query = new CypherQuery();
		Record record;
		String hashTransaction = hashTransactionMap.get("hashTransaction");
		String indexOutput = hashTransactionMap.get("indexOutput");
		StatementResult result = query.getOriginBlockByTransactionWithIndex(hashTransaction,indexOutput,session);

		if(result.hasNext()){
			record = result.next();
			saveOriginNodes(record);
		}
	}

	public void searchByTransactionAllIndexes(Map<String,String> hashTransactionMap, Session session){
		CypherQuery query = new CypherQuery();
		Record record;
		String hashTransaction = hashTransactionMap.get("hashTransaction");

		if(result.hasNext()){
			// FALTA COMPLETAR
		}
	}

	public void saveOriginNodes(Record record){
		addToNodes(0,record.get("b").asMap());
		addToNodes(1, record.get("t").asMap());
		addToNodes(2, record.get("o").asMap());

		double idTxd = record.get("ID(t)").asDouble();
		int idTx = (int) idTxd;
		addToIds("idTx", idTx);

		double idOutd = record.get("ID(o)").asDouble();
		int idOut = (int) idOutd;
		addToIds("idOut", idOut);
	}

	public BlockNodes getIterationBlock(int idOutStart , Session session){
		StatementResult result;
		Record record;
		CypherQuery query = new CypherQuery();

		// Obtiene y almacena el nodo Block y el nodo Transaction del bloque siguiente 
		// utilizando el la relación ORIGIN_OUTPUT
		int idTx;
		result = query.getNextBlockFromOutput(idOutStart,session);
		if(result.hasNext()){
			record = result.next();
			addToNodes(0,record.get("b").asMap());
			addToNodes(1,record.get("t").asMap());
			double idTxd = record.get("ID(t)").asDouble();
			idTx = (int) idTxd;
		}else{
			return null;
		}

		// Obtiene y almacena los nodos Input de la transacción obtenida en la sentencia 
		// anterior. De los nodos saca y almacena las direcciones
		result = query.getInputsOfTx(idTx,session);
		int indexMap = 2;
		while(result.hasNext()){
			record = result.next();
			addToNodes(indexMap, record.get("i").asMap());
			double idInd = record.get("ID(i)").asDouble();
			int idIn = (int) idInd;
			addToIds("idIn"+(indexMap-1), idIn);
			try{
				addToAddressIdInput(idIn,getAddress(query.getOriginOutput(idIn,session).get("o").asMap()));
			}catch(Exception e){}
			indexMap++;
		}

		System.out.println("El numero de addresses guardadas es: " + addressIdInput.size());

		// Obtiene los outputs de la transacción.
		result = query.getOutputsOfTx(idTx,session);
		// Almacena los outputs obtenidos. No vale almacenar todos, ya que solo interesa el output
		// que representa el cambio en la transacción, ya que es el que corresponde con una
		// dirección del usuario. Todo esto se hace en storeChangeOutput
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
		
		// Almacena en la lista outputs los nodos Output pasados en la variable result
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
				// Abandonar este camino -----  Pensar si puede ser un mecanismo de Bitcoin
				// Solo hay un output, luego el pago es exacto y no hay cambio
				return;
			}else{
				// Seguimiento --- Pensar si es mejor hacerlo cuando haya almacenadas un número 
				// mayor de direcciones del usuario
				//
				// No hay forma de conocer cual es el cambio, así que se debe realizar
				// un seguimiento de todos los outputs hasta que se consuma un alcance,
				// "iterationFollow", o hasta que en alguno de ellos se detecte una 
				// conexión con otra de las direcciones almacenadas del usuario
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
				// Abandonar este camino -----  Pensar si puede ser un mecanismo de Bitcoin
				// Solo hay un output, luego el pago es exacto y no hay cambio
				return;
			}else if(numberOutputs == 2){
				// Buscar combinaciones
				// Al ser x inputs y 2 outputs, se pueden buscar combinaciones entre
				// valores de satoshis para encontrar el cambio
				int changeOutputIndex = combinationXIn2Out(outputs,session);
				if(changeOutputIndex == -1){
					for(int i=0; i<numberOutputs; i++){
						if(followOutput(idsOutputs.get(i), session, 3)){
							addToNodes(this.nodes.size(), outputs.get(i));
							addToIds("idOut", idsOutputs.get(i));
						}
					}
				}else{
					addToNodes(this.nodes.size(),outputs.get(changeOutputIndex));
					addToIds("idOut", idsOutputs.get(changeOutputIndex));
				}
			}else{
				if(numberOutputs > 6){  // PENSAR ESTE VALOR (PARA CONSIDERAR POOLS)
					return;
				}else{
					// Seguimiento --- Pensar si es mejor hacerlo cuando haya almacenadas un número 
					// mayor de direcciones del usuario
					//
					// No hay forma de conocer cual es el cambio, así que se debe realizar
					// un seguimiento de todos los outputs hasta que se consuma un alcance,
					// "iterationFollow", o hasta que en alguno de ellos se detecte una 
					// conexión con otra de las direcciones almacenadas del usuario
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

		if(iterationFollow == 0){
			return false;
		}

		CypherQuery query = new CypherQuery();
		Record record;

		// Obtiene el bloque siguiente partiendo del idOutput
		int idTx;
		StatementResult result = query.getNextBlockFromOutput(idOutput,session);
		if(result.hasNext()){
			record = result.next();
			double idTxd = record.get("ID(t)").asDouble();
			idTx = (int) idTxd;
		}else{
			return false;
		}

		// Obtiene los nodos Input de la transacción obtenida arriba.
		// Obtiene de cada Input la dirección y comprueba si se corresponde con
		// alguna de las ya almacenadas del usuario. Si es así, esa dirección
		// tambien es del usuario, y por tanto, el output es el cambio. 
		result = query.getInputsOfTx(idTx,session);
		int idOut = -1;
		while(result.hasNext()){
			record = result.next();
			double idInputd = record.get("ID(i)").asDouble();
			idInput = (int) idInputd;
			result = query.getOriginOutput(idInput,session);
			if(result.hasNext()){
				record = result.next();
				String address = getAddress(record.get("o").asMap());
				if(this.addressIdInput.values().contains(address)){
					return true;
				}
			}
		}

		// Si de los inputs de esta transacción no se encuentran coincidencias,
		// esto es, no hay ninguna direccion del usuario, se sigue buscando con los outputs
		// de la misma transacción.
		// Partiendo de los outputs, se inicia otra vez el proceso.
		// ------ PENSAR SI CON LOS OUTPUTS YA NO PUEDE APLICARSE, PORQUE PUEDE
		// ESTAR REALIZANDOSE UNA TRANSACCIÓN INVERSA DEL QUE RECIBIÓ AL QUE PAGÓ
		result = query.getOutputsOfTx(idTx,session);
		while(result.hasNext()){
			record.next();
			double idOutd = record.get("ID(o)").asDouble();
			int idOut = (int) idOutd;
			String address = getAddress(record.get("o").asMap());
			if(this.addressIdInput.values().contains(address)){
				return true;
			}
			if(followOutput(idOut, session, iterationFollow-1)){
				return true;
			}
		}

		return false;
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
			int transactionFee = getTransactionFee(valueSatoshis0+valueSatoshis1,session);
			if((shatoshisInput>valueSatoshis0+transactionFee) && (shatoshisInput<valueSatoshis1)){
				return 0;
			}else if((shatoshisInput>valueSatoshis1+transactionFee) && (shatoshisInput<valueSatoshis0)){
				return 1;
			}else{
				int satoshisCombination = 0;
				for(int j=0; j<numberInputs; j++){
					if(i==j) continue;
					else{
						satoshisCombination += getSatoshis(this.ids.get("idIn"+(j+1)),session);
					}
				}
				if((satoshisCombination>valueSatoshis0+transactionFee) && (satoshisCombination<valueSatoshis1)){
					return 0;
				}else if((satoshisCombination<valueSatoshis0) && (satoshisCombination>valueSatoshis1+transactionFee)){
					return 1;
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
