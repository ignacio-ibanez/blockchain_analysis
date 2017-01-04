package common;

import java.util.*;
import org.neo4j.driver.v1.*;
import common.CypherQuery;

//javac -cp classes:neo4j-java-driver-1.0.6.jar common/InitialBlock.java -d classes

public class InitialBlock{
	private Map<String, Integer> ids = new HashMap<String, Integer>();
	private List<Map<String, Object>> nodes = new ArrayList<Map<String, Object>>();

	public InitialBlock getOriginNodes(String mode, Map<String,String> param, Session session){
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