package MovieRecommender;

import java.util.List;

public class TestRecommender {

    // May have to change the localhost url
    public static MovieRecommender recommender = new MovieRecommender( "bolt://localhost:7687", "neo4j", "ds_4300" );
    public static void main( String... args ) throws Exception
    {
        try 
        {
            // finds a movie by title
            Movie movieTest = recommender.findMovieByTitle("Inception");
            System.out.println(movieTest.getTitle());

            // recommends a movie based on a user
            // List<Movie> recsByUser = recommender.getRecs("user", 1);
            // for (Movie rec : recsByUser) {
            //     System.out.println(rec.getTitle() + ": " + Float.toString(rec.getSimilarity()));
            // }

            // recommends movies based on a movie
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
