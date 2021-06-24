package MovieRecommender;

import java.util.List;

public class TestRecommender {

    // May have to change the localhost url
    public static MovieRecommender recommender = new MovieRecommender( "bolt://localhost:7687", "neo4j", "neo4j" );
    public static void main( String... args ) throws Exception
    {
        try 
        {
            List<Movie> recsByUser = recommender.getRecs("user", 1);
            for (Movie rec : recsByUser) {
                System.out.println(rec.getTitle() + ": " + Float.toString(rec.getSimilarity()));
            }

            List<Movie> recsByMovie = recommender.getRecs("movie", 1);
            for (Movie rec : recsByMovie) {
                System.out.println(rec.getTitle() + ": " + Float.toString(rec.getSimilarity()));
            }
        }
        finally {
            recommender.close();
        }
    }
}
