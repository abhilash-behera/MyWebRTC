package com.apps.mywebrtc;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

/**
 * Created by Abhilash on 11-11-2017
 */

public class CustomSdpObserver implements SdpObserver {


    private String tag = "mwr";
    private Context context;

    public CustomSdpObserver(String logTag, Context context) {
        this.tag = this.tag + " " + logTag;
        this.context=context;
    }


    @Override
    public void onCreateSuccess(final SessionDescription sessionDescription) {
        ((Activity)context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(tag, "onCreateSuccess() called with: sessionDescription = [" + sessionDescription + "]");
            }
        });

    }

    @Override
    public void onSetSuccess() {
        ((Activity)context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(tag, "onSetSuccess() called");
            }
        });

    }

    @Override
    public void onCreateFailure(final String s) {
        ((Activity)context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(tag, "onCreateFailure() called with: s = [" + s + "]");
            }
        });

    }

    @Override
    public void onSetFailure(final String s) {
        ((Activity)context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(tag, "onSetFailure() called with: s = [" + s + "]");
            }
        });

    }

}