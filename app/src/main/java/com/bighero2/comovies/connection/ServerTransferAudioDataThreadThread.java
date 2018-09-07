package com.bighero2.comovies.connection;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tuan on 30/03/2016.
 */
public class ServerTransferAudioDataThreadThread extends ServerSideThread {
    public static final int PORT_1221 = 1221;
    private ServerSocket mServerSocket;
    private List<Socket> mSocketClientList;

    public ServerTransferAudioDataThreadThread() {
        mSocketClientList = new ArrayList<>();
    }

    @Override
    public void run() {
        try {
            Log.d("Tuan DEBUG", "IP: " + getIpAddress());
            mServerSocket = new ServerSocket(PORT_1221);

            while (true) {
                Socket clientSocket = mServerSocket.accept();
                mSocketClientList.add(clientSocket);

                ServerListensClientThread replyClient = new ServerListensClientThread(clientSocket);
                replyClient.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Socket> getClientSockets() {
        return mSocketClientList;
    }

    private class ServerListensClientThread extends Thread {
        private Socket mSocket;

        public ServerListensClientThread(Socket client) {
            mSocket = client;
        }
        @Override
        public void run() {
//            try {
//
//                InputStream in = mSocket.getInputStream();
//                byte[] buffer = new byte[1024];
//                while(true) {
//                    int num = in.read(buffer);
//                    if (num > 0) {
//                        Log.d("Tuan DEBUG", "num: " + num);
//                    }
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }

    }


}
