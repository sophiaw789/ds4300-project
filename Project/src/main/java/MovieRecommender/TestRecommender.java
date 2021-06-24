package MovieRecommender;

public class TestRecommender {

    // May have to change the localhost url
    public static MovieRecommender recommender = new MovieRecommender( "bolt://localhost:7687", "neo4j", "neo4j" );
    public static void main( String... args ) throws Exception
    {
        try 
        {
            recommender.getRecs("user", "1" );
            recommender.getRecs("movie", "1");
        }
        finally {
            recommender.close();
        }
    }
}
