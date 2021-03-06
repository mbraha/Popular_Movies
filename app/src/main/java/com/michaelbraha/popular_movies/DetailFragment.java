package com.michaelbraha.popular_movies;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.like.LikeButton;
import com.like.OnLikeListener;
import com.michaelbraha.popular_movies.adapters.ReviewAdapter;
import com.michaelbraha.popular_movies.adapters.TrailerAdapter;
import com.michaelbraha.popular_movies.data.DatabaseHandler;
import com.michaelbraha.popular_movies.helpers.RecyclerItemClickListener;
import com.michaelbraha.popular_movies.helpers.SimpleDividerItemDecoration;
import com.michaelbraha.popular_movies.objects.MovieItem;
import com.michaelbraha.popular_movies.objects.Review;
import com.michaelbraha.popular_movies.objects.Trailer;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class DetailFragment extends Fragment {

    RecyclerView rvTrailers;
    Trailer trailer = new Trailer();
    ArrayList<Trailer> trailers;

    RecyclerView rvReviews;
    Review review = new Review();
    ArrayList<Review> reviews;

    private Context mContext;

    public DatabaseHandler db;
    public DetailFragment(){

    }


    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = getContext();
        db = new DatabaseHandler(mContext);
        Configuration screenConfig = getResources().getConfiguration();
        final View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        final MovieItem receivedMovieItem = getActivity().getIntent().getParcelableExtra("MovieItemParcel");
        new TrailerTask().execute(receivedMovieItem);
        new ReviewTask().execute(receivedMovieItem);


        ((TextView) rootView.findViewById(R.id.movie_title_textview)).setText(receivedMovieItem.getTitle());

        ImageView posterImageView = (ImageView) rootView.findViewById(R.id.movie_poster_imageview);
        Picasso.with(getContext()).load(receivedMovieItem.getImage()).into(posterImageView);
        if (screenConfig.orientation == 2) {
            posterImageView.getLayoutParams().width = ((int) getResources().getDimension(R.dimen.detail_image_width)) / 2;
            posterImageView.getLayoutParams().height = ((int) getResources().getDimension(R.dimen.detail_image_height)) / 2;
        }

        ((TextView) rootView.findViewById(R.id.year_release_date_textview)).setText(receivedMovieItem.getReleaseDate());

        String voteText = receivedMovieItem.getVote() + getString(R.string.over_10);
        ((TextView) rootView.findViewById(R.id.vote_textview)).setText(voteText);

        TextView overviewTextView = (TextView) rootView.findViewById(R.id.overview_textview);
        overviewTextView.setText(receivedMovieItem.getOverview());
        overviewTextView.setMovementMethod(new ScrollingMovementMethod());

        rvTrailers = (RecyclerView) rootView.findViewById(R.id.trailer_view);
        LinearLayoutManager trailerLLM = new LinearLayoutManager(getContext());
        trailerLLM.setOrientation(1);
        rvTrailers.setLayoutManager(trailerLLM);
        // Sets the divider
        rvTrailers.addItemDecoration(new SimpleDividerItemDecoration(getContext()));
        rvTrailers.setNestedScrollingEnabled(false);

        rvReviews = (RecyclerView) rootView.findViewById(R.id.review_view);
        LinearLayoutManager reviewLLM = new LinearLayoutManager(getContext());
        reviewLLM.setOrientation(1);
        rvReviews.setLayoutManager(reviewLLM);
        rvReviews.addItemDecoration(new SimpleDividerItemDecoration(getContext()));
        rvReviews.setNestedScrollingEnabled(false);

        LikeButton favoriteButton = (LikeButton) rootView.findViewById(R.id.favorite_button);
        boolean movieAlreadyLiked = db.checkIfDataExists(receivedMovieItem.getTitle());
        if (movieAlreadyLiked) {
            favoriteButton.setLiked(true);
        }
        else {
            favoriteButton.setLiked(false);
        }
        favoriteButton.setOnLikeListener(new OnLikeListener(){
            @Override
            public void liked(LikeButton favoriteButton) {
                db.addFavorite(receivedMovieItem);
            }
            @Override
            public void unLiked(LikeButton favoriteButton) {
                db.deleteFavorite(receivedMovieItem.getTitle());
            }
        });
        return rootView;
    }

    public class TrailerTask extends AsyncTask<MovieItem, Void, String[]> {
        private final String LOG_TAG = TrailerTask.class.getSimpleName();

        protected String[] doInBackground(MovieItem... params) {

            if (params.length == 0) {
                return null;
            }

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String id = BuildConfig.THE_MOVIE_DATABASE_API_KEY;
            String movieId = params[0].getMovieId();
            String apiResult;

            try {
                String MOVIE_BASE_URL = "http://api.themoviedb.org/3/movie";
                String VIDEOS = "videos";
                String ID_PARAM = "api_key";

                Uri builtUri;
                builtUri = Uri.parse(MOVIE_BASE_URL).buildUpon()
                        .appendPath(movieId)
                        .appendPath(VIDEOS)
                        .appendQueryParameter(ID_PARAM, id)
                        .build();

                URL url = new URL(builtUri.toString());
                /**
                 * Uncomment for goodies in log
                 */
//                Log.d(LOG_TAG, url.toString());

                // Create the request to TheMovieDatabase, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;

                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    apiResult = null;
                }

                apiResult = buffer.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error", e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            /**
             * Uncomment for goodies in log
             */
//            Log.d(LOG_TAG, apiResult);

            String[] youtubeURLs = createYoutubeURLsFromJSON(apiResult);
            if (youtubeURLs != null) {
                trailers = Trailer.createTrailerList(youtubeURLs.length);
                trailer.setURLs(youtubeURLs);
                for (int i = 0; i < youtubeURLs.length; i++) {
                    trailers.get(i).setIndividualURL(trailer.getSpecificURL(i));
                }
            }
            return youtubeURLs;
        }

        private String[] createYoutubeURLsFromJSON(String result) {
            String TMDB_RESULTS = "results";
            String TMDB_KEY = "key";
            String[] videoKeysArray = null;
            try {
                JSONArray resultsArray = new JSONObject(result).getJSONArray(TMDB_RESULTS);
                int numVideos = resultsArray.length();
                videoKeysArray = new String[numVideos];
                for (int i = 0; i < numVideos; i++) {
                    videoKeysArray[i] = resultsArray.getJSONObject(i).getString(TMDB_KEY);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return createYoutubeURLs(videoKeysArray);
        }

        private String[] createYoutubeURLs(String[] trailerKeys) {
            int len = trailerKeys.length;
            String BASE_YOUTUBE_URL = "http://www.youtube.com/watch?v=";
            String[] urlStrings = new String[len];
            for (int i = 0; i < len; i++) {
                urlStrings[i] = BASE_YOUTUBE_URL + trailerKeys[i];
            }
            return urlStrings;
        }

        protected void onPostExecute(String[] youtubeURLs) {
                rvTrailers.setAdapter(new TrailerAdapter(trailers));
                rvTrailers.addOnItemTouchListener(
                        new RecyclerItemClickListener(getContext(), new RecyclerItemClickListener.OnItemClickListener() {
                            public void onItemClick(View view, int position) {
                                boolean isIntentSafe = false;
                                Intent videoIntent = new Intent("android.intent.action.VIEW", Uri
                                        .parse(trailers.get(position).getIndividualURL()));
                                if (getContext().getPackageManager().queryIntentActivities(videoIntent, 0).size() > 0) {
                                    isIntentSafe = true;
                                }
                                if (isIntentSafe) {
                                    startActivity(videoIntent);
                                } else {
                                    Log.d("LOOOOK", "Intent isn't safe");
                                }
                            }
                        })
                );
        }
    }

    public class ReviewTask extends AsyncTask<MovieItem, Void, String[]>{
        private final String LOG_TAG = ReviewTask.class.getSimpleName();

        @Override
        protected String[] doInBackground(MovieItem... params) {
            if (params.length == 0) {
                return null;
            }

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String id = BuildConfig.THE_MOVIE_DATABASE_API_KEY;
            String movieId = params[0].getMovieId();
            String apiResult;

            try {
                String MOVIE_BASE_URL = "http://api.themoviedb.org/3/movie";
                String REVIEWS = "reviews";
                String ID_PARAM = "api_key";

                Uri builtUri;
                builtUri = Uri.parse(MOVIE_BASE_URL).buildUpon()
                        .appendPath(movieId)
                        .appendPath(REVIEWS)
                        .appendQueryParameter(ID_PARAM, id)
                        .build();

                URL url = new URL(builtUri.toString());
                /**
                 * Uncomment for goodies in log
                 */
//                Log.d(LOG_TAG, url.toString());

                // Create the request to TheMovieDatabase, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuilder builder = new StringBuilder();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;

                while ((line = reader.readLine()) != null) {
                    builder.append(line + "\n");
                }

                if (builder.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    apiResult = null;
                }

                apiResult = builder.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error", e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            /**
             * Uncomment for goodies in log
             */
//            Log.d(LOG_TAG, apiResult);
            String[] reviewResults = createReviewsFromJSON(apiResult);
            if (reviewResults != null) {
                reviews = Review.createReviewList(reviewResults.length);
                review.setReviews(reviewResults);
                for (int i = 0; i < reviewResults.length; i++) {
                    reviews.set(i, review);
                }
            }
            return reviewResults;
        }

        private String[] createReviewsFromJSON(String result){
            String TMDB_RESULTS = "results";
            String TMDB_AUTHOR = "author";
            String TMDB_CONTENT = "content";

            String[] reviewsArray = null;

            try {
                JSONArray resultsArray = new JSONObject(result).getJSONArray(TMDB_RESULTS);
                int numReviews = resultsArray.length();
                reviewsArray = new String[numReviews];

                for (int i = 0; i < numReviews; i++) {
                    String author = resultsArray.getJSONObject(i).getString(TMDB_AUTHOR);
                    String review = resultsArray.getJSONObject(i).getString(TMDB_CONTENT);
                    reviewsArray[i] = author + "*" + review;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return reviewsArray;
        }

        protected void onPostExecute (String[] reviewResults){
                rvReviews.setAdapter(new ReviewAdapter(reviews));
        }
    }
}