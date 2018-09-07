
/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bighero2.comovies.garbage;
import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;

import com.bighero2.comovies.R;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Test for the integration of MediaMuxer and MediaCodec's encoder.
 *
 * <p>It uses MediaExtractor to get frames from a test stream, decodes them to a surface, uses a
 * shader to edit them, encodes them from the resulting surface, and then uses MediaMuxer to write
 * them into a file.
 *
 * <p>It does not currently check whether the result file is correct, but makes sure that nothing
 * fails along the way.
 *
 * <p>It also tests the way the codec config buffers need to be passed from the MediaCodec to the
 * MediaMuxer.
 */
@TargetApi(18)
public class ExtractDecodeEditEncodeMuxTest {
    private static final String TAG = "Tuan ExtractDecideE";
    private static final boolean VERBOSE = false; // lots of logging
    /** How long to wait for the next buffer to become available. */
    private static final int TIMEOUT_USEC = 10000;
    // parameters for the audio encoder
    private static final String OUTPUT_AUDIO_MIME_TYPE = "audio/mp4a-latm"; // Advanced Audio Coding
    private static final int OUTPUT_AUDIO_CHANNEL_COUNT = 2; // Must match the input stream.
    private static final int OUTPUT_AUDIO_BIT_RATE = 128 * 1024;
    private static final int OUTPUT_AUDIO_AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectHE;
    private static final int OUTPUT_AUDIO_SAMPLE_RATE_HZ = 44100; // Must match the input stream.

    /** Whether to copy the audio from the test video. */
    private boolean mCopyAudio;
    /** The destination file for the encoded output. */
    private String mOutputFile;

    private String mInputPath;
    private String mOutputPath;

    public ExtractDecodeEditEncodeMuxTest(Context context, String input, String output) {
        mInputPath = input;
        mOutputPath = output;
    }

    /** Wraps testExtractDecodeEditEncodeMux() */
    private static class TestWrapper implements Runnable {
        private Throwable mThrowable;
        private ExtractDecodeEditEncodeMuxTest mTest;
        private TestWrapper(ExtractDecodeEditEncodeMuxTest test) {
            mTest = test;
        }
        @Override
        public void run() {
            try {
                mTest.extractDecodeEditEncodeMux();
            } catch (Throwable th) {
                mThrowable = th;
            }
        }
        /**
         * Entry point.
         */
        public static void runTest(ExtractDecodeEditEncodeMuxTest test) throws Throwable {
            test.setOutputFile();
            TestWrapper wrapper = new TestWrapper(test);
            Thread th = new Thread(wrapper, "codec test");
            th.start();
            th.join();
            if (wrapper.mThrowable != null) {
                throw wrapper.mThrowable;
            }
        }
    }
    /**
     * Sets the test to copy the video stream.
     */
    private void setCopyAudio() {
        mCopyAudio = true;
    }
    /**
     * Sets the name of the output file based on the other parameters.
     *
     */
    private void setOutputFile() {
        mOutputFile = mOutputPath;
    }
    public void testExtractDecodeEditEncodeMuxAudio() throws Throwable {

        setCopyAudio();
        TestWrapper.runTest(this);
    }

