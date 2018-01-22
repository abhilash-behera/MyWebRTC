package com.apps.mywebrtc.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.TextView;
import android.widget.Toast;

import com.apps.mywebrtc.R;
import com.apps.mywebrtc.Utils;
import com.apps.mywebrtc.adapter.AvailableUsersAdapter;
import com.apps.mywebrtc.retrofit.User;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Ack;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONArray;
import org.json.JSONObject;
import org.webrtc.SessionDescription;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE=1;
    private RecyclerView recyclerView;
    private Socket socket;
    private TextView txtStatus;
    private String email="";
    private ArrayList<User> users=new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setSubtitle("Connecting");
        email=getSharedPreferences(Utils.USER_SHARED_PREF,MODE_PRIVATE).getString(Utils.USER_EMAIL,"");
        checkPermissions();
    }

    private void checkPermissions(){
        String[] permissions={
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.MODIFY_AUDIO_SETTINGS
        };

        if(ContextCompat.checkSelfPermission(this,permissions[0])!= PackageManager.PERMISSION_GRANTED||
                ContextCompat.checkSelfPermission(this,permissions[1])!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,permissions,PERMISSION_REQUEST_CODE);
        }else{
            initializeViews();
        }
    }

    private void initializeViews() {
        recyclerView=findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        int resId = R.anim.layout_anim_slide_from_right;
        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(MainActivity.this, resId);
        recyclerView.setLayoutAnimation(animation);
        txtStatus=findViewById(R.id.txtStatus);

    }

    @Override
    protected void onResume() {
        super.onResume();
        connectToServer();
    }

    private void connectToServer() {
        if(Utils.isNetworkAvailable(MainActivity.this)){
            if(socket!=null){
                socket.off();
            }
            socket= Utils.getSocket();
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(final Object... args) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("mwr","Socket connected ");
                            notifyOtherSockets();
                        }
                    });
                }
            });

            socket.on(Socket.EVENT_RECONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    notifyOtherSockets();
                }
            });

            socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("mwr","Socket disconnected");
                            try{
                                Snackbar.make(txtStatus,"Server disconnected",Snackbar.LENGTH_INDEFINITE)
                                        .setActionTextColor(Color.RED)
                                        .setAction("Reconnect", new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                connectToServer();
                                            }
                                        }).show();
                            }catch (Exception e){
                                Log.d("mwr","Exception in creating snackbar: "+e.toString());
                            }
                        }
                    });
                }
            });

            socket.on(Socket.EVENT_RECONNECTING, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Snackbar.make(txtStatus,"Reconnecting to server",Snackbar.LENGTH_LONG).show();
                        }
                    });
                }
            });

            socket.on(Utils.USER_ONLINE, new Emitter.Listener() {
                @Override
                public void call(final Object... args) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("mwr","Got user online event");
                            for(int i=0;i<users.size();i++){
                                try{
                                    User user=users.get(i);
                                    if(user.getEmail().equalsIgnoreCase(((JSONObject)args[0]).getString("email"))){
                                        user.setStatus("online");
                                        recyclerView.setAdapter(new AvailableUsersAdapter(MainActivity.this,users));
                                        recyclerView.getAdapter().notifyDataSetChanged();
                                    }
                                }catch (Exception e){
                                    Log.d("mwr","Exception in changing user status: "+e.toString());
                                }
                            }
                        }
                    });

                }
            });

            socket.on(Utils.USER_OFFLINE, new Emitter.Listener() {
                @Override
                public void call(final Object... args) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("mwr","Got user offline event");
                            for(int i=0;i<users.size();i++){
                                try{
                                    User user=users.get(i);
                                    if(user.getEmail().equalsIgnoreCase(((JSONObject)args[0]).getString("email"))){
                                        user.setStatus("offline");
                                        recyclerView.setAdapter(new AvailableUsersAdapter(MainActivity.this,users));
                                        recyclerView.getAdapter().notifyDataSetChanged();
                                    }
                                }catch (Exception e){
                                    Log.d("mwr","Exception in changing user status: "+e.toString());
                                }
                            }
                        }
                    });

                }
            });

            socket.on(Utils.USER_JOINED, new Emitter.Listener() {
                @Override
                public void call(final Object... args) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                JSONArray array=(JSONArray)args[0];
                                JSONObject obj=array.getJSONObject(0);
                                User user=new User(obj.getString("email"),obj.getString("status"),obj.getString("socketId"));
                                users.add(user);
                                recyclerView.setAdapter(new AvailableUsersAdapter(MainActivity.this,users));
                                recyclerView.getAdapter().notifyDataSetChanged();
                            }catch (Exception e){
                                Log.d("mwr","Exception in getting user joined event:"+e.toString());
                            }
                        }
                    });
                }
            });

            socket.on("offer", new Emitter.Listener() {
                @Override
                public void call(final Object... args) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                JSONObject offer=(JSONObject)args[0];
                                JSONObject sdp=offer.getJSONObject("sdp");
                                SessionDescription.Type type= SessionDescription.Type.OFFER;
                                SessionDescription sessionDescription=new SessionDescription(type,sdp.getString("description"));
                                Log.d("mwr","Offer object received: "+offer.toString());

                                Intent intent=new Intent(MainActivity.this,IncomingCallActivity.class);
                                intent.putExtra(Utils.USER_EMAIL,offer.getString("from"));
                                intent.putExtra("description",sessionDescription.description);
                                intent.putExtra(Utils.CALL_TYPE,Utils.CALL_TYPE_INCOMING);
                                startActivity(intent);
                            }catch (Exception e){
                                Log.d("mwr","Exception in getting offer: "+e.toString());
                            }
                        }
                    });
                }
            });
            notifyOtherSockets();

        }else{
            Snackbar.make(findViewById(R.id.txtStatus),"No internet connection",Snackbar.LENGTH_INDEFINITE)
                    .setAction("Retry", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            connectToServer();
                        }
                    })
                    .setActionTextColor(Color.RED)
                    .show();
        }

    }

    private void notifyOtherSockets() {
        try{
            JSONObject emailObj=new JSONObject("{email:"+email+"}");
            socket.emit(Utils.USER_ONLINE, emailObj, new Ack() {
                @Override
                public void call(final Object... args) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                if((boolean)args[0]){
                                    Log.d("mwr","online event sent successfully");
                                }else{
                                    Log.d("mwr","online event sending failed.");
                                }
                                txtStatus.setText("Getting available users list...");
                                getSupportActionBar().setSubtitle("Available");
                            }catch (Exception e){
                                Log.d("mwr","Exception in sending online event: "+e.toString());
                            }
                        }
                    });
                }
            });

            socket.emit(Utils.GET_USERS_LIST, emailObj, new Ack() {
                @Override
                public void call(final Object... args) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                JSONArray usersArray=(JSONArray)args[0];
                                Log.d("mwr","Got users: "+usersArray.toString());
                                if(usersArray.length()==0){
                                    txtStatus.setText("Sorry, no users are available at this moment");
                                }else{
                                    users=new ArrayList<>();
                                    for(int i=0;i<usersArray.length();i++){

                                        JSONObject userObj=usersArray.getJSONObject(i);
                                        User user=new User(userObj.getString("email"),userObj.getString("status"),userObj.getString("socketId"));
                                        users.add(user);
                                    }
                                    txtStatus.setVisibility(View.GONE);
                                    recyclerView.setAdapter(new AvailableUsersAdapter(MainActivity.this,users));
                                    recyclerView.getAdapter().notifyDataSetChanged();
                                }
                            }catch (Exception e){
                                Log.d("mwr","Exception in getting users: "+e.toString());
                            }
                        }
                    });
                }
            });
        }catch (Exception e){
            Log.d("mwr","Exception in parsing json: "+e.toString());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==PERMISSION_REQUEST_CODE){
            if(grantResults[0]==PackageManager.PERMISSION_GRANTED&&
                    grantResults[1]==PackageManager.PERMISSION_GRANTED){
                initializeViews();
            }else{
                AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Permissions Required");
                builder.setMessage("You need to provide the permissions to use this app.");
                builder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                });
                builder.setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        checkPermissions();
                    }
                });
                builder.show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try{
            JSONObject emailObj=new JSONObject("{email:"+email+"}");
            socket.emit(Utils.USER_OFFLINE, emailObj, new Ack() {
                @Override
                public void call(Object... args) {
                    try{
                        Log.d("mwr","User now offline: "+((JSONObject)args[0]).getString("email"));
                    }catch (Exception e){
                        Log.d("mwr","Exception in calling user offline event: "+e.toString());
                    }
                }
            });
        }catch(Exception e){
            Log.d("mwr","Exception in sending offline event: "+e.toString());
        }

        socket.off();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        socket.off();
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Hang on")
                .setMessage("Are you sure you want to close this app?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .setNegativeButton("No",null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.logout){
            AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Hang on")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            getSharedPreferences(Utils.USER_SHARED_PREF,MODE_PRIVATE)
                                    .edit()
                                    .remove(Utils.USER_EMAIL)
                                    .apply();

                            Intent intent=new Intent(MainActivity.this,LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    })
                    .setNegativeButton("No",null)
                    .show();

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }
}
