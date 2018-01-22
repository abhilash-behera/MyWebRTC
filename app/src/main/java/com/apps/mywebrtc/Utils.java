package com.apps.mywebrtc;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import java.net.URISyntaxException;

/**
 * Created by Abhilash on 07-11-2017
 */

public class Utils {
    private static Socket socket;
    public static final String USER_SHARED_PREF="com.apps.mywebrtc.user";
    public static final String USER_EMAIL="com.apps.mywebrtc.user.email";
    public static final String GET_USERS_LIST="get_users_list";
    public static final String USER_JOINED="user_joined";
    public static final String USER_OFFLINE="user_offline";
    public static final String USER_ONLINE="user_online";
    public static final String UPDATE_SOCKET_ID="update_socket_id";
    public static final String EVENT_GIVE_ME_CANDIDATES="event_give_me_candidates";
    public static final String EVENT_END_CALL="event_end_call";
    public static final String EVENT_BUSY="event_busy";
    public static final String CALL_TYPE="call_type";
    public static final String CALL_TYPE_INCOMING="call_type_incoming";
    public static final String CALL_TYPE_OUTGOING="call_type_outgoing";

    public static void hideKeyboard(Activity activity){
        View view=activity.getCurrentFocus();
        InputMethodManager imm=(InputMethodManager)activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if(view==null){
            view=new View(activity);
        }
        try{
            imm.hideSoftInputFromWindow(view.getWindowToken(),0);
        }catch (NullPointerException e){
            Log.d("mwr","Exception in hiding keyboard:"+e.toString());
        }
    }

    public static Socket getSocket(){
        if(socket==null||!socket.connected()){
            try{
                socket=IO.socket("https://web-rtc-server-abhilash.herokuapp.com");
                socket.connect();
            }catch (Exception e){
                Log.d("mwr","Exception in connecting socket: "+e.toString());
            }
        }

        return socket;
    }

    public static boolean isNetworkAvailable(Context context){
        ConnectivityManager connectivityManager=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo=connectivityManager.getActiveNetworkInfo();
        if(networkInfo==null||!networkInfo.isConnected()){
            return false;
        }else{
            return true;
        }
    }


}
