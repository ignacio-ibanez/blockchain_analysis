package common;

import java.util.*;
import org.neo4j.driver.v1.*;
import common.CypherQuery;

//javac -cp classes:neo4j-java-driver-1.0.6.jar common/BlockNodes.java -d classes

public class BlockNodes{
	private Map<String, Integer> ids = new HashMap<String, Integer>();
	private List<Map<String, Object>> nodes = new ArrayList<Map<String, Object>>();
	private Map<Integer, String> addressesUser;
	// FALTA METER LAS DIRECCIONES EN EL MAP DE ABAJO
	private Map<Integer, String> addressIdInput = new HashMap<Integer, String>();

	public BlockNodes(Map<Integer, String> addressesUser){
		this.addressesUser = addressesUser;
	}

	public BlockNodes analyzeNextBlock(int idOutStart , Session session){
		StatementResult result;
		Record record;
		CypherQuery query = new CypherQuery();

		System.out.println("El id del output origen en analyzeNextBlock es: " + idOutStart);

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

		System.out.println("El id de la transacción en analyzeNextBlock es: " + idTx);

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
				addToAddressIdInput(idIn,getAddress(getOriginOutput(idIn,session)));
			}catch(Exception e){}
			indexMap++;
		}

		System.out.println("El numero de addresses guardadas es: " + addressIdInput.size());

		// Obtiene los outputs de la transacción.
		result = query.getOutputsOfTx(idTx,session);
		List<Map<String, Object>> outputs = new ArrayList<Map<String, Object>>();
		Map<Integer, Integer> idsOutputs = new HashMap<Integer, Integer>();
		// Almacena en la lista outputs los nodos Output y comprueba si alguno de ellos
		// tiene de dirección alguna de las ya almacenadas.
		// Solo interesa el output que representa el cambio en la transacción, 
		// ya que es el que corresponde con una
		// dirección del usuario. 
		int indexVariableOutput = 0;
		while(result.hasNext()){
			record = result.next();
			outputs.add(indexVariableOutput,record.get("o").asMap());
			double idOutd = record.get("ID(o)").asDouble();
			int idOut = (int) idOutd;
			System.out.println("Nuevo output de la transacción, con id: " + idOut);
			idsOutputs.put(indexVariableOutput, idOut);
			String address = getAddress(record.get("o").asMap());
			if(addressesUser.values().contains(address) || addressIdInput.values().contains(address)){
				System.out.println("Encontrado el output cambio, ya que se paga a una direccion ya almacenada");
				addToNodes(this.nodes.size(), record.get("o").asMap());
				addToIds("idOut", idOut);
				return this;
			}
			indexVariableOutput++;
		}

		System.out.println("En este punto se va a llamar a findChangeOutput");

		// Comprueba que se debe hacer para encontrar el output cambio, si es que hay cambio
		int changeWay = findChangeOutput(outputs,idsOutputs,session);
		System.out.println("changeWay debería ser 2 y es: " + changeWay);
		// changeWay puede ser: 0->abandonar; 1->seguimiento ; 2->combinaciones
		switch(changeWay){
			case 0:
				break;

			case 1:
				System.out.println("Debería entrar aquí para empezar el seguimiento");
				for(int i=0; i<outputs.size(); i++){
					if(followOutput(idsOutputs.get(i), session, 2)){
						addToNodes(this.nodes.size(), outputs.get(i));
						addToIds("idOut", idsOutputs.get(i));
						int idInput = getIdNextInput(idsOutputs.get(i),session);
						if(idInput != (-1)){
							addToAddressIdInput(idInput,getAddress(outputs.get(i))); 
						}
						break;
					}
				}
				break;

			case 2:
				int changeOutputIndex = combinationXIn2Out(outputs,session);
				addToNodes(this.nodes.size(),outputs.get(changeOutputIndex));
				addToIds("idOut", idsOutputs.get(changeOutputIndex));
		}

		return this;
	}

	// DE MOMENTO NO SE USA. SI DA TIEMPO, PENSAR UTILIDAD
	public BlockNodes analyzePreviousBlock(int idInStart, Session session){
		StatementResult result;
		Record record;
		CypherQuery query = new CypherQuery();

		// Extrae desde el input el bloque anterior
		int idTx;
		result = query.getPreviousBlockFromInput(idInStart,session);
		if(result.hasNext()){
			record = result.next();
			//addToNodes(0,record.get("b").asMap());
			//addToNodes(1,record.get("t").asMap());
			double idTxd = record.get("ID(t)").asDouble();
			idTx = (int) idTxd;
		}else{
			return null;
		}

		// Obtiene todos los outputs de la transacción. De ellos hay que ver si el que se corresponde con nuestro 
		// input origen es el del cambio. Si es así, todos los input de esta transacción son del usuario
		result = query.getOutputsOfTx(idTx,session);
		List<Map<String, Object>> outputs = new ArrayList<Map<String, Object>>();
		Map<Integer, Integer> idsOutputs = new HashMap<Integer, Integer>();
		// Almacena en la lista outputs los nodos Output y comprueba si alguno de ellos
		// tiene de dirección alguna de las ya almacenadas.
		int indexOutput = 0;
		while(result.hasNext()){
			record = result.next();
			outputs.add(indexOutput,record.get("o").asMap());
			double idOutd = record.get("ID(o)").asDouble();
			int idOut = (int) idOutd;
			idsOutputs.put(indexOutput, idOut);
			String address = getAddress(record.get("o").asMap());
			if(addressesUser.values().contains(address) || addressIdInput.values().contains(address)){
				addToNodes(this.nodes.size(), record.get("o").asMap());
				addToIds("idOut", idOut);
				return this;
			}
			indexOutput++;
		}
		
		//findChangeOutput(result,session);
		return this;
	}

	private int findChangeOutput(List<Map<String, Object>> outputs, Map<Integer, Integer> idsOutputs, Session session){

		// Sacamos el numero de inputs y de outputs para realizar diferentes acciones dependiendo del caso
		int numberOutputs = outputs.size();
		int numberInputs = this.nodes.size()-2;
		System.out.println("El número de outputs en findChangeOutput es: " + numberOutputs);
		System.out.println("El número de inputs en findChangeOutput es: " + numberInputs);
		if(numberInputs==1){
			if(numberOutputs == 1){
				// Abandonar este camino 
				return 0;
			}else{
				// Seguimiento 
				//
				// No hay forma de conocer cual es el cambio, así que se debe realizar
				// un seguimiento de todos los outputs hasta que se consuma un alcance,
				// "iterationFollow", o hasta que en alguno de ellos se detecte una 
				// conexión con otra de las direcciones almacenadas del usuario
				return 1;
			}
		}else{
			if(numberOutputs == 1){
				// Abandonar este camino 
				return 0;
			}else if(numberOutputs == 2){
				// Buscar combinaciones
				// Al ser x inputs y 2 outputs, se pueden buscar combinaciones entre
				// valores de satoshis para encontrar el cambio
				return 2;
			}else{
				if(numberOutputs > 6){  // PENSAR ESTE VALOR (PARA CONSIDERAR POOLS)
					return 0;
				}else{
					// Seguimiento 
					//
					// No hay forma de conocer cual es el cambio, así que se debe realizar
					// un seguimiento de todos los outputs hasta que se consuma un alcance,
					// "iterationFollow", o hasta que en alguno de ellos se detecte una 
					// conexión con otra de las direcciones almacenadas del usuario
					return 1;
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

		System.out.println("El valor de iterationFollow es: " + iterationFollow);
		System.out.println("El id del output al que se hace seguimiento es: " + idOutput);

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

		System.out.println("Obtenida la transacción en followOutput");

		// Obtiene los nodos Input de la transacción obtenida arriba.
		// Obtiene de cada Input la dirección y comprueba si se corresponde con
		// alguna de las ya almacenadas del usuario. Si es así, esa dirección
		// tambien es del usuario, y por tanto, el output es el cambio. 
		result = query.getInputsOfTx(idTx,session);
		int idOut = -1;
		while(result.hasNext()){
			record = result.next();
			double idInputd = record.get("ID(i)").asDouble();
			int idInput = (int) idInputd;
			result = query.getOriginOutput(idInput,session);
			if(result.hasNext()){
				record = result.next();
				String address = getAddress(record.get("o").asMap());
				if(addressesUser.values().contains(address) || addressIdInput.values().contains(address)){
					System.out.println("Obtenida dirección del usuario en el seguimiento");
					return true;
				}
			}
		}

		// Si de los inputs de esta transacción no se encuentran coincidencias,
		// esto es, no hay ninguna direccion del usuario, se sigue buscando con los outputs
		// de la misma transacción.
		// Si en los outputs de esta transacción se puede encontrar cual es el cambio, se realiza el 
		// seguimiento sobre ese output.
		// Partiendo de los outputs, se inicia otra vez el proceso.

		System.out.println("Para continuar el seguimiento, se debe intentar encontrar el cambio");

		List<Map<String, Object>> outputsTx = new ArrayList<Map<String, Object>>();
		Map<Integer, Integer> idsOutputsTx = new HashMap<Integer, Integer>();
		result = query.getOutputsOfTx(idTx,session);
		int indexVariableOutput = 0;
		while(result.hasNext()){
			record = result.next();
			outputsTx.add(indexVariableOutput,record.get("o").asMap());
			double idOutd = record.get("ID(o)").asDouble();
			idOut = (int) idOutd;
			idsOutputsTx.put(indexVariableOutput, idOut);
			String address = getAddress(record.get("o").asMap());
			indexVariableOutput++;
		}
		int changeWay = findChangeOutput(outputsTx,idsOutputsTx,session);
		System.out.println("El valor de changeWay en el seguimiento es: " + changeWay);
		if(changeWay==2){
			// combinaciones para encontrar el output cambio
			int changeOutputIndex = combinationXIn2Out(outputsTx,session);
			// output cambio: outputsTx.get(changeOutputIndex)
			String addressChangeOutput = getAddress(outputsTx.get(changeOutputIndex));
			if(addressesUser.values().contains(addressChangeOutput) || addressIdInput.values().contains(addressChangeOutput)){
				return true;
			}
			if(followOutput(idsOutputsTx.get(changeOutputIndex), session, iterationFollow-1)){
				return true;
			}
		}
		else if(changeWay==1){
			return false;
		}

		System.out.println("No se ha encontrado nada");
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

	// Devuelve los satoshis de los outputs de una transacción (los satoshis de la entrada menos la propina)
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
			//System.out.println(record.get("o").asMap());
			Map<String,Object> output = new HashMap<String,Object>();
			output = record.get("o").asMap();
			String satoshisStr = output.get("valueSatoshis").toString();
			int satoshis = Integer.parseInt(satoshisStr.substring(0,satoshisStr.length()-1));
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
		System.out.println("Entra en combinationXIn2Out");
		
		for(int i=0; i<numberInputs; i++){
			int shatoshisInput = getSatoshis(this.ids.get("idIn"+(i+1)),session);
			int valueSatoshis0 = 0;
			int valueSatoshis1 = 0;
			String valueSatoshis0Str = outputs.get(0).get("valueSatoshis").toString();
			String valueSatoshis1Str = outputs.get(1).get("valueSatoshis").toString();
			valueSatoshis0 = Integer.parseInt(valueSatoshis0Str.substring(0,valueSatoshis0Str.length()-1));
			valueSatoshis1 = Integer.parseInt(valueSatoshis1Str.substring(0,valueSatoshis0Str.length()-1));
			System.out.println("Bitcoins del output 0: " + valueSatoshis0 + ". Bitcoins del output 1: " + valueSatoshis1);
			int transactionFee = getTransactionFee(valueSatoshis0+valueSatoshis1,session);
			System.out.println("La propina es: " + transactionFee);
			if((shatoshisInput>valueSatoshis0+transactionFee) && (shatoshisInput<valueSatoshis1)){
				System.out.println("El índice 0 es el output de cambio");
				return 0;
			}else if((shatoshisInput>valueSatoshis1+transactionFee) && (shatoshisInput<valueSatoshis0)){
				System.out.println("El índice 1 es el output de cambio");
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

	private int getIdNextInput(int idOutput, Session session){
		Map<String, Object> params = new HashMap<String, Object>();
		CypherQuery query = new CypherQuery();
		StatementResult result = query.getInputFromOutput(idOutput,session);
		if(result.hasNext()){
			Record record = result.next();
			double idInd = record.get("ID(i)").asDouble();
			int idIn = (int) idInd;
			return idIn;
		}else{
			return -1;
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

	public Map<Integer, String> getAddresses(){
		return this.addressIdInput;
	}
}
