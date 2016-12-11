package common;

import java.util.*;
import org.neo4j.driver.v1.*;

//javac -cp neo4j-java-driver-1.0.6.jar common/CypherQuery.java -d classes

public class CypherQuery{

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