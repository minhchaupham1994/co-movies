package com.bighero2.comovies.view;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.VideoView;

import com.bighero2.comovies.R;

/**
 * Created by Tuan on 20/03/2016.
 */
public class CustomVideoView extends VideoView {

    private ImageButton playImButton;

    public CustomVideoView(Context context) {
        super(context);
    }

    public CustomVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void initialize(Activity activity) {
        addCustomControllerViews(activity);
    }

    private void addCustomControllerViews(Activity activity) {
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        FrameLayout controllerViews = (FrameLayout) inflater.inflate(R.layout.container_controller_views, null);

        playImButton = (ImageButton) controllerViews.findViewById(R.id.im_btn_play);
        playImButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CustomVideoView.this.isPlaying()) {
                    CustomVideoView.this.pause();
                } else {
                    CustomVideoView.this.start();
                }
            }
        });
    }

    @Override
    public void pause() {
        super.pause();
        Log.d("Tuan Debug", "CustomVideoView Pause");
    }

    @Override
    public void start() {
        super.start();
        Log.d("Tuan Debug", "CustomVideoView Start");
    }
}
