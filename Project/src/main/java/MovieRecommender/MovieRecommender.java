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

import java.util.ArrayList;
import java.util.List;

// Movie Recommending API that connects to the Neo4J database and generates recommendations based on similarity functions
public class MovieRecommender implements AutoCloseable
{
    private final Driver driver;

    // Constructor
    public MovieRecommender( String uri, String user, String password )
    {
        driver = GraphDatabase.driver( uri, AuthTokens.basic( user, password ) );
    }

    // Closing the driver connection
    @Override
    public void close() throws Exception
    {
        driver.close();
    }

/*
    // Creates a new user in the neo4j database
    private void addUser(String uid)
    {
        try (Session session = driver.session())
        {
            // Use `session.writeTransaction` for writes and `session.readTransaction` for reading data.
            session.writeTransaction(tx -> tx.run("MERGE (a:User {uid: $x})", parameters("x", uid)));
        }
    }
*/

    // Dispatches based on how the recommendation will be generated (type)
    private void getRecs(String type, String text)
    {
        if (type == "user") {
            this.recsByUsers(text);
        }
        else if (type == "movie") {
            this.recsByMovies(text);
        }
    }

    // Uses the Pearson Similarity function to quantify the users with the highest similarity to the given user 
    // in order to generate movie recommendations
    private List<Movie> recsByUsers(String user)
    {
        try (Session session = driver.session())
        {
            // The `session.run` method will run the specified Query.
            Result result = session.run(
                    "MATCH (u1:User {userId:$uid})-[r:RATED]->(m:Movie)" + 
                    "WITH u1, avg(toFloat(r.rating)) AS u1_mean" + 
                     
                    "MATCH (u1)-[r1:RATED]->(m:Movie)<-[r2:RATED]-(u2)" + 
                    "WITH u1, u1_mean, u2, COLLECT({r1: r1, r2: r2}) AS ratings WHERE size(ratings) > 10" + 
                     
                    "MATCH (u2)-[r:RATED]->(m:Movie)" + 
                    "WITH u1, u1_mean, u2, avg(toFLoat(r.rating)) AS u2_mean, ratings" + 
                     
                    "UNWIND ratings AS r" + 
                     
                    "WITH sum( (toFloat(r.r1.rating)-u1_mean) * (toFloat(r.r2.rating)-u2_mean) ) AS nom," +
                         "sqrt( sum( (toFloat(r.r1.rating) - u1_mean)^2) * sum( (toFloat(r.r2.rating) - u2_mean) ^2)) AS denom," +
                         "u1, u2 WHERE denom <> 0" +
                     
                    "WITH u1, u2, nom/denom AS pearson" +
                    "ORDER BY pearson DESC LIMIT 10" +
                     
                    "MATCH (u2)-[r:RATED]->(m:Movie) WHERE NOT EXISTS( (u1)-[:RATED]->(m) )" +
                     
                    "RETURN m.title, SUM( pearson * toFloat(r.rating)) AS score" +
                    "ORDER BY score DESC LIMIT 25",
                    parameters("uid", user));

            List<Movie> recs = new ArrayList<Movie>();

            // Each Cypher execution returns a stream of records.
            while (result.hasNext())
            {
                Record record = result.next();
                Movie rec = new Movie(Integer.parseInt(record.get("movieId").asString()), record.get("title").asString());
                recs.add(rec);
                // Values can be extracted from a record by index or name.
                System.out.println(rec.getTitle() + ": " + record.get("score").asString());
            }

            return recs;
        }
    }
    // Uses the Cosine Similarity function to quantify the movies with the highest similarity to the given movie 
    private List<Movie> recsByMovies(String movie)
    {
        try (Session session = driver.session())
        {
            Result result = session.run(
                    "MATCH (p1:Movie {movieId: $mid})-[x:TAGGED_AS]->(movie)<-[x2:TAGGED_AS]-(p2:Movie)" +
                    "WHERE p2 <> p1 and toFloat(x.relevance) > 0.5" +
                    "WITH p1, p2, collect(toFLoat(x.relevance)) AS p1Relevance, collect(toFloat(x2.relevance)) AS p2Relevance" + 
                    "RETURN p1.title AS from," +
                           "p2.title AS to," +
                           "gds.alpha.similarity.cosine(p1Relevance, p2Relevance) AS similarity" + 
                    "ORDER BY similarity DESC",
                    parameters("mid", movie));

            List<Movie> recs = new ArrayList<Movie>();

            while (result.hasNext())
            {
                Record record = result.next();
                Movie rec = new Movie(Integer.parseInt(record.get("movieId").asString()), record.get("title").asString());
                recs.add(rec);
                System.out.println(rec.getTitle() + ": " + record.get("similarity").asString());
            }

            return recs;
        }
    }

    // Find the given movie and return a Movie object
    private void findMovie(String title)
    {
        try (Session session = driver.session())
        {
            Result result = session.run("MATCH (m:Movie {title:$x}) RETURN m",
                    parameters("x", title));

            while (result.hasNext())
            {
                Record record = result.next();
                System.out.println(record.get("title").asString());
            }
        }
    }

    // Find the given user and return a User object
    private void findUser(String username)
    {
        try (Session session = driver.session())
        {
            Result result = session.run("MATCH (u:User {username:$x}) RETURN u",
                    parameters("x", username));

            while (result.hasNext())
            {
                Record record = result.next();
                System.out.println(record.get("username").asString());
            }
        }
    }

    public static void main( String... args ) throws Exception
    {
        // May have to change the localhost url
        try ( MovieRecommender recommender = new MovieRecommender( "bolt://localhost:7687", "neo4j", "neo4j" ) )
        {
            recommender.getRecs("user", "1" );
            recommender.getRecs("movie", "1");
            recommender.close();
        }
    }
}