    /**
     * Tests encoding and subsequently decoding video from frames generated into a buffer.
     * <p>
     * We encode several frames of a video test pattern using MediaCodec, then decode the output
     * with MediaCodec and do some simple checks.
     */
    private void extractDecodeEditEncodeMux() throws Exception {
        // Exception that may be thrown during release.
        Exception exception = null;
        MediaCodecInfo audioCodecInfo = selectCodec(OUTPUT_AUDIO_MIME_TYPE);
        if (audioCodecInfo == null) {
            // Don't fail CTS if they don't have an AAC codec (not here, anyway).
            Log.e(TAG, "Unable to find an appropriate codec for " + OUTPUT_AUDIO_MIME_TYPE);
            return;
        } else {
            Log.d(TAG, "AudioCodecInfo: " + audioCodecInfo.getName());
        }
        if (VERBOSE) Log.d(TAG, "audio found codec: " + audioCodecInfo.getName());
        MediaExtractor audioExtractor = null;
        MediaCodec audioDecoder = null;
        MediaCodec audioEncoder = null;
        MediaMuxer muxer = null;
        try {
            if (mCopyAudio) {
                audioExtractor = createExtractor();
                int audioInputTrack = getAndSelectAudioTrackIndex(audioExtractor);
                MediaFormat inputFormat = audioExtractor.getTrackFormat(audioInputTrack);
                MediaFormat outputAudioFormat =
                        MediaFormat.createAudioFormat(
                                OUTPUT_AUDIO_MIME_TYPE, OUTPUT_AUDIO_SAMPLE_RATE_HZ,
                                OUTPUT_AUDIO_CHANNEL_COUNT);
                outputAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_AUDIO_BIT_RATE);
                outputAudioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, OUTPUT_AUDIO_AAC_PROFILE);
                // Create a MediaCodec for the desired codec, then configure it as an encoder with
                // our desired properties. Request a Surface to use for input.
                audioEncoder = createAudioEncoder(audioCodecInfo, outputAudioFormat);
                // Create a MediaCodec for the decoder, based on the extractor's format.
                audioDecoder = createAudioDecoder(inputFormat);
            }
            // Creates a muxer but do not start or add tracks just yet.
            muxer = createMuxer();
            doExtractDecodeEditEncodeMux(
                    audioExtractor,
                    audioDecoder,
                    audioEncoder,
                    muxer);
        } finally {
            if (VERBOSE) Log.d(TAG, "releasing extractor, decoder, encoder, and muxer");
            // Try to release everything we acquired, even if one of the releases fails, in which
            // case we save the first exception we got and re-throw at the end (unless something
            // other exception has already been thrown). This guarantees the first exception thrown
            // is reported as the cause of the error, everything is (attempted) to be released, and
            // all other exceptions appear in the logs.

            try {
                if (audioExtractor != null) {
                    audioExtractor.release();
                }
            } catch(Exception e) {
                Log.e(TAG, "error while releasing audioExtractor", e);
                if (exception == null) {
                    exception = e;
                }
            }


            try {
                if (audioDecoder != null) {
                    audioDecoder.stop();
                    audioDecoder.release();
                }
            } catch(Exception e) {
                Log.e(TAG, "error while releasing audioDecoder", e);
                if (exception == null) {
                    exception = e;
                }
            }
            try {
                if (audioEncoder != null) {
                    audioEncoder.stop();
                    audioEncoder.release();
                }
            } catch(Exception e) {
                Log.e(TAG, "error while releasing audioEncoder", e);
                if (exception == null) {
                    exception = e;
                }
            }
            try {
                if (muxer != null) {
                    muxer.stop();
                    muxer.release();
                }
            } catch(Exception e) {
                Log.e(TAG, "error while releasing muxer", e);
                if (exception == null) {
                    exception = e;
                }
            }
        }
        if (exception != null) {
            throw exception;
        }
    }
    /**
     * Creates an extractor that reads its frames from.
     */
    private MediaExtractor createExtractor() throws IOException {
        MediaExtractor extractor;
        extractor = new MediaExtractor();
        extractor.setDataSource(mInputPath);
        return extractor;
    }


    /**
     * Creates a decoder for the given format.
     *
     * @param inputFormat the format of the stream to decode
     */
    private MediaCodec createAudioDecoder(MediaFormat inputFormat) throws IOException{
        MediaCodec decoder = MediaCodec.createDecoderByType(getMimeTypeFor(inputFormat));
        decoder.configure(inputFormat, null, null, 0);
        decoder.start();
        return decoder;
    }
    /**
     * Creates an encoder for the given format using the specified codec.
     *
     * @param codecInfo of the codec to use
     * @param format of the stream to be produced
     */
    private MediaCodec createAudioEncoder(MediaCodecInfo codecInfo, MediaFormat format)  throws IOException{
        MediaCodec encoder = MediaCodec.createByCodecName(codecInfo.getName());
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        encoder.start();
        return encoder;
    }
    /**
     * Creates a muxer to write the encoded frames.
     *
     * <p>The muxer is not started as it needs to be started only after all streams have been added.
     */
    private MediaMuxer createMuxer() throws IOException {
        return new MediaMuxer(mOutputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
    }

    private int getAndSelectAudioTrackIndex(MediaExtractor extractor) {
        for (int index = 0; index < extractor.getTrackCount(); ++index) {
            if (VERBOSE) {
                Log.d(TAG, "format for track " + index + " is "
                        + getMimeTypeFor(extractor.getTrackFormat(index)));
            }
            if (isAudioFormat(extractor.getTrackFormat(index))) {
                extractor.selectTrack(index);
                return index;
            }
        }
        return -1;
    }
    /**
     * Does the actual work for extracting, decoding, encoding and muxing.
     */
    private void doExtractDecodeEditEncodeMux(
            MediaExtractor audioExtractor,
            MediaCodec audioDecoder,
            MediaCodec audioEncoder,
            MediaMuxer muxer) {

        ByteBuffer[] audioDecoderInputBuffers = null;
        ByteBuffer[] audioDecoderOutputBuffers = null;
        ByteBuffer[] audioEncoderInputBuffers = null;
        ByteBuffer[] audioEncoderOutputBuffers = null;
        MediaCodec.BufferInfo audioDecoderOutputBufferInfo = null;
        MediaCodec.BufferInfo audioEncoderOutputBufferInfo = null;
        if (mCopyAudio) {
            audioDecoderInputBuffers = audioDecoder.getInputBuffers();
            audioDecoderOutputBuffers =  audioDecoder.getOutputBuffers();
            audioEncoderInputBuffers = audioEncoder.getInputBuffers();
            audioEncoderOutputBuffers = audioEncoder.getOutputBuffers();
            audioDecoderOutputBufferInfo = new MediaCodec.BufferInfo();
            audioEncoderOutputBufferInfo = new MediaCodec.BufferInfo();
        }
        // We will get these from the decoders when notified of a format change.
        MediaFormat decoderOutputAudioFormat = null;
        // We will get these from the encoders when notified of a format change.
        MediaFormat encoderOutputAudioFormat = null;
        // We will determine these once we have the output format.
        int outputAudioTrack = -1;
        // Whether things are done on the audio side.
        boolean audioExtractorDone = false;
        boolean audioDecoderDone = false;
        boolean audioEncoderDone = false;
        // The audio decoder output buffer to process, -1 if none.
        int pendingAudioDecoderOutputBufferIndex = -1;
        boolean muxing = false;
        int audioExtractedFrameCount = 0;
        int audioDecodedFrameCount = 0;
        int audioEncodedFrameCount = 0;
        while ((mCopyAudio && !audioEncoderDone)) {
            Log.d(TAG, String.format(
                    "loop: "
                    + "A(%b){"
                    + "extracted:%d(done:%b) "
                    + "decoded:%d(done:%b) "
                    + "encoded:%d(done:%b) "
                    + "pending:%d} "
                    + "muxing:%b(A:%d)",
                    mCopyAudio,
                    audioExtractedFrameCount, audioExtractorDone,
                    audioDecodedFrameCount, audioDecoderDone,
                    audioEncodedFrameCount, audioEncoderDone,
                    pendingAudioDecoderOutputBufferIndex,
                    muxing, outputAudioTrack));

            // Extract audio from file and feed to decoder.
            // Do not extract audio if we have determined the output format but we are not yet
            // ready to mux the frames.
            while (mCopyAudio && !audioExtractorDone
                    && (encoderOutputAudioFormat == null || muxing)) {
                int decoderInputBufferIndex = audioDecoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (decoderInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    Log.d(TAG, "no audio decoder input buffer");
                    break;
                }
                    Log.d(TAG, "audio decoder: returned input buffer: " + decoderInputBufferIndex);

                ByteBuffer decoderInputBuffer = audioDecoderInputBuffers[decoderInputBufferIndex];
                int size = audioExtractor.readSampleData(decoderInputBuffer, 0);
                long presentationTime = audioExtractor.getSampleTime();
                Log.d(TAG, "audio extractor: returned buffer of size " + size);
                Log.d(TAG, "audio extractor: returned buffer for time " + presentationTime);

                if (size >= 0) {
                    audioDecoder.queueInputBuffer(
                            decoderInputBufferIndex,
                            0,
                            size,
                            presentationTime,
                            audioExtractor.getSampleFlags());
                }
                audioExtractorDone = !audioExtractor.advance();
                if (audioExtractorDone) {
                    if (VERBOSE) Log.d(TAG, "audio extractor: EOS");
                    audioDecoder.queueInputBuffer(
                            decoderInputBufferIndex,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }
                audioExtractedFrameCount++;
                // We extracted a frame, let's try something else next.
                break;
            }

            // Poll output frames from the audio decoder.
            // Do not poll if we already have a pending buffer to feed to the encoder.
            while (mCopyAudio && !audioDecoderDone && pendingAudioDecoderOutputBufferIndex == -1
                    && (encoderOutputAudioFormat == null || muxing)) {
                int decoderOutputBufferIndex =
                        audioDecoder.dequeueOutputBuffer(
                                audioDecoderOutputBufferInfo, TIMEOUT_USEC);
                if (decoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (VERBOSE) Log.d(TAG, "no audio decoder output buffer");
                    break;
                }
                if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    if (VERBOSE) Log.d(TAG, "audio decoder: output buffers changed");
                    audioDecoderOutputBuffers = audioDecoder.getOutputBuffers();
                    break;
                }
                if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    decoderOutputAudioFormat = audioDecoder.getOutputFormat();
                    if (VERBOSE) {
                        Log.d(TAG, "audio decoder: output format changed: "
                                + decoderOutputAudioFormat);
                    }
                    break;
                }
                if (VERBOSE) {
                    Log.d(TAG, "audio decoder: returned output buffer: "
                            + decoderOutputBufferIndex);
                }
                if (VERBOSE) {
                    Log.d(TAG, "audio decoder: returned buffer of size "
                            + audioDecoderOutputBufferInfo.size);
                }
                ByteBuffer decoderOutputBuffer =
                        audioDecoderOutputBuffers[decoderOutputBufferIndex];
                if ((audioDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
                        != 0) {
                    if (VERBOSE) Log.d(TAG, "audio decoder: codec config buffer");
                    audioDecoder.releaseOutputBuffer(decoderOutputBufferIndex, false);
                    break;
                }
                if (VERBOSE) {
                    Log.d(TAG, "audio decoder: returned buffer for time "
                            + audioDecoderOutputBufferInfo.presentationTimeUs);
                }
                if (VERBOSE) {
                    Log.d(TAG, "audio decoder: output buffer is now pending: "
                            + pendingAudioDecoderOutputBufferIndex);
                }
                pendingAudioDecoderOutputBufferIndex = decoderOutputBufferIndex;
                audioDecodedFrameCount++;
                // We extracted a pending frame, let's try something else next.
                break;
            }
            // Feed the pending decoded audio buffer to the audio encoder.
            while (mCopyAudio && pendingAudioDecoderOutputBufferIndex != -1) {
                if (VERBOSE) {
                    Log.d(TAG, "audio decoder: attempting to process pending buffer: "
                            + pendingAudioDecoderOutputBufferIndex);
                }
                int encoderInputBufferIndex = audioEncoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (encoderInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (VERBOSE) Log.d(TAG, "no audio encoder input buffer");
                    break;
                }
                if (VERBOSE) {
                    Log.d(TAG, "audio encoder: returned input buffer: " + encoderInputBufferIndex);
                }
                ByteBuffer encoderInputBuffer = audioEncoderInputBuffers[encoderInputBufferIndex];
                int size = audioDecoderOutputBufferInfo.size;
                long presentationTime = audioDecoderOutputBufferInfo.presentationTimeUs;
                if (VERBOSE) {
                    Log.d(TAG, "audio decoder: processing pending buffer: "
                            + pendingAudioDecoderOutputBufferIndex);
                }
                if (VERBOSE) {
                    Log.d(TAG, "audio decoder: pending buffer of size " + size);
                    Log.d(TAG, "audio decoder: pending buffer for time " + presentationTime);
                }
                if (size >= 0) {
                    ByteBuffer decoderOutputBuffer =
                            audioDecoderOutputBuffers[pendingAudioDecoderOutputBufferIndex]
                                    .duplicate();
                    decoderOutputBuffer.position(audioDecoderOutputBufferInfo.offset);
                    decoderOutputBuffer.limit(audioDecoderOutputBufferInfo.offset + size);
                    encoderInputBuffer.position(0);
                    encoderInputBuffer.put(decoderOutputBuffer);
                    audioEncoder.queueInputBuffer(
                            encoderInputBufferIndex,
                            0,
                            size,
                            presentationTime,
                            audioDecoderOutputBufferInfo.flags);
                }
                audioDecoder.releaseOutputBuffer(pendingAudioDecoderOutputBufferIndex, false);
                pendingAudioDecoderOutputBufferIndex = -1;
                if ((audioDecoderOutputBufferInfo.flags
                        & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (VERBOSE) Log.d(TAG, "audio decoder: EOS");
                    audioDecoderDone = true;
                }
                // We enqueued a pending frame, let's try something else next.
                break;
            }

            // Poll frames from the audio encoder and send them to the muxer.
            while (mCopyAudio && !audioEncoderDone
                    && (encoderOutputAudioFormat == null || muxing)) {
                int encoderOutputBufferIndex = audioEncoder.dequeueOutputBuffer(
                        audioEncoderOutputBufferInfo, TIMEOUT_USEC);
                if (encoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (VERBOSE) Log.d(TAG, "no audio encoder output buffer");
                    break;
                }
                if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    if (VERBOSE) Log.d(TAG, "audio encoder: output buffers changed");
                    audioEncoderOutputBuffers = audioEncoder.getOutputBuffers();
                    break;
                }
                if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (VERBOSE) Log.d(TAG, "audio encoder: output format changed");
                    if (outputAudioTrack >= 0) {
                        Log.e(TAG, "audio encoder changed its output format again?");
                    }
                    encoderOutputAudioFormat = audioEncoder.getOutputFormat();
                    break;
                }
                if (VERBOSE) {
                    Log.d(TAG, "audio encoder: returned output buffer: "
                            + encoderOutputBufferIndex);
                    Log.d(TAG, "audio encoder: returned buffer of size "
                            + audioEncoderOutputBufferInfo.size);
                }
                ByteBuffer encoderOutputBuffer =
                        audioEncoderOutputBuffers[encoderOutputBufferIndex];
                if ((audioEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
                        != 0) {
                    if (VERBOSE) Log.d(TAG, "audio encoder: codec config buffer");
                    // Simply ignore codec config buffers.
                    audioEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
                    break;
                }
                if (VERBOSE) {
                    Log.d(TAG, "audio encoder: returned buffer for time "
                            + audioEncoderOutputBufferInfo.presentationTimeUs);
                }
                if (audioEncoderOutputBufferInfo.size != 0) {
                    muxer.writeSampleData(
                            outputAudioTrack, encoderOutputBuffer, audioEncoderOutputBufferInfo);
                }
                if ((audioEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        != 0) {
                    if (VERBOSE) Log.d(TAG, "audio encoder: EOS");
                    audioEncoderDone = true;
                }
                audioEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
                audioEncodedFrameCount++;
                // We enqueued an encoded frame, let's try something else next.
                break;
            }
            if (!muxing
                    && (!mCopyAudio || encoderOutputAudioFormat != null)) {
                if (mCopyAudio) {
                    Log.d(TAG, "muxer: adding audio track.");
                    outputAudioTrack = muxer.addTrack(encoderOutputAudioFormat);
                }
                Log.d(TAG, "muxer: starting");
                muxer.start();
                muxing = true;
            }
        }
        // Basic sanity checks.
        if (mCopyAudio) {
        }
        // TODO: Check the generated output file.
    }
    private static boolean isAudioFormat(MediaFormat format) {
        return getMimeTypeFor(format).startsWith("audio/");
    }
    private static String getMimeTypeFor(MediaFormat format) {
        return format.getString(MediaFormat.KEY_MIME);
    }
    /**
     * Returns the first codec capable of encoding the specified MIME type, or null if no match was
     * found.
     */
    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }
}