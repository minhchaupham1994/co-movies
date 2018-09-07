package com.bighero2.comovies.garbage;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.Image;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.bighero2.comovies.R;

import java.util.ArrayList;

public class PlayMovieActivityOld extends AppCompatActivity {
    private static final String EXTERNAL_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();

    private ArrayList<String> mListVideo;
    private int mCurrentVideoIdx;

    private ImageButton playImButton;
    private ImageButton stopImButton;
    private ImageButton preImButton;
    private ImageButton nextImButton;
    private ImageButton fullscrImButton;
    private ImageButton backImButton;
    private ImageButton castImButton;

    private TextView currentTimeTextView;
    private TextView totalTimeTextView;

    private SeekBar mVideoSeekBar;
    private VideoView mVideoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_movie);

        initializeListVideo();;
        initializeViews();

        mCurrentVideoIdx = 0;
        mVideoView = (VideoView) findViewById(R.id.video_view);
        mVideoView.setVideoPath(mListVideo.get(mCurrentVideoIdx));

    }

    private void initializeListVideo() {
        mListVideo = new ArrayList<>();
        mListVideo.add(EXTERNAL_PATH + "/Download/Hue.mp4");
        mListVideo.add(EXTERNAL_PATH + "/Download/VietNam.mp4");
        mListVideo.add(EXTERNAL_PATH + "/Download/Tet.mp4");
    }

    private void initializeViews() {
        initializePlayImButton();
        initializeStopImButton();
        initializePreImButton();
        initializeNextImButton();
        initializeFullImButton();
        initializeBackImButton();
        initializeCastImButton();
        initializeTimerSeekbar();
        initializeTimerTextView();
    }

    private void initializePlayImButton() {
        playImButton = (ImageButton) findViewById(R.id.im_btn_play);
        playImButton.setOnClickListener(new OnPlayButtonClickListener());
    }

    private class OnPlayButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (mVideoView.isPlaying()) {
                mVideoView.pause();
                changeIconToPlayImage(v);
            } else {
                mVideoView.start();
                changeIconToPauseImage(v);
                syncSeekBarFromVideo();
            }
        }

        private void changeIconToPlayImage(View v) {
            ((ImageButton) v).setImageResource(R.mipmap.ic_play_circle_outline_white_48dp);
        }

        private void changeIconToPauseImage(View v) {
            ((ImageButton) v).setImageResource(R.mipmap.ic_pause_circle_outline_white_48dp);
        }

    }

    private void syncSeekBarFromVideo() {
        Runnable syncTimeRunnable = new Runnable() {
            @Override
            public void run() {
                if (mVideoView.isPlaying()) {
                    int totalTime = mVideoView.getDuration();
                    totalTimeTextView.setText(milisecond2TimeFormat(totalTime));
                    mVideoSeekBar.setMax(totalTime);

                    int currentTime = mVideoView.getCurrentPosition();
                    mVideoSeekBar.setProgress(currentTime);
                    currentTimeTextView.setText(milisecond2TimeFormat(currentTime));
                }

                mVideoSeekBar.postDelayed(this, 100);
            }
        };
        syncTimeRunnable.run();
    }

    private String milisecond2TimeFormat(int milisecond) {
        int seconds = milisecond / 1000;
        int remainHours = seconds / 3600;
        int remainMinutes = (seconds % 3600) / 60;
        int remainSeconds = (seconds % 3600) % 60;

        String remainHoursString = remainHours > 0 ? String.format("%02d:", remainHours) : "";
        String remainMinutesString = String.format("%02d:", remainMinutes);
        String remainSecondsString = String.format("%02d", remainSeconds);

        return remainHoursString + remainMinutesString + remainSecondsString;
    }

    private void initializeStopImButton() {
        stopImButton = (ImageButton) findViewById(R.id.im_btn_stop);
        stopImButton.setOnClickListener(new OnStopButtonClickListener());
    }

    private class OnStopButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            mVideoView.pause();
            mVideoView.seekTo(0);
            mVideoSeekBar.setProgress(0);
            currentTimeTextView.setText(milisecond2TimeFormat(0));
            playImButton.setImageResource(R.mipmap.ic_play_circle_outline_white_48dp);
        }
    }

    private void initializePreImButton() {
        preImButton = (ImageButton) findViewById(R.id.im_btn_previous);
        preImButton.setOnClickListener(new OnPreButtonClickListener());
    }

    private class OnPreButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            mCurrentVideoIdx = ((mCurrentVideoIdx - 1) + mListVideo.size()) % mListVideo.size();
            mVideoView.stopPlayback();
            mVideoView.setVideoPath(mListVideo.get(mCurrentVideoIdx));
            mVideoView.start();
            syncSeekBarFromVideo();
        }
    }

    private void initializeNextImButton() {
        nextImButton = (ImageButton) findViewById(R.id.im_btn_next);
        nextImButton.setOnClickListener(new OnNextButtonClickListener());
    }

    private class OnNextButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            mCurrentVideoIdx = ((mCurrentVideoIdx + 1) + mListVideo.size()) % mListVideo.size();
            mVideoView.stopPlayback();
            mVideoView.setVideoPath(mListVideo.get(mCurrentVideoIdx));
            mVideoView.start();
            syncSeekBarFromVideo();
        }
    }

    private void initializeFullImButton() {
        fullscrImButton = (ImageButton) findViewById(R.id.im_btn_full_screen);
        fullscrImButton.setOnClickListener(new OnFullScrButtonClickListener());
    }

    private class OnFullScrButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
            int orientation = display.getRotation();
            Log.d("Tuan Debug", "Rotation: " + orientation);
            switch(orientation) {
                case 0:
                    setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    break;
                case 1:
                    setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    break;
            }
        }
    }

    private void initializeBackImButton() {
        backImButton = (ImageButton) findViewById(R.id.im_btn_back);
        backImButton.setOnClickListener(new OnBackButtonClickListener());
    }

    private class OnBackButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {

        }
    }

    private void initializeCastImButton() {
        castImButton = (ImageButton) findViewById(R.id.im_btn_cast_connect);
        castImButton.setOnClickListener(new OnCastButtonClickListener());
    }

    private class OnCastButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {

        }
    }

    private void initializeTimerSeekbar() {
        mVideoSeekBar = (SeekBar) findViewById(R.id.sb_running_time);
        mVideoSeekBar.setOnSeekBarChangeListener(new OnVideoSeekBarChangeListener());
    }

    private class OnVideoSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mVideoView.seekTo(seekBar.getProgress());
        }
    }

    private void initializeTimerTextView() {
        currentTimeTextView = (TextView) findViewById(R.id.tv_running_time);
        totalTimeTextView = (TextView) findViewById(R.id.tv_total_time);
    }


}
