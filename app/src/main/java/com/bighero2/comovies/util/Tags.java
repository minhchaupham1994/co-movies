package com.bighero2.comovies.util;

/**
 * Created by tuan on 17/04/2016.
 */
public class Tags {
    public static final String REQUEST_S = "<REQ>";
    public static final String REQUEST_E = "</REQ>";
    public static final String ANSWER_S = "<ANS>";
    public static final String ANSWER_E = "</ANS>";
    public static final String READY_S = "<REA>";
    public static final String READY_E = "</REA>";
    public static final String PLAY_S = "<PLA>";
    public static final String PLAY_E = "</PLA>";
    public static final String PAUSE_S = "<PAU>";
    public static final String PAUSE_E = "</PAU>";
    public static final String RESUME_S = "<RES>";
    public static final String RESUME_E = "</RES>";
    public static final String SEEK_S = "<SEK>";
    public static final String SEEK_E = "</SEK>";
    public static final String STOP_S = "<STO>";
    public static final String STOP_E = "</STO>";

    public static final int TAG_START_LENGTH = 5;
    public static final int TAG_END_LENGTH = 6;

    public static final int TYPE_REQ = 1;
    public static final int TYPE_PLAY = 2;
    public static final int TYPE_PAUSE = 3;
    public static final int TYPE_STOP = 4;
    public static final int TYPE_ANSWER = 5;
    public static final int TYPE_READY = 6;
    public static final int TYPE_RESUME = 7;
    public static final int TYPE_SEEK = 8;


    public static final int typeOfMessage(byte[] message, int length) {
        byte[] start = new byte[TAG_START_LENGTH];
        byte[] end = new byte[TAG_END_LENGTH];

        System.arraycopy(message, 0, start, 0, start.length);
        System.arraycopy(message, length - start.length - 1, end, 0, end.length);

        String startS = bytes2String(start, start.length);
        String endS = bytes2String(end, end.length);

        if (startS.equals(REQUEST_S) && endS.equals(REQUEST_E)) {
            return TYPE_REQ;
        } else if (startS.equals(PLAY_S) && endS.equals(PLAY_E)) {
            return TYPE_PLAY;
        } else if (startS.equals(PAUSE_S) && endS.equals(PAUSE_E)) {
            return TYPE_PAUSE;
        } else if (startS.equals(STOP_S) && endS.equals(STOP_E)) {
            return TYPE_STOP;
        } else if (startS.equals(ANSWER_S) && endS.equals(ANSWER_E)) {
            return TYPE_ANSWER;
        } else if (startS.equals(READY_S) && endS.equals(READY_E)) {
            return TYPE_READY;
        } else if (startS.equals(RESUME_S) && endS.equals(RESUME_E)) {
            return TYPE_RESUME;
        } else if (startS.equals(SEEK_S) && endS.equals(SEEK_E)) {
            return TYPE_SEEK;
        } else {
            return -1;
        }
    }

    public static final String contentOfMessage(byte[] message, int length) {
        byte[] content = new byte[length - TAG_START_LENGTH - TAG_END_LENGTH];
        System.arraycopy(message, TAG_START_LENGTH, content, 0, content.length);

        return bytes2String(content, content.length);
    }

    public static final String bytes2String(byte[] bytes, int length) {
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < length; i++) {
            builder.append((char) bytes[i]);
        }

        return builder.toString();
    }

    public static final byte[] makeAMessage(int type, String content) {
        String start = "";
        String end = "";
        switch (type) {
            case TYPE_REQ:
                start = REQUEST_S;
                end = REQUEST_E;
                break;
            case TYPE_ANSWER:
                start = ANSWER_S;
                end = ANSWER_E;
                break;
            case TYPE_READY:
                start = READY_S;
                end = READY_E;
                break;
            case TYPE_PLAY:
                start = PLAY_S;
                end = PLAY_E;
                break;
            case TYPE_PAUSE:
                start = PAUSE_S;
                end = PAUSE_E;
                break;
            case TYPE_RESUME:
                start = RESUME_S;
                end = RESUME_E;
                break;
            case TYPE_SEEK:
                start = SEEK_S;
                end = SEEK_E;
                break;
            case TYPE_STOP:
                start = STOP_S;
                end = STOP_E;
                break;
            default:
                break;
        }

        byte[] contentBytes = content.getBytes();
        byte[] startBytes = start.getBytes();
        byte[] endBytes = end.getBytes();

        byte[] result = new byte[startBytes.length + contentBytes.length + endBytes.length];
        System.arraycopy(startBytes, 0, result, 0, startBytes.length);
        System.arraycopy(contentBytes, 0, result, startBytes.length, contentBytes.length);
        System.arraycopy(endBytes, 0, result, startBytes.length + contentBytes.length, endBytes.length);

        return result;
    }
}
