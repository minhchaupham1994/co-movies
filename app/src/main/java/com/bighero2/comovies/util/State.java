package com.bighero2.comovies.util;

/**
 * Created by Tuan on 20/03/2016.
 */
public class State {
    public int currentTime;
    public boolean isPlaying;

    public State(int time, boolean playing) {
        currentTime = time;
        isPlaying = playing;
    }
}
