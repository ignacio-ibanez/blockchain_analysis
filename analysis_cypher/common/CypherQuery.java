package common;

import java.util.*;
import org.neo4j.driver.v1.*;

//javac -cp neo4j-java-driver-1.0.6.jar common/CypherQuery.java -d classes

public class CypherQuery{

	public StatementResult getOriginBlockByTimeStamp(String timeStamp, Session session){
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("timeStamp",timeStamp);
		StatementResult result = session.run("OPTIONAL MATCH (b:Block)<-[:TO]-(t:Transaction)<-[:TO]-(o:Output) WHERE b.timeStamp={timeStamp} RETURN b,t,o,ID(t),ID(o) LIMIT 1",
					params);
		return result;
	}

	public StatementResult getOriginBlockByAddress(String address, Session session){
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("address",address);
		StatementResult result = session.run("OPTIONAL MATCH (b:Block)<-[:TO]-(t:Transaction)<-[:TO]-(o:Output) WHERE o.");
		return result;
	}

	public StatementResult getOriginBlockByHashBlock(String hashHeaderReduced, Session session){
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("hashHeaderReduced",hashHeaderReduced);
		StatementResult result = session.run("OPTIONAL MATCH (b:Block)<-[:TO]-(t:Transaction)<-[:TO]-(o:Output) WHERE b.hashHeaderReduced={hashHeaderReduced} RETURN b,t,o,ID(t),ID(o) LIMIT 1",
					params);
		return result;
	}

	public StatementResult getOriginBlockByTransaction(String hashTransactionReduced, Session session){
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("hashTransactionReduced",hashTransactionReduced);
		StatementResult result = session.run("OPTIONAL MATCH (b:Block)<-[:TO]-(t:Transaction)<-[:TO]-(o:Output) WHERE t.hashTransactionReduced={hashTransactionReduced} AND o.indexTxOut='00000000' RETURN b,t,o,ID(t),ID(o) LIMIT 1",
					params);
		return result;
	}

	public StatementResult getOriginBlockByTransactionWithIndex(String hashTransactionReduced, String indexOutput, Session session){
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("hashTransactionReduced",hashTransactionReduced);
		params.put("indexOutput",indexOutput);
		StatementResult result = session.run("OPTIONAL MATCH (b:Block)<-[:TO]-(t:Transaction)<-[:TO]-(o:Output) WHERE t.hashTransactionReduced={hashTransactionReduced} AND o.indexTxOut={indexOutput} RETURN b,t,o,ID(t),ID(o) LIMIT 1",
					params);
		return result;
	}

	public StatementResult getNextBlockFromOutput(int idOut, Session session){
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("idOut",idOut);
		StatementResult result = session.run("START o=node({idOut}) MATCH (o)<-[:ORIGIN_OUTPUT]-(i:Input)-[:TO]->(t:Transaction)-[:TO]->(b:Block) RETURN b,t,ID(t) LIMIT 1",
					params);
		return result;
	}

	public StatementResult getPreviousBlockFromInput(int idInput, Session session){
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("idInput",idInput);
		StatementResult result = session.run("START i=node({idInput}) MATCH (i)-[:ORIGIN_OUTPUT]->(o:Output)-[:TO]->(t:Transaction)-[:TO]->(b:Block) RETURN b,t,ID(t) LIMIT 1",
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

	public StatementResult getInputFromOutput(int idOutput, Session session){
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("idOutput",idOutput);
		StatementResult result = session.run("START o=node({idOutput}) MATCH (o)<-[:ORIGIN_OUTPUT]-(i:Input) RETURN i,ID(i)", params);
		return result;
	}
}