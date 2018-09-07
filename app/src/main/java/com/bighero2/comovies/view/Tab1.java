package com.bighero2.comovies.view;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.bighero2.comovies.util.CustomAdapter;
import com.bighero2.comovies.R;

/**
 * Created by hp1 on 21-01-2015.
 */
public class Tab1 extends Fragment {

    private Cursor videocursor;
    int count;
    ListView lv;
    ImageView imageThumbnail;
    LinearLayout view;
    String[] result;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v =inflater.inflate(R.layout.tab_1,container,false);

        System.gc();
        String[] proj = { MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT};
        videocursor = getContext().getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                proj, null, null, null);

        result = new String[videocursor.getCount()];

        lv=(ListView) v.findViewById(R.id.listView);
        lv.setAdapter(new CustomAdapter(this, videocursor,result));


        return v;
    }

}
