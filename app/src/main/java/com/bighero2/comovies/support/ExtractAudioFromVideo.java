package com.bighero2.comovies.support;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import com.bighero2.comovies.util.PieceObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import it.sauronsoftware.jave.AudioAttributes;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.EncodingAttributes;
import it.sauronsoftware.jave.InputFormatException;

/**
 * Created by Tuan on 20/03/2016.
 */
public class ExtractAudioFromVideo {
    private static final String TAG = "Tuan ExtractAudioF";
    private MediaExtractor mExtractor;
    private ArrayList<PieceObject> mAudioByteBuffers;

    private String input;
    private String output;

    public ExtractAudioFromVideo(String input, String output) {
        this.input = input;
        this.output = output + "/audio.mp3";
        this.mAudioByteBuffers = new ArrayList<>();
    }

    public void extractAudio() throws IOException {
        MediaExtractor audioExtractor = createAudioExtractor(input);
        int audioTrackIndex = getAndSelectAudioTrackIndex(audioExtractor);
        MediaFormat inputFormat = audioExtractor.getTrackFormat(audioTrackIndex);
        MediaCodec audioDecoder = createAudioDecoder(inputFormat);
        doExtractor(audioExtractor, audioDecoder);
    }

    private int getAndSelectAudioTrackIndex(MediaExtractor extractor) {
        for (int idx = 0; idx < extractor.getTrackCount(); idx++) {
            MediaFormat format = extractor.getTrackFormat(idx);
            String mimeType = format.getString(MediaFormat.KEY_MIME);
            if(mimeType.startsWith("audio/")) {
                extractor.selectTrack(idx);
                return idx;
            }
        }
        return -1;
    }

    private MediaExtractor createAudioExtractor(String file) {
        try {
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(file);
            return extractor;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private MediaCodec createAudioDecoder(MediaFormat format) {
        try {
            MediaCodec decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
            decoder.configure(format, null, null, 0);
            decoder.start();
            return decoder;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void doExtractor(MediaExtractor audioExtractor, MediaCodec audioDecoder) throws IOException {
        ByteBuffer[] audioDecoderInputBuffers = null;
        ByteBuffer[] audioDecoderOutputBuffers = null;
        MediaCodec.BufferInfo audioDecoderOutputBufferInfo = null;

        audioDecoderInputBuffers = audioDecoder.getInputBuffers();
        audioDecoderOutputBuffers = audioDecoder.getOutputBuffers();
        audioDecoderOutputBufferInfo = new MediaCodec.BufferInfo();

        boolean audioExtractorDone = false;
        while(!audioExtractorDone) {
            int decoderInputBufferIndex = audioDecoder.dequeueInputBuffer(10000);
            if (decoderInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                Log.d("Tuan Debug", "No audio decoder input buffer");
            }

//            ByteBuffer decoderInputBuffer = audioDecoderInputBuffers[decoderInputBufferIndex];
            ByteBuffer decoderInputBuffer = ByteBuffer.allocate(1024 * 8);
            int size = audioExtractor.readSampleData(decoderInputBuffer, 0);
            long presentationTime = audioExtractor.getSampleTime();
            Log.d(TAG, "audio extractor: returned buffer of size " + size);
            Log.d(TAG, "audio extractor: returned buffer for time " + presentationTime);

            mAudioByteBuffers.add(new PieceObject(decoderInputBuffer, size, presentationTime));
            if (size >= 0) {
            }
//            if (size >= 0 ) {
//                audioDecoder.queueInputBuffer(
//                        decoderInputBufferIndex,
//                        0,
//                        size,
//                        presentationTime,
//                        audioExtractor.getSampleFlags());
//
//            }

            audioExtractorDone = !audioExtractor.advance();
//            if (audioExtractorDone) {
//                Log.d(TAG, "Audio extractor: EOS");
//                audioDecoder.queueInputBuffer(
//                        decoderInputBufferIndex,
//                        0,
//                        0,
//                        0,
//                        MediaCodec.BUFFER_FLAG_END_OF_STREAM);
//            }
        }
        audioExtractor.release();
    }

    public ArrayList<PieceObject> getAudioByteBuffers() {
        return mAudioByteBuffers;
    }
}
