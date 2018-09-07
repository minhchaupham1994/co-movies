package com.bighero2.comovies.connection;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * Created by Tuan on 30/03/2016.
 */
public class ClientReceiveAudioDataThread extends ClientSideThread {
    private static final String PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
    public ClientReceiveAudioDataThread(Context context) {
        super(context);
    }

    @Override
    public void run() {
        String ip = getGatewayIpAddress();
        int port = AUDIO_PORT;

        try {
            FileOutputStream fos = null;
            Socket socket = new Socket(ip, port);
            InputStream in = socket.getInputStream();
            byte[] buffer = new byte[1024];
            while(true) {
                int num = in.read(buffer);
                if (num > 0) {
                    fos = new FileOutputStream(new File(PATH + "audio.mp3"));
                    do {
                        Log.d("Tuan Debug", "NUmber of read: " + num);
                        fos.write(buffer, 0, num);
                        fos.flush();

                        num = in.read(buffer);
                    } while (num > 0);
                    fos.close();
                    break;
//                    Log.d("Tuan Debug", "NUmber of read: " + num);
//                    int transType = Tags.getTypeOfTransfer(buffer, num);
//                    if (transType == Tags.TRANS_INT_BEGIN) {
//                        fos = new FileOutputStream(new File(PATH + "audio.mp3"));
//
//                    } else if (transType == Tags.TRANS_INT_CONTENT) {
//
//                        byte[] content = Tags.getContentOfTransfered(buffer, num);
//                        int length = num - Tags.LENGTH_OF_TAG_B - Tags.LENGTH_OF_TAG_E;
//                        fos.write(content, 0, length);
//                        fos.flush();
//
//                    } else if (transType == Tags.TRANS_INT_END) {
//                        fos.close();
//
//                    } else {
//
//                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
