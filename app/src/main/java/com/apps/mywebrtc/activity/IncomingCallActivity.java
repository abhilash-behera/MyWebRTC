package com.apps.mywebrtc.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.apps.mywebrtc.R;
import com.apps.mywebrtc.Utils;

import org.webrtc.SessionDescription;

public class IncomingCallActivity extends AppCompatActivity {
    private TextView txtEmail;
    private Button btnReceiveCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);
        initializeViews();
    }

    private void initializeViews() {
        txtEmail=findViewById(R.id.txtEmail);
        btnReceiveCall=findViewById(R.id.btnReceiveCall);
        final Intent intent=getIntent();
        txtEmail.setText(intent.getStringExtra(Utils.USER_EMAIL));
        btnReceiveCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent1=new Intent(IncomingCallActivity.this,VideoChatActivity.class);
                intent1.putExtra(Utils.CALL_TYPE,Utils.CALL_TYPE_INCOMING);
                intent1.putExtra("description",intent.getStringExtra("description"));
                intent1.putExtra(Utils.USER_EMAIL,intent.getStringExtra(Utils.USER_EMAIL));
                startActivity(intent1);
                finish();
            }
        });
    }
}
