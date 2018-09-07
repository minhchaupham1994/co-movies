package com.bighero2.comovies.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bighero2.comovies.R;
import com.bighero2.comovies.connection.ClientSideThread;
import com.bighero2.comovies.util.Tags;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

/**
 * Created by hp1 on 21-01-2015.
 */
public class Tab2 extends Fragment {
    private static final String TAG = "Tuan Tab2";

    private ImageButton syncBtn;
    private ProgressBar timeBar;
    private TextView currentTimeTV;
    private TextView totalTimeTV;

    private boolean isOn = false;

    private Context mContext;
    private MediaPlayer mMediaPlayer;
    private ClientReceiveSignalThread clientReceiveSignalThread;
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.tab_2, container, false);
        mContext = inflater.getContext();

        currentTimeTV = (TextView) v.findViewById(R.id.tv_client_current_time);
        totalTimeTV = (TextView) v.findViewById(R.id.tv_client_total_time);
        syncBtn = (ImageButton) v.findViewById(R.id.syncBtn);
        timeBar = (ProgressBar) v.findViewById(R.id.progressBar);

        timeBar.getProgressDrawable().setColorFilter(
                getResources().getColor(R.color.colorPrimary), android.graphics.PorterDuff.Mode.SRC_IN);

        syncBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WifiManager wifiManager = (WifiManager) (inflater.getContext()).getSystemService(Context.WIFI_SERVICE);
                if (!wifiManager.isWifiEnabled()) {
                    isOn = !isOn;
                    Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    startActivity(intent);
                }

                while (!wifiManager.isWifiEnabled()) {
                    Log.d(TAG, "waiting for turn on wifi");
                }
                Log.d(TAG, "after waiting for turn on wifi");

                isOn = !isOn;
                if (isOn) {

                    clientReceiveSignalThread = new ClientReceiveSignalThread(mContext);
                    clientReceiveSignalThread.start();

                    syncBtn.setImageResource(R.drawable.btn);
                } else {

                    try {
                        if (clientReceiveSignalThread != null) {
                            clientReceiveSignalThread.closeSocket();
                            if (!clientReceiveSignalThread.isInterrupted()) {
                                clientReceiveSignalThread.interrupt();
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (mMediaPlayer != null) {
                        mMediaPlayer.stop();
                        mMediaPlayer.release();
                    }

                    syncBtn.setImageResource(R.drawable.btn2);
                }

            }
        });

        return v;
    }

    public class ClientReceiveSignalThread extends ClientSideThread {

        private Socket mSocket;

        public ClientReceiveSignalThread(Context context) {
            super(context);
        }

        @Override
        public void run() {
            String ip = getGatewayIpAddress();
            int port = SIGNAL_PORT;

            try {
                mSocket = new Socket(ip, port);
                sendRequest();

                InputStream in = mSocket.getInputStream();
                byte[] buffer = new byte[1024];
                while(true) {
                    try {
                        int num = in.read(buffer);
                        if (num != -1) {
                            Log.d(TAG, "InputStream: " + Tags.bytes2String(buffer, num));
                            int type = Tags.typeOfMessage(buffer, num);
                            switch (type) {
                                case Tags.TYPE_ANSWER:
                                    String portHttpD = Tags.contentOfMessage(buffer, num);
                                    prepareMedia(portHttpD);
                                    break;
                                case Tags.TYPE_PLAY:
                                    String time = Tags.contentOfMessage(buffer, num);
                                    startMedia(time);
                                    break;
                                case Tags.TYPE_PAUSE:
                                    pauseMedia();
                                    break;
                                case Tags.TYPE_RESUME:
                                    String resumeTime = Tags.contentOfMessage(buffer, num);
                                    resumeMedia(resumeTime);
                                case Tags.TYPE_SEEK:
                                    String seekTime = Tags.contentOfMessage(buffer, num);
                                    seekMedia(seekTime);
                                    break;
                                case Tags.TYPE_STOP:
                                    stopMedia();
                                    break;
                            }
                        }
                    } catch (SocketException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        public void sendRequest() throws IOException {
            byte[] message = Tags.makeAMessage(Tags.TYPE_REQ, "");
            Log.d(TAG, "sendRequest: " + message.length);
            OutputStream out = mSocket.getOutputStream();
            out.write(message);
            out.flush();
        }

        private void prepareMedia(String port) throws IOException {
            String address =  "http://" + getGatewayIpAddress() + ":" + port + "/";
            Log.d(TAG, "prepareMedia: " + address);
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(address);
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    try {
                        updateTotalTimeTV();
                        sendReady();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            mMediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                    Log.d(TAG, "Percent: " + percent + " %");
                }
            });
            mMediaPlayer.prepare();
        }

        private void updateTotalTimeTV() {
            Activity activity = (Activity) mContext;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int time = mMediaPlayer.getDuration();
                    String timeS = milisecond2TimeFormat(time);
                    totalTimeTV.setText(timeS);
                    syncSeekBarFromMedia();
                }
            });
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

        int mCurrentTime = 0;
        private void syncSeekBarFromMedia() {
            Runnable syncTimeRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        if (mMediaPlayer != null) {
                            if (mMediaPlayer.isPlaying()) {
                                int totalTime = mMediaPlayer.getDuration();
                                timeBar.setMax(totalTime);

                                mCurrentTime = mMediaPlayer.getCurrentPosition();
                                timeBar.setProgress(mCurrentTime);
                                currentTimeTV.setText(milisecond2TimeFormat(mCurrentTime));
                            }

                            timeBar.postDelayed(this, 100);
                        }
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                        try {
                            join();
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            };
            syncTimeRunnable.run();
        }
        private void sendReady() throws IOException {
            byte[] message = Tags.makeAMessage(Tags.TYPE_READY, "");
            Log.d(TAG, "sendReady: " + message.length);
            OutputStream out = mSocket.getOutputStream();
            out.write(message);
            out.flush();
        }

        private void startMedia(String time) {
            int timeToSeek = Integer.parseInt(time);
            Log.d(TAG, "timeToSeek: " + timeToSeek);
            mMediaPlayer.start();
            mMediaPlayer.seekTo(timeToSeek);

        }

        private void pauseMedia() {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
            }
        }

        private void resumeMedia(String time) {
            if (!mMediaPlayer.isPlaying()){
                int timeToSeek = Integer.parseInt(time);
                mMediaPlayer.start();
                mMediaPlayer.seekTo(timeToSeek);
            }
        }

        private void seekMedia(final String time) {

            if (mMediaPlayer != null) {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.seekTo(Integer.parseInt(time));
                } else {
                    Activity activity = (Activity) mContext;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            timeBar.setProgress(Integer.parseInt(time));
                        }
                    });

                }
            }
        }

        private void stopMedia() {
            mMediaPlayer.pause();
            mMediaPlayer.seekTo(0);

            Activity activity = (Activity) mContext;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    timeBar.setProgress(0);
                    currentTimeTV.setText(milisecond2TimeFormat(0));
                }
            });


        }
        public void closeSocket() throws IOException {
            if (mSocket != null) {
                mSocket.close();
            }
        }
    }
}