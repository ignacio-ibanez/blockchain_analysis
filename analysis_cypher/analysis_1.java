import org.neo4j.driver.v1.*;

//javac -cp neo4j-java-driver-1.0.6.jar analysis_1.java
//java -cp .:neo4j-java-driver-1.0.6.jar execute

class execute {
	
	public static void main (String[] args){
		System.out.println("prueba");
		Driver driver = GraphDatabase.driver( "bolt://localhost:7687", AuthTokens.basic( "neo4j", "123456" ) );
		Session session = driver.session();

		StatementResult result = session.run( "MATCH (b:Block) WITH COUNT(b) as sumB RETURN sumB" );
		System.out.println(result.next().get( "sumB" ));

		session.close();
		driver.close();
	}
}