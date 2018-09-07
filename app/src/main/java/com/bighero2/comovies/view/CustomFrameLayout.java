package com.bighero2.comovies.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.bighero2.comovies.R;
import com.bighero2.comovies.connection.FileServer;
import com.bighero2.comovies.connection.ServerSideThread;
import com.bighero2.comovies.externalcode.WifiApControl;
import com.bighero2.comovies.support.ExtractAndTransferAudio;
import com.bighero2.comovies.util.State;
import com.bighero2.comovies.util.Tags;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tuan on 20/03/2016.
 */
public class CustomFrameLayout extends FrameLayout {
    private static final String TAG = "Tuan CustomFrameLayout";
    private Context mContext;
    private LayoutInflater mInflater;
    private Activity mActivity;

    private ArrayList<String> mListVideo;
    private int mCurrentVideoIdx;
    private int mOrientation;
    private int mCurrentTime;

    private FrameLayout mControllerViewsContainer;
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
    private CustomVideoViews mVideoView;
    private State mState;

    private ServerTransferSignalThread serverTransferSignalThread;
    private FileServer mFileServer;

    private ExtractAndTransferAudio extractAndTransferAudio;

    public CustomFrameLayout(Context context) {
        super(context);
        mContext = context;
    }

    public CustomFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public CustomFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
    }

    public void initialize(Activity activity, int rotation, ArrayList<String> listVideo, int idxVideo, State state) {
        mActivity = activity;
        mOrientation = rotation;
        mListVideo = listVideo;
        mCurrentVideoIdx = idxVideo;
        mState = state;

        mInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        addFrameLayouts();

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                initializeViews();
                registerOnCompleteVideo();
                registerHideControllerViews();
                mVideoView.start();
            }
        });
    }

    public void addFrameLayouts() {
        addCustomVideoView();
        addControllerViews();
    }

    public void addCustomVideoView() {
        mVideoView = new CustomVideoViews(mContext);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        mVideoView.setLayoutParams(params);
        mVideoView.setVideoPath(mListVideo.get(mCurrentVideoIdx));

        FrameLayout frameContainVideoView = (FrameLayout) mInflater.inflate(R.layout.container_video_view, null);
        frameContainVideoView.addView(mVideoView);
        this.addView(frameContainVideoView);
    }

    public void addControllerViews() {
        mControllerViewsContainer = (FrameLayout) mInflater.inflate(R.layout.container_controller_views, null);
        this.addView(mControllerViewsContainer);
        playImButton = (ImageButton) mControllerViewsContainer.findViewById(R.id.im_btn_play);
        playImButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mVideoView.isPlaying()) {
                    mVideoView.pause();
                } else {
                    mVideoView.start();
                }
            }
        });
    }

    private void initializeViews() {
        initializePlayImButton();
        initializeStopImButton();
        initializePreImButton();
        initializeNextImButton();
        initializeFullImButton();
        initializeBackImButton();
        initializeCastImButton();
        initializeTimerTextView();
        initializeTimerSeekbar();
    }

    private void initializePlayImButton() {
        playImButton = (ImageButton) findViewById(R.id.im_btn_play);
        playImButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mVideoView.isPlaying()) {
                    mVideoView.pause();
                    sendPauseSignal();
                } else {
                    mVideoView.start();
                    mVideoView.seekTo(mVideoView.getCurrentPosition());
                    sendResumeSignal();
//                    syncSeekBarFromVideo();
                }
            }
        });
    }

    private void sendPlaySignal() {
        Log.d(TAG, "sendPlaySignal");
        if (serverTransferSignalThread != null) {

            for (Socket socket : serverTransferSignalThread.getClientSockets()) {
                if (socket != null) {
                    try {
                    OutputStream out = socket.getOutputStream();
                    byte[] message = Tags.makeAMessage(Tags.TYPE_PLAY, String.valueOf(mVideoView.getCurrentPosition()));
                    out.write(message);
                    out.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (IndexOutOfBoundsException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }

    private void sendPauseSignal() {
        Log.d(TAG, "sendPauseSignal");
        if (serverTransferSignalThread != null) {
            for (Socket socket : serverTransferSignalThread.getClientSockets()) {
                if (socket != null) {
                    try {
                    OutputStream out = socket.getOutputStream();
                    byte[] message = Tags.makeAMessage(Tags.TYPE_PAUSE, "");
                    out.write(message);
                    out.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (IndexOutOfBoundsException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void sendResumeSignal() {
        Log.d(TAG, "sendResumeSignal");
        if (serverTransferSignalThread != null) {
            for (Socket socket : serverTransferSignalThread.getClientSockets()) {
                if (socket != null) {
                    try {
                        OutputStream out = socket.getOutputStream();
                        byte[] message = Tags.makeAMessage(Tags.TYPE_RESUME, String.valueOf(mVideoView.getCurrentPosition()));
                        out.write(message);
                        out.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (IndexOutOfBoundsException e) {
                        e.printStackTrace();
                    }
                }
            }

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

                    mCurrentTime = mVideoView.getCurrentPosition();
                    mVideoSeekBar.setProgress(mCurrentTime);
                    currentTimeTextView.setText(milisecond2TimeFormat(mCurrentTime));
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
        stopImButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                mVideoView.pause();
                mVideoView.seekTo(0);
                mVideoSeekBar.setProgress(0);
                currentTimeTextView.setText(milisecond2TimeFormat(0));
                sendStopSignal();
            }
        });
    }

    private void sendStopSignal() {
        Log.d(TAG, "sendStopSignal");
        if (serverTransferSignalThread != null) {
            for (Socket socket : serverTransferSignalThread.getClientSockets()) {
                if (socket != null) {
                    try {
                        OutputStream out = socket.getOutputStream();
                        byte[] message = Tags.makeAMessage(Tags.TYPE_STOP, "");
                        out.write(message);
                        out.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (IndexOutOfBoundsException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    private void initializePreImButton() {
        preImButton = (ImageButton) findViewById(R.id.im_btn_previous);
        preImButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                mCurrentVideoIdx = ((mCurrentVideoIdx - 1) + mListVideo.size()) % mListVideo.size();
                mVideoView.stopPlayback();
                mVideoView.setVideoPath(mListVideo.get(mCurrentVideoIdx));
                mVideoView.start();
                syncSeekBarFromVideo();
            }
        });
    }

    private void initializeNextImButton() {
        nextImButton = (ImageButton) findViewById(R.id.im_btn_next);
        nextImButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                mCurrentVideoIdx = ((mCurrentVideoIdx + 1) + mListVideo.size()) % mListVideo.size();
                mCurrentTime = 0;
                mVideoView.stopPlayback();
                mVideoView.setVideoPath(mListVideo.get(mCurrentVideoIdx));
                mVideoView.start();
                syncSeekBarFromVideo();
            }
        });
    }

    private void initializeFullImButton() {
        fullscrImButton = (ImageButton) findViewById(R.id.im_btn_full_screen);
        if (mOrientation == 0) {
            fullscrImButton.setImageDrawable(getResources().getDrawable(R.drawable.im_btn_fullscreen_selector));
        } else {
            hideStatusBar();
            fullscrImButton.setImageDrawable(getResources().getDrawable(R.drawable.im_btn_exitfullscreen_selector));
        }

        fullscrImButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (mOrientation) {
                    case 0:
                        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        break;
                    case 1:
                        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        break;
                }
            }
        });
    }

    private void initializeBackImButton() {
        backImButton = (ImageButton) findViewById(R.id.im_btn_back);
        backImButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.onBackPressed();
            }
        });
    }

    boolean isBroadcast = false;
    private void initializeCastImButton() {

        castImButton = (ImageButton) findViewById(R.id.im_btn_cast_connect);
        castImButton.setOnClickListener(new OnClickListener() {
            WifiApControl wifiApControl = new WifiApControl((WifiManager)mContext.getSystemService(Context.WIFI_SERVICE));
            WifiConfiguration wifiConfiguration = wifiApControl.getWifiApConfiguration();

            @Override
            public void onClick(View v) {
                isBroadcast = !isBroadcast;
                togglePortableWifiHotspot();


            }

            private void togglePortableWifiHotspot() {
                boolean isOn = wifiApControl.isWifiApEnabled();
                if (isOn && isBroadcast) {
                    showHotspotWasTurnedOn();
                } else if (isOn && !isBroadcast) {
                    showTurnOffHotspotDialog();
                } else if (!isOn && isBroadcast) {
                    showTurnOnHotspotDialog();
                } else {
                    showHotspotWasTurnedOff();
                }

            }

            private void showTurnOnHotspotDialog() {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle("Broadcast")
                        .setMessage("Enable portable Wifi Hotspot \"" + wifiConfiguration.SSID + "\"?")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                runServer();
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                Dialog dialog = builder.create();
                dialog.show();

            }


            private void showHotspotWasTurnedOn() {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle("Broadcast")
                        .setMessage("Portable Wifi Hotspot \"" + wifiConfiguration.SSID + "\" was turned on")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                runServer();
                                dialog.dismiss();
                            }
                        });

                Dialog dialog = builder.create();
                dialog.show();
            }

            private void runServer() {
                wifiApControl.setWifiApEnabled(wifiConfiguration, true);
                while (!wifiApControl.isWifiApEnabled()) {
                    Log.d(TAG, "waiting for Hotspot turn on");
                }
                Log.d(TAG, "After waiting for Hotspot turn on");

                if (serverTransferSignalThread == null) {
                    serverTransferSignalThread = new ServerTransferSignalThread();
                    serverTransferSignalThread.start();
                }

                try {
                    mFileServer = new FileServer();
                    mFileServer.setFileToServe(mListVideo.get(mCurrentVideoIdx), "mp4");
                    mFileServer.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            private void showHotspotWasTurnedOff() {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle("Broadcast")
                        .setMessage("Portable Wifi Hotspot \"" + wifiConfiguration.SSID + "\" was turned off")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                Dialog dialog = builder.create();
                dialog.show();
            }

            private void showTurnOffHotspotDialog() {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle("Broadcast")
                        .setMessage("Disable portable Wifi Hotspot \"" + wifiConfiguration.SSID + "\"?")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                wifiApControl.setWifiApEnabled(wifiConfiguration, false);
                                onDestroyView();
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                Dialog dialog = builder.create();
                dialog.show();
            }

            private void extractAudio() {
                extractAndTransferAudio = new ExtractAndTransferAudio(mContext);
                extractAndTransferAudio.extractAndTransfer(mListVideo.get(mCurrentVideoIdx));
            }

        });
    }

    private void initializeTimerSeekbar() {
        mVideoSeekBar = (SeekBar) findViewById(R.id.sb_running_time);
        mVideoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                int time = seekBar.getProgress();
                mVideoView.seekTo(time);
                sendSeekTime();
                currentTimeTextView.setText(milisecond2TimeFormat(mVideoView.getCurrentPosition()));
            }
        });

        if (mState != null) {
            mVideoView.seekTo(mState.currentTime);
            mVideoSeekBar.setMax(mVideoView.getDuration());
            mVideoSeekBar.setProgress(mState.currentTime);
            if (mState.isPlaying) {
                mVideoView.start();
            }
        }
        syncSeekBarFromVideo();
    }

    private void sendSeekTime() {
        Log.d(TAG, "sendSeekTimeSignal");
        if (serverTransferSignalThread != null) {

            for (Socket socket : serverTransferSignalThread.getClientSockets()) {
                if (socket != null) {
                    try {
                        OutputStream out = socket.getOutputStream();
                        byte[] message = Tags.makeAMessage(Tags.TYPE_SEEK, String.valueOf(mVideoView.getCurrentPosition()));
                        out.write(message);
                        out.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (IndexOutOfBoundsException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }
    private void initializeTimerTextView() {
        currentTimeTextView = (TextView) findViewById(R.id.tv_running_time);
        if (mState != null) {
            currentTimeTextView.setText(milisecond2TimeFormat(mState.currentTime));
        }

        totalTimeTextView = (TextView) findViewById(R.id.tv_total_time);
        totalTimeTextView.setText(milisecond2TimeFormat(mVideoView.getDuration()));
    }

    private void registerOnCompleteVideo() {
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                playImButton.setImageResource(R.mipmap.ic_play_circle_outline_white_48dp);
                mVideoSeekBar.setProgress(mVideoSeekBar.getMax());
                currentTimeTextView.setText(milisecond2TimeFormat(mVideoView.getDuration()));
            }
        });
    }

    private void registerHideControllerViews() {
        mControllerViewsContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideOrShowControllerAtTop();
                hideOrShowControllerAtBottom();
            }
        });
    }

    private void hideOrShowControllerAtTop() {
        final RelativeLayout controllerContainer = (RelativeLayout) findViewById(R.id.layout_container_controller_top);
        if (controllerContainer.getVisibility() != GONE) {
            controllerContainer.animate()
                    .translationY(-controllerContainer.getHeight())
                    .alpha(0.0f)
                    .setDuration(1000)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            controllerContainer.setVisibility(GONE);
                        }
                    });
        } else {
            controllerContainer.animate()
                    .translationY(0)
                    .alpha(1.0f)
                    .setDuration(1)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            controllerContainer.setVisibility(VISIBLE);
                        }
                    });
        }
    }

    private void hideOrShowControllerAtBottom() {
        final LinearLayout controllerContainer = (LinearLayout) findViewById(R.id.layout_container_controller_bottom);
        if (controllerContainer.getVisibility() != GONE) {
            controllerContainer.animate()
                    .translationY(controllerContainer.getHeight())
                    .alpha(0.0f)
                    .setDuration(1000)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            controllerContainer.setVisibility(GONE);
                        }
                    });
        } else {
            controllerContainer.animate()
                    .translationY(0)
                    .alpha(1.0f)
                    .setDuration(1)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            controllerContainer.setVisibility(VISIBLE);
                        }
                    });
        }
    }

    private void hideStatusBar() {
        if (Build.VERSION.SDK_INT < 16) {
            mActivity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        else {
            View decorView = mActivity.getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    public int getVideoIdx() {
        return mCurrentVideoIdx;
    }

    public int getCurrentTime() {
        return mCurrentTime;
    }

    public boolean getPlaying() {
        return mVideoView.isPlaying();
    }

    public void onDestroyView() {
        if (serverTransferSignalThread != null) {
            try {
                serverTransferSignalThread.closeSockets();
                serverTransferSignalThread.stopThreads();

                if (!serverTransferSignalThread.isInterrupted()) {
                    serverTransferSignalThread.interrupt();
                    serverTransferSignalThread = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (mFileServer != null) {
            mFileServer.stop();
        }

        if (mVideoView != null) {
            mVideoView.stopPlayback();
        }
    }
    private class CustomVideoViews extends VideoView {

        public CustomVideoViews(Context context) {
            super(context);
        }

        public CustomVideoViews(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public CustomVideoViews(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @Override
        public void pause() {
            changeIconToPlayImage();
            super.pause();
        }

        private void changeIconToPlayImage() {
            playImButton.setImageResource(R.mipmap.ic_play_circle_outline_white_48dp);
        }

        @Override
        public void start() {
            changeIconToPauseImage();
            super.start();
        }

        private void changeIconToPauseImage() {
            playImButton.setImageResource(R.mipmap.ic_pause_circle_outline_white_48dp);
        }


    }

    public class ServerTransferSignalThread extends ServerSideThread {
        public static final int PORT_1331 = 1331;
        private ServerSocket mServerSocket;
        private List<Socket> mClientSockets;
        private List<Thread> mReplyClientThreads;

        public ServerTransferSignalThread() {
            mClientSockets = new ArrayList<>();
            mReplyClientThreads = new ArrayList<>();
        }

        @Override
        public void run() {
            try {
                Log.d("Tuan DEBUG", "IP: " + getIpAddress());
                mServerSocket = new ServerSocket(PORT_1331);

                while (true) {
                    Socket clientSocket = mServerSocket.accept();
                    mClientSockets.add(clientSocket);

                    ServerListensClientThread replyClient = new ServerListensClientThread(clientSocket);
                    mReplyClientThreads.add(replyClient);
                    replyClient.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public List<Socket> getClientSockets() {
            return mClientSockets;
        }

        public void closeSockets() throws IOException {
            for (Socket socket : mClientSockets) {
                if (!socket.isClosed()) {
                    socket.close();
                }
            }

            if (!mServerSocket.isClosed()) {
                mServerSocket.close();
            }
        }

        public void stopThreads() throws InterruptedException {
            for (Thread thread : mReplyClientThreads) {
                if (thread != null && !thread.isInterrupted()) {
                    thread.interrupt();
                }
            }
        }

        private class ServerListensClientThread extends Thread {
            private Socket mSocket;
            private OutputStream out;

            public ServerListensClientThread(Socket client) {
                try {
                    mSocket = client;
                    out = mSocket.getOutputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void run() {
                try {
                    InputStream in = mSocket.getInputStream();
                    byte[] buffer = new byte[1024];
                    while(true) {
                        int num = in.read(buffer);
                        if (num != -1) {
                            Log.d(TAG, "InputStream - " + num + ": " + Tags.bytes2String(buffer, num));
                            int type = Tags.typeOfMessage(buffer, num);
                            switch (type) {
                                case Tags.TYPE_REQ:
                                    sendPortNumber();
                                    break;
                                case Tags.TYPE_READY:
                                    if (mVideoView.isPlaying()) {
                                        mVideoView.seekTo(mVideoView.getCurrentPosition());
                                        sendPlaySignal();
                                    }
                                    else {
                                        sendPauseSignal();
                                    }
                                    break;
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            private void sendPortNumber() throws IOException {
                Log.d(TAG, "sendPortNumber");
                byte[] message = Tags.makeAMessage(Tags.TYPE_ANSWER, "8080");
                out.write(message);
                out.flush();
            }

        }

    }

}
