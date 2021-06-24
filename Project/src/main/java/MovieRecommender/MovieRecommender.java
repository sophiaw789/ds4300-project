package MovieRecommender;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
//import org.neo4j.driver.Transaction;
//import org.neo4j.driver.TransactionWork;

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
    public List<Movie> getRecs(String type, int id)
    {
        if (type == "user") {
            return this.recsByUsers(id);
        }
        else if (type == "movie") {
            return this.recsByMovies(id);
        }
        return null;
    }

    // Uses the Pearson Similarity function to quantify the users with the highest similarity to the given user 
    // in order to generate movie recommendations
    private List<Movie> recsByUsers(int user)
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
                    parameters("uid", Integer.toString(user)));

            List<Movie> recs = new ArrayList<Movie>();

            // Each Cypher execution returns a stream of records.
            while (result.hasNext())
            {
                Record record = result.next();
                Movie rec = new Movie(Integer.parseInt(record.get("movieId").asString()), 
                        record.get("title").asString(), Float.parseFloat(record.get("score").asString()));
                recs.add(rec);
                // Values can be extracted from a record by index or name.
                //System.out.println(rec.getTitle() + ": " + record.get("score").asString());
            }

            return recs;
        }
    }
    // Uses the Cosine Similarity function to quantify the movies with the highest similarity to the given movie 
    private List<Movie> recsByMovies(int movie)
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
                    parameters("mid", Integer.toString(movie)));

            List<Movie> recs = new ArrayList<Movie>();

            while (result.hasNext())
            {
                Record record = result.next();
                Movie rec = new Movie(Integer.parseInt(record.get("movieId").asString()), 
                        record.get("title").asString(), Float.parseFloat(record.get("similarity").asString()));
                recs.add(rec);
                //System.out.println(rec.getTitle() + ": " + record.get("similarity").asString());
            }

            return recs;
        }
    }

    // Find the movie by the given title and return a Movie object
    private Movie findMovieByTitle(String title)
    {
        try (Session session = driver.session())
        {
            Result result = session.run("MATCH (m:Movie {title:$x}) RETURN m",
                    parameters("x", title));

            if (result.hasNext())
            {
                Record record = result.next();
                Movie rec = new Movie(Integer.parseInt(record.get("movieId").asString()), record.get("title").asString(), (float) 0.0);
                //System.out.println(rec.getTitle());
                return rec;
            }
            return null;
        }
    }

    // Find the movie by the given movieId and return a Movie object
    private Movie findMovieById(int mid)
    {
        try (Session session = driver.session())
        {
            Result result = session.run("MATCH (m:Movie {title:$x}) RETURN m",
                    parameters("x", Integer.toString(mid)));

            if (result.hasNext())
            {
                Record record = result.next();
                Movie rec = new Movie(Integer.parseInt(record.get("movieId").asString()), record.get("title").asString(), (float) 0.0);
                //System.out.println(rec.getTitle());
                return rec;
            }
            return null;
        }
    }

    // Find the user with the given username and return a User object
    // Do users have a username property? Or is it only uid?
    private User findUserByName(String username)
    {
        try (Session session = driver.session())
        {
            Result result = session.run("MATCH (u:User {username:$x}) RETURN u",
                    parameters("x", username));

            if (result.hasNext())
            {
                Record record = result.next();
                User user = new User(Integer.parseInt(record.get("userId").asString()), record.get("username").asString());
                //System.out.println(user.getUsername());
                return user;
            }
            return null;
        }
    }

    // Find the user with the given userId and return a User object
    private User findUserById(int uid)
    {
        try (Session session = driver.session())
        {
            Result result = session.run("MATCH (u:User {userId:$x}) RETURN u",
                    parameters("x", Integer.toString(uid)));

            if (result.hasNext())
            {
                Record record = result.next();
                User user = new User(Integer.parseInt(record.get("userId").asString()), record.get("username").asString());
                //System.out.println(user.getUserId());
                return user;
            }
            return null;
        }
    }
}
