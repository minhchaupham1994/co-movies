package com.bighero2.comovies.support;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.bighero2.comovies.garbage.ExtractDecodeEditEncodeMuxTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

/**
 * Created by tuan on 09/04/2016.
 */
public class ExtractAndTransferAudio {
    private Context mContext;
    private List<Socket> mClientSockets;
    private File mExtractedAudio;

    public ExtractAndTransferAudio(Context context) {
        mContext = context;
    }

    public void setClientSockets(List<Socket> sockets) {
        mClientSockets = sockets;
    }

    public void extractAndTransfer(String video) {
        ExtractAndTransferAudioFromVideo extractAudioAsyncTask = new ExtractAndTransferAudioFromVideo(mContext);
        extractAudioAsyncTask.execute(video);
    }

    private class ExtractAndTransferAudioFromVideo extends AsyncTask<String, Void, Boolean> {
        private Context mContext;
        private ProgressDialog progressDialog;

        public ExtractAndTransferAudioFromVideo(Context context) {
            mContext = context;
        }

        @Override
        protected void onPreExecute() {
//        progressDialog = ProgressDialog.show(mContext, "Please wait. . . ", "hehe", true);
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String input = params[0];
            String output = setNameAudio(input);

            mExtractedAudio = new File(output);
            if (!mExtractedAudio.exists()) {
                ExtractDecodeEditEncodeMuxTest test = new ExtractDecodeEditEncodeMuxTest(mContext, input, output);
                try {
                    test.testExtractDecodeEditEncodeMuxAudio();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }

//            ExtractAudioFromVideo extract = new ExtractAudioFromVideo(input, output);
//            try {
//                extract.extractAudio();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            return true;
        }

        private String setNameAudio(String input) {
            File file = new File(input);
            String dir = file.getParent();
            String out = file.getName().replace(".mp4", ".mp3");
            return dir + "/" + out;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
//        progressDialog.dismiss();
            TransferAudioAsyncTask transferAudioAsynctask = new TransferAudioAsyncTask();
            transferAudioAsynctask.execute();
            super.onPostExecute(aBoolean);
        }


    }



    private class TransferAudioAsyncTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            return transferAudio();
        }

        private boolean transferAudio() {
            try {
                while (mClientSockets == null) {
                    Log.d("Tuan Debug", "client sockets null");
                    Thread.sleep(500);
                }
                Log.d("Tuan Debug", "client sockets NOT null");

                while (mClientSockets.size() < 1) {
                    Log.d("Tuan Debug", "client socket size < 1");
                    Thread.sleep(500);
                }
                Log.d("Tuan Debug", "client socket size > 0");

                OutputStream os = mClientSockets.get(0).getOutputStream();
//                flushBeginOfTransfer(os);
                flushContentOfAudio(os);
//                flushEndOfTransfer(os);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {

            }

            return true;
        }

        private void flushBeginOfTransfer(OutputStream os) throws IOException {
//            byte[] begin = Tags.combineBytesTag(Tags.TRANS_INT_BEGIN, null, 0);
//            os.write(begin, 0, begin.length);
//            os.flush();
        }

        private void flushContentOfAudio(OutputStream os) throws IOException {
            FileInputStream fis = new FileInputStream(mExtractedAudio);

            int size = 1024;
            byte[] buffer = new byte[size];
            int num = 0;
            while ((num = fis.read(buffer, 0, size)) != -1) {
                Log.d("Tuan Debug", "Num of read: " + num);
//                byte[] transBuffer = Tags.combineBytesTag(Tags.TRANS_INT_CONTENT, buffer, num);
//                Log.d("Tuan Debug", "Num of transfer: " + transBuffer.length);
//                os.write(transBuffer, 0, transBuffer.length);
                os.write(buffer, 0, num);
                os.flush();

            }

            fis.close();
        }

//        private void flushEndOfTransfer(OutputStream os) throws IOException{
//            Log.d("Tuan Debug", "EndOfTransfer");
//            byte[] end = Tags.combineBytesTag(Tags.TRANS_INT_END, null, 0);
//            os.write(end, 0, end.length);
//            os.flush();
//        }
    }
}
