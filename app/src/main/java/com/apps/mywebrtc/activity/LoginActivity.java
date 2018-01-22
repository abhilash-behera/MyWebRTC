package com.apps.mywebrtc.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.apps.mywebrtc.R;
import com.apps.mywebrtc.Utils;
import com.apps.mywebrtc.retrofit.ApiClient;
import com.apps.mywebrtc.retrofit.ApiInterface;
import com.apps.mywebrtc.retrofit.LoginRequest;
import com.apps.mywebrtc.retrofit.LoginResponse;
import com.github.nkzawa.socketio.client.Ack;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONArray;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private EditText txtEmail;
    private EditText txtPassword;
    private TextView txtCreateAccount;
    private Button btnLogin;
    private ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if(getSharedPreferences(Utils.USER_SHARED_PREF,MODE_PRIVATE).contains(Utils.USER_EMAIL)){
            Intent intent=new Intent(LoginActivity.this,MainActivity.class);
            startActivity(intent);
            finish();
        }
        initializeViews();
    }

    private void initializeViews() {
        txtEmail=findViewById(R.id.txtEmail);
        txtPassword=findViewById(R.id.txtPassword);
        txtCreateAccount=findViewById(R.id.txtCreateAccount);
        btnLogin=findViewById(R.id.btnLogin);
        progressDialog=new ProgressDialog(LoginActivity.this);

        txtCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(LoginActivity.this,SignupActivity.class);
                startActivity(intent);
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(txtEmail.getText().toString().isEmpty()){
                    txtEmail.setError("email required");
                }else{
                    if(txtPassword.getText().toString().isEmpty()){
                        txtPassword.setError("password required");
                    }else{
                        login(txtEmail.getText().toString(),txtPassword.getText().toString());
                    }
                }
            }
        });
    }

    private void login(String email, String password) {
        Utils.hideKeyboard(LoginActivity.this);
        progressDialog.setTitle("Authenticating");
        progressDialog.setMessage("please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        ApiInterface service= ApiClient.getRetrofit().create(ApiInterface.class);
        Call<LoginResponse> call=service.login(new LoginRequest(email,password));
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, final Response<LoginResponse> response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        if(response.isSuccessful()){
                            if(response.body().isSuccess()){
                                SharedPreferences sharedPreferences=getSharedPreferences(Utils.USER_SHARED_PREF,MODE_PRIVATE);
                                sharedPreferences.edit().putString(Utils.USER_EMAIL,response.body().getData())
                                        .apply();
                                Intent intent=new Intent(LoginActivity.this,MainActivity.class);
                                startActivity(intent);
                                finish();
                            }else{
                                Toast.makeText(LoginActivity.this, response.body().getData(), Toast.LENGTH_SHORT).show();
                            }
                        }else{
                            Toast.makeText(LoginActivity.this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
                        }

                    }
                });
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        Toast.makeText(LoginActivity.this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
