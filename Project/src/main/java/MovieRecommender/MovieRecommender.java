package MovieRecommender;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.TransactionWork;

import static org.neo4j.driver.Values.parameters;

public class MovieRecommender implements AutoCloseable
{
    private final Driver driver;

    public MovieRecommender( String uri, String user, String password )
    {
        driver = GraphDatabase.driver( uri, AuthTokens.basic( user, password ) );
    }

    @Override
    public void close() throws Exception
    {
        driver.close();
    }

    private void addUser(String uid)
    {
        try (Session session = driver.session())
        {
            // Use `session.writeTransaction` for writes and `session.readTransaction` for reading data.
            session.writeTransaction(tx -> tx.run("MERGE (a:User {uid: $x})", parameters("x", uid)));
        }
    }

    private void getRecs(String type, String text)
    {
        if (type == "user") {
            this.recsByUsers(text);
        }
        else if (type == "movie") {
            this.recsByMovies(text);
        }
    }

    private void recsByUsers(String user)
    {
        try (Session session = driver.session())
        {
            // The `session.run` method will run the specified Query.
            Result result = session.run(
                    // Where the Cypher query goes
                    "MATCH (a:User) WHERE a.uid = $x RETURN a.uid AS User",
                    parameters("x", user));
            // Each Cypher execution returns a stream of records.
            while (result.hasNext())
            {
                Record record = result.next();
                // Values can be extracted from a record by index or name.
                System.out.println(record.get("User").asString());
            }
        }
    }

    private void recsByMovies(String movie)
    {
        try (Session session = driver.session())
        {
            Result result = session.run(
                    "MATCH (m:Movie) WHERE m.title STARTS WITH $x RETURN m.title AS title",
                    parameters("x", movie));

            while (result.hasNext())
            {
                Record record = result.next();
                // Values can be extracted from a record by index or name.
                System.out.println(record.get("title").asString());
            }
        }
    }

    public static void main( String... args ) throws Exception
    {
        try ( MovieRecommender recommender = new MovieRecommender( "bolt://localhost:7687", "neo4j", "neo4j" ) )
        {
            recommender.getRecs("user", "Vevey" );
        }
    }
}
