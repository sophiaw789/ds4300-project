package MovieRecommender;

// Movie object class
public final class Movie {
    private int movieId;
    private String title;
    private float similarity;

    public Movie(int movieId, String title, float similarity) {
        this.movieId = movieId;
        this.title = title;
        this.similarity = similarity;
    }

    public int getMovieId() {
        return movieId;
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public float getSimilarity() {
        return similarity;
    }

    public void setSimilarity(float similarity) {
        this.similarity = similarity;
    }
}

