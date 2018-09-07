package com.bighero2.comovies;

import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.bighero2.comovies.util.CustomAdapter;
import com.bighero2.comovies.util.State;
import com.bighero2.comovies.view.CustomFrameLayout;

import java.util.ArrayList;
import java.util.List;

public class PlayMovieActivity extends AppCompatActivity {
    private static final String EXTERNAL_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
    private static final String KEY_VIDEO_IDX = "video index";
    private static final String KEY_CURRENT_TIME = "current time";
    private static final String KEY_IS_PLAYING = "playing or not";

    private CustomFrameLayout mContainer;
    private int mPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_custom_movie);

        FrameLayout container = (FrameLayout) findViewById(R.id.frame_container_all);
        mContainer = (CustomFrameLayout) container;

        if (savedInstanceState == null) {
            ArrayList<String> movieList = getListVideo();
            mContainer.initialize(this, getOrientation(), movieList, mPosition, null);
        } else {
            // Get current time and status of playing (play or not)
            int currTime = savedInstanceState.getInt(KEY_CURRENT_TIME);
            boolean isPlaying = savedInstanceState.getBoolean(KEY_IS_PLAYING);
            State state = new State(currTime, isPlaying);

            int idxVideo = savedInstanceState.getInt(KEY_VIDEO_IDX);
            mContainer.initialize(this, getOrientation(), getListVideo(), idxVideo, state);
        }


    }

    private int getOrientation() {
        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        int orientation = display.getRotation();
        return orientation;
    }

    private ArrayList<String> getListVideo() {
        Intent intent = getIntent();
        String[] movies = intent.getStringArrayExtra(CustomAdapter.EXTRA_MOVIES);
        mPosition = intent.getIntExtra(CustomAdapter.EXTRA_POSITION, 0);

        ArrayList<String> mListVideo = new ArrayList<>();
        for (int idx = 0; idx < movies.length; idx++) {
            mListVideo.add(movies[idx]);
        }

        return mListVideo;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_VIDEO_IDX, mContainer.getVideoIdx());
        outState.putInt(KEY_CURRENT_TIME, mContainer.getCurrentTime());
        outState.putBoolean(KEY_IS_PLAYING, mContainer.getPlaying());

        super.onSaveInstanceState(outState);
    }
}
