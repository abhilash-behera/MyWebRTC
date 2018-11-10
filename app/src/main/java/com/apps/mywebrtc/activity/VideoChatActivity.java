package com.apps.mywebrtc.activity;

import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.apps.mywebrtc.CustomSdpObserver;
import com.apps.mywebrtc.R;
import com.apps.mywebrtc.Utils;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Ack;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;


public class VideoChatActivity extends AppCompatActivity{
    private PeerConnectionFactory peerConnectionFactory;
    private MediaConstraints audioConstraints;
    private MediaConstraints videoConstraints;
    private MediaConstraints sdpConstraints;
    private VideoSource videoSource;
    private AudioSource audioSource;
    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;
    private SurfaceViewRenderer localVideoView;
    private SurfaceViewRenderer remoteVideoView;
    private VideoRenderer localRenderer;
    private VideoRenderer remoteRenderer;
    private PeerConnection localPeer;
    //private PeerConnection remotePeer;
    private Button btnEndCall;
    private Socket socket;
    private String to;
    private String from;
    private String call_type;
    private TextView txtStatus;
    private TextView candidateSent;
    private TextView candidateReceived;
    private int cSent=0;
    private int cReceived=0;
    private VideoCapturer videoCapturerAndroid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_chat);
        socket= Utils.getSocket();

        call_type=getIntent().getStringExtra(Utils.CALL_TYPE);
        Log.d("mwr","Call type: "+call_type);
        if(call_type.equalsIgnoreCase(Utils.CALL_TYPE_INCOMING)){
            to=getSharedPreferences(Utils.USER_SHARED_PREF,MODE_PRIVATE).getString(Utils.USER_EMAIL,"");
            from=getIntent().getStringExtra(Utils.USER_EMAIL);
        }else if(call_type.equalsIgnoreCase(Utils.CALL_TYPE_OUTGOING)){
            to=getIntent().getStringExtra(Utils.USER_EMAIL);
            from=getSharedPreferences(Utils.USER_SHARED_PREF,MODE_PRIVATE).getString(Utils.USER_EMAIL,"");
        }

        initializeViews();
        boostSpeakerVolume();
        initializeVideos();
        initializeCall();
    }

    private void boostSpeakerVolume() {
        AudioManager am=(AudioManager)getSystemService(Context.AUDIO_SERVICE);

        am.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                0
        );
    }

    private void initializeViews() {
        localVideoView=findViewById(R.id.localView);
        remoteVideoView=findViewById(R.id.remoteView);
        btnEndCall=findViewById(R.id.btnEndCall);
        txtStatus=findViewById(R.id.txtStatus);
        candidateReceived=findViewById(R.id.candidateReceived);
        candidateSent=findViewById(R.id.candidateSent);

        btnEndCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //remotePeer.close();
                //localPeer=null;
                //remotePeer=null;
                finish();
            }
        });
    }

    private void initializeVideos() {
        EglBase rootEglBase=EglBase.create();
        localVideoView.init(rootEglBase.getEglBaseContext(),null);
        remoteVideoView.init(rootEglBase.getEglBaseContext(),null);
        localVideoView.setZOrderMediaOverlay(true);
        remoteVideoView.setZOrderMediaOverlay(true);
    }

    private void initializeCall() {
        Log.d("mwr","initializing peer connection factory: "+PeerConnectionFactory.initializeAndroidGlobals(this,true,true,true));
        peerConnectionFactory=new PeerConnectionFactory();
        videoCapturerAndroid=getVideoCapturer();

        audioConstraints=new MediaConstraints();
        videoConstraints=new MediaConstraints();

        videoSource=peerConnectionFactory.createVideoSource(videoCapturerAndroid,videoConstraints);
        localVideoTrack=peerConnectionFactory.createVideoTrack("100",videoSource);

        audioSource=peerConnectionFactory.createAudioSource(audioConstraints);
        localAudioTrack=peerConnectionFactory.createAudioTrack("101",audioSource);
        localVideoView.setVisibility(View.VISIBLE);

        localRenderer=new VideoRenderer(localVideoView);
        localVideoTrack.addRenderer(localRenderer);

        makeCall();
    }

    private void makeCall() {
        try{
            JSONObject emailObj=new JSONObject();
            emailObj.put("email",getSharedPreferences(Utils.USER_SHARED_PREF,MODE_PRIVATE).getString(Utils.USER_EMAIL,""));
            socket.emit(Utils.EVENT_BUSY, emailObj, new Ack() {
                @Override
                public void call(final Object... args) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            boolean success=(boolean)args[0];
                            if(success){
                                Log.d("mwr","Busy event sent successfully");
                            }else{
                                Log.d("mwr","Error in sending busy event");
                            }
                        }
                    });
                }
            });
        }catch(Exception e){
            Log.d("mwr","Exception in sending busy event: "+e.toString());
        }
        Logging.enableTracing("logcat:", EnumSet.of(Logging.TraceLevel.TRACE_ALL), Logging.Severity.LS_SENSITIVE);
        List<PeerConnection.IceServer> iceServers=new ArrayList<>();
        PeerConnection.IceServer stun=new PeerConnection.IceServer("stun:stun.l.google.com:19302");
        PeerConnection.IceServer turn1=new PeerConnection.IceServer("turn:numb.viagenie.ca:3478?transport=tcp","abhilashbehera88@gmail.com","AbhiLima2@");
        PeerConnection.IceServer turn2=new PeerConnection.IceServer("turn:numb.viagenie.ca:3478?transport=udp","abhilashbehera88@gmail.com","AbhiLima2@");
        PeerConnection.IceServer turn3=new PeerConnection.IceServer("turn:numb.viagenie.ca:3478","abhilashbehera88@gmail.com","AbhiLima2@");
        PeerConnection.IceServer turn4=new PeerConnection.IceServer("turn:numb.viagenie.ca","abhilashbehera88@gmail.com","AbhiLima2@");
        iceServers.add(stun);
        iceServers.add(turn1);
        iceServers.add(turn2);
        iceServers.add(turn3);
        iceServers.add(turn4);

        sdpConstraints=new MediaConstraints();
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveAudio","true"));
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveVideo","true"));
        sdpConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));

        localPeer=peerConnectionFactory.createPeerConnection(iceServers, sdpConstraints, new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {

            }

            @Override
            public void onIceConnectionChange(final PeerConnection.IceConnectionState iceConnectionState) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("mwr","IceConnection change: "+iceConnectionState.name());
                        if(iceConnectionState== PeerConnection.IceConnectionState.CLOSED||
                                iceConnectionState== PeerConnection.IceConnectionState.FAILED||
                                iceConnectionState== PeerConnection.IceConnectionState.DISCONNECTED){
                            txtStatus.setTextColor(Color.RED);
                            remoteVideoView.setVisibility(View.GONE);
                            txtStatus.setText(iceConnectionState.name());
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    finish();
                                }
                            },2000);
                        }else{
                            txtStatus.setTextColor(Color.GREEN);
                            txtStatus.setText(iceConnectionState.name());
                        }
                    }
                });

            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {

            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

            }

            @Override
            public void onIceCandidate(final IceCandidate iceCandidate) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("mwr","Ice candidate changed: "+iceCandidate);
                        try{
                            JSONObject candidate=new JSONObject();
                            if(call_type.equalsIgnoreCase(Utils.CALL_TYPE_INCOMING)){
                                candidate.put("to",from);
                                candidate.put("from",to);
                            }else if(call_type.equalsIgnoreCase(Utils.CALL_TYPE_OUTGOING)){
                                candidate.put("to",to);
                                candidate.put("from",from);
                            }

                            candidate.put("sdp",iceCandidate.sdp);
                            candidate.put("sdpMid",iceCandidate.sdpMid);
                            candidate.put("sdpMLineIndex",iceCandidate.sdpMLineIndex);
                            Log.d("mwr","Ice candidate created: "+candidate.toString());
                            socket.emit("candidate", candidate, new Ack() {
                                @Override
                                public void call(final Object... args) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try{
                                                if((boolean)args[0]){
                                                    Log.d("mwr","Candidate sent successfully");
                                                    cSent+=1;
                                                    candidateSent.setText("Candidate Sent: "+cSent);
                                                }else{
                                                    Log.d("mwr","Candidate not sent. Something is wrong.");
                                                }
                                            }catch (Exception e){
                                                Log.d("mwr","exception in getting ack: "+e.toString());
                                            }

                                        }
                                    });
                                }
                            });
                        }catch (Exception e){
                            Log.d("mwr","Exception in sending candidate: "+e.toString());
                        }
                    }
                });

            }

            @Override
            public void onAddStream(final MediaStream mediaStream) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        gotRemoteStream(mediaStream);
                    }
                });

            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("mwr","Media stream removed");
                    }
                });
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {

            }

            @Override
            public void onRenegotiationNeeded() {

            }
        });


        if(call_type.equalsIgnoreCase(Utils.CALL_TYPE_INCOMING)){
            try{
                JSONObject obj=new JSONObject();
                obj.put("from",from);
                obj.put("to",to);
                socket.emit(Utils.EVENT_GIVE_ME_CANDIDATES,obj);
                Log.d("mwr","Get CANDIDATES request sent successfully");
            }catch(Exception e){
                Log.d("mwr","Exception in requesting candidates: "+e.toString());
            }
            Log.d("mwr","Waiting for candidates...");
        }


        socket.on("candidate", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("mwr","Candidate received");
                        try{
                            JSONObject candidate=(JSONObject)args[0];
                            Log.d("mwr","received new candidate:"+candidate.toString());
                            IceCandidate candidate1=new IceCandidate(candidate.getString("sdpMid"),candidate.getInt("sdpMLineIndex"),candidate.getString("sdp"));
                            localPeer.addIceCandidate(candidate1);
                            cReceived+=1;
                            candidateReceived.setText("Candidate Received: "+cReceived);
                        }catch (Exception e){
                            Log.d("mwr","Exception in receiving candidate: "+e.toString());
                        }
                    }
                });
            }
        });

        MediaStream stream=peerConnectionFactory.createLocalMediaStream("102");
        stream.addTrack(localAudioTrack);
        stream.addTrack(localVideoTrack);
        localPeer.addStream(stream);

        if(getIntent().getStringExtra(Utils.CALL_TYPE).equalsIgnoreCase(Utils.CALL_TYPE_OUTGOING)){
            localPeer.createOffer(new CustomSdpObserver("localCreateOffer",VideoChatActivity.this){
                @Override
                public void onCreateSuccess(final SessionDescription sessionDescription) {
                    super.onCreateSuccess(sessionDescription);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("mwr","session description : "+sessionDescription.description+" session type: "+sessionDescription.type);
                            localPeer.setLocalDescription(new CustomSdpObserver("localSetLocalDesc",VideoChatActivity.this),sessionDescription);
                            try{
                                JSONObject offerObject=new JSONObject();
                                offerObject.put("to",getIntent().getStringExtra(Utils.USER_EMAIL));
                                offerObject.put("from",getSharedPreferences(Utils.USER_SHARED_PREF,MODE_PRIVATE).getString(Utils.USER_EMAIL,""));
                                JSONObject sdp=new JSONObject();
                                sdp.put("description",sessionDescription.description);
                                sdp.put("type",sessionDescription.type);
                                offerObject.put("sdp",sdp);
                                Log.d("mwr","Offer object created: "+offerObject.toString());
                                socket.emit("offer", offerObject, new Ack() {
                                    @Override
                                    public void call(final Object... args) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                try{
                                                    if((boolean)args[0]){
                                                        Log.d("mwr","Offer sent successfully");
                                                    }else{
                                                        Log.d("mwr","Offer not sent");
                                                    }
                                                }catch (Exception e){
                                                    Log.d("mwr","Exception in getting ack: "+e.toString());
                                                }
                                            }
                                        });
                                    }
                                });

                                socket.on("answer", new Emitter.Listener() {
                                    @Override
                                    public void call(final Object... args) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    JSONObject answer=(JSONObject)args[0];
                                                    SessionDescription sdp=new SessionDescription(SessionDescription.Type.ANSWER,answer.getJSONObject("sdp").getString("description"));
                                                    Log.d("mwr","Answer sdp: "+sdp);
                                                    localPeer.setRemoteDescription(new CustomSdpObserver("local_set_remote_desc_for_offer",VideoChatActivity.this),sdp);
                                                }catch (Exception e){
                                                    Log.d("mwr","Exception in receiving answer: "+e.toString());
                                                }
                                            }
                                        });
                                    }
                                });
                            }catch (Exception e){
                                Log.d("mwr","Exception in creating offer: "+e.toString());
                            }

                    /*remotePeer.setRemoteDescription(new CustomSdpObserver("remoteSetRemoteDesc"),sessionDescription);
                    remotePeer.createAnswer(new CustomSdpObserver("remoteCreateOffer"){
                        @Override
                        public void onCreateSuccess(SessionDescription sessionDescription) {
                            super.onCreateSuccess(sessionDescription);
                            remotePeer.setLocalDescription(new CustomSdpObserver("remoteSetLocalDesc"),sessionDescription);
                            localPeer.setRemoteDescription(new CustomSdpObserver("localSetRemoteDesc"),sessionDescription);
                        }
                    },new MediaConstraints());*/
                        }
                    });

                }
            },sdpConstraints);
        }else{
            localPeer.setRemoteDescription(
                    new CustomSdpObserver("local_set_remote_desc_for_answer",VideoChatActivity.this){
                        @Override
                        public void onSetSuccess() {
                            super.onSetSuccess();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    localPeer.createAnswer(new CustomSdpObserver("local_create_answer",VideoChatActivity.this) {
                                        @Override
                                        public void onCreateSuccess(final SessionDescription sessionDescription) {
                                            super.onCreateSuccess(sessionDescription);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    localPeer.setLocalDescription(new CustomSdpObserver("local_set_local_description",VideoChatActivity.this), sessionDescription);
                                                    try {
                                                        JSONObject answer = new JSONObject();
                                                        answer.put("to", from);
                                                        answer.put("from", to);
                                                        JSONObject sdp = new JSONObject();
                                                        sdp.put("type", "answer");
                                                        sdp.put("description", sessionDescription.description);
                                                        answer.put("sdp", sdp);
                                                        socket.emit("answer", answer, new Ack() {
                                                            @Override
                                                            public void call(final Object... args) {
                                                                runOnUiThread(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        try {
                                                                            if ((boolean)args[0]) {
                                                                                Log.d("mwr", "answer sent successfully");
                                                                            } else {
                                                                                Log.d("mwr", "answer not sent");
                                                                            }
                                                                        } catch (Exception e) {
                                                                            Log.d("mwr", "Exception in sending answer: " + e.toString());
                                                                        }
                                                                    }
                                                                });
                                                            }
                                                        });

                                                    } catch (Exception e) {
                                                        Log.d("mwr","Exception in creating answer: "+e.toString());
                                                    }
                                                }
                                            });

                                        }
                                    }, new MediaConstraints());
                                }
                            });

                        }
                        }, new SessionDescription(SessionDescription.Type.OFFER,getIntent().getStringExtra("description")
                    )
            );
        }
    }

    private void gotRemoteStream(final MediaStream mediaStream) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try{
                    Log.d("mwr","Got remote mediaStream: "+mediaStream);
                    final VideoTrack videoTrack= mediaStream.videoTracks.getFirst();
                    final AudioTrack audioTrack= mediaStream.audioTracks.getFirst();
                    Log.d("mwr","Got videoTrack: "+videoTrack+" audioTrack: "+audioTrack);
                    remoteRenderer=new VideoRenderer(remoteVideoView);
                    remoteVideoView.setVisibility(View.VISIBLE);
                    videoTrack.addRenderer(remoteRenderer);
                }catch (Exception e){
                    Log.d("mwr","Exception in getting remote stream: "+e.toString());
                }
            }
        });
    }

    /*private void onIceCandidateReceived(PeerConnection peer, IceCandidate iceCandidate) {
        if(peer==localPeer){
            remotePeer.addIceCandidate(iceCandidate);
        }else{
            localPeer.addIceCandidate(iceCandidate);
        }
    }*/

    private VideoCapturer getVideoCapturer() {
        String[] cameraFacing={/*"front",*/"back"};
        int[] cameraIndex={0,1};
        int[] cameraOrientation={0,90,180,270};
        for(String facing:cameraFacing){
            for(int index:cameraIndex){
                for(int orientation:cameraOrientation){
                    String name="Camera "+index+", Facing "+facing+", Orientation "+orientation;
                    VideoCapturer capturer=VideoCapturer.create(name);
                    if(capturer!=null){
                        Log.d("mwr","Using camera: "+name);
                        return capturer;
                    }
                }
            }
        }
        throw new RuntimeException("Failed to open capture");
    }

    @Override
    protected void onResume() {
        super.onResume();
        //sendOnlineEvent();
    }

    private void sendOnlineEvent() {
        try{
            JSONObject emailObj=new JSONObject();
            emailObj.put("email",getSharedPreferences(Utils.USER_SHARED_PREF,MODE_PRIVATE).getString(Utils.USER_EMAIL,""));
            socket.emit(Utils.UPDATE_SOCKET_ID, emailObj, new Ack() {
                @Override
                public void call(final Object... args) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                if((boolean)args[0]){
                                    Log.d("mwr","Id updated successfully.");
                                }else{
                                    Log.d("mwr","Something went wrong while updating socket id.");
                                }
                            }catch (Exception e){
                                Log.d("mwr","Exception in getting ack for update id event: "+e.toString());
                            }
                        }
                    });
                }
            });
        }catch (Exception e){
            Log.d("mwr","Exception in sending update id event: "+e.toString());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try{
            JSONObject jsonObject=new JSONObject();
            jsonObject.put("to",to);
            jsonObject.put("from",from);
            socket.emit(Utils.EVENT_END_CALL,jsonObject);
            Log.d("mwr","End call event sent successfully.");
        }catch (Exception e){
            Log.d("mwr","Exception in sending end call event.");
        }
        if(localPeer!=null){
            localPeer.close();
            localPeer.dispose();
            videoCapturerAndroid.dispose();
            localPeer=null;
            videoCapturerAndroid=null;
        }else{
            Log.d("mwr","Local peer is null");
        }
    }
}
