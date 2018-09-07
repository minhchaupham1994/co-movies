package com.bighero2.comovies.util;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bighero2.comovies.PlayMovieActivity;
import com.bighero2.comovies.R;
import com.bighero2.comovies.view.Tab1;

public class CustomAdapter extends BaseAdapter{
    public static final String EXTRA_POSITION = "position";
    public static final String EXTRA_MOVIES = "movies array";
    Cursor videocursor;
    Context context;
    int video_column_index;
    String title;
    String duration;
    String resolution;
    Bitmap bitmap;
    LinearLayout view;
    ImageView img;
    String[] result;

    private static LayoutInflater inflater=null;
    public CustomAdapter(Tab1 tab1, Cursor _videocursor, String[] _result) {
        // TODO Auto-generated constructor stub
        videocursor=_videocursor;
        context=tab1.getContext();
        result = _result;
        inflater = ( LayoutInflater )context.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return videocursor.getCount();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    public class Holder
    {
        TextView title;
        TextView duration;
        TextView resolution;
        ImageView thumbnail;
    }
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        Holder holder=new Holder();
        final View rowView;

        videocursor.moveToPosition(position);

        video_column_index = videocursor
                .getColumnIndexOrThrow(MediaStore.Video.Media.DATA);

        result[position] = videocursor.getString(video_column_index);

        video_column_index = videocursor
                .getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);

        title = videocursor.getString(video_column_index);

        bitmap = MediaStore.Video.Thumbnails.getThumbnail(
                context.getContentResolver(), videocursor.getLong(videocursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns._ID)),
                MediaStore.Video.Thumbnails.MINI_KIND,
                (BitmapFactory.Options) null);

        video_column_index = videocursor
                .getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);

        duration = videocursor.getString(video_column_index);
        int temp = Integer.parseInt(duration)/1000;
//        duration = String.valueOf(temp/60) + ":" + String.valueOf(temp%60);
        duration = String.format("%02d", temp/60) + ":" + String.format("%02d", temp%60);

        video_column_index = videocursor
                .getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH);

        resolution = videocursor.getString(video_column_index);

        video_column_index = videocursor
                .getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT);

        resolution += " x " + videocursor.getString(video_column_index);



        if(videocursor.getPosition() != 0) {
            rowView = inflater.inflate(R.layout.program_list, null);
            holder.title=(TextView) rowView.findViewById(R.id.title);
            holder.thumbnail=(ImageView) rowView.findViewById(R.id.Thumbnail);
            holder.title=(TextView) rowView.findViewById(R.id.title);
            holder.duration = (TextView) rowView.findViewById(R.id.duration);
            holder.resolution = (TextView) rowView.findViewById(R.id.resolution);

            holder.title.setText(title);
            holder.duration.setText(duration);
            holder.resolution.setText(resolution);

            holder.title.setText(title);

            holder.thumbnail.setImageBitmap(bitmap);
            holder.thumbnail.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        }

        else {
            System.out.println(title);
            rowView = inflater.inflate(R.layout.head, null);
            holder.thumbnail=(ImageView) rowView.findViewById(R.id.Thumbnail);
            holder.title=(TextView) rowView.findViewById(R.id.title);
            holder.duration = (TextView) rowView.findViewById(R.id.duration);
            holder.resolution = (TextView) rowView.findViewById(R.id.resolution);

            holder.title.setText(title);
            holder.duration.setText(duration);
            holder.resolution.setText(resolution);

            view = (LinearLayout) rowView.findViewById(R.id.mainroot);

            videocursor.moveToPosition(0);
            final Bitmap bitmap2 = MediaStore.Video.Thumbnails.getThumbnail(
                    context.getContentResolver(), videocursor.getLong(videocursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns._ID)),
                    MediaStore.Video.Thumbnails.MINI_KIND,
                    (BitmapFactory.Options) null);

            view.post(new Runnable() {
                @Override
                public void run() {
                    Bitmap bitmap1 = Bitmap.createScaledBitmap(bitmap2, view.getWidth(), view.getWidth()*9/16, false);

                    img = (ImageView) rowView.findViewById(R.id.Thumbnail);
                    img.setImageBitmap(bitmap1);
                }
            });
        }

        rowView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Intent playMovieIntent = new Intent(context, PlayMovieActivity.class);
                playMovieIntent.putExtra(EXTRA_MOVIES, result);
                playMovieIntent.putExtra(EXTRA_POSITION, position);
                context.startActivity(playMovieIntent);

                Toast.makeText(context, "You Clicked " + result[position], Toast.LENGTH_LONG).show();
            }
        });
        return rowView;
    }

}