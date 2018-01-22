package com.apps.mywebrtc.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.apps.mywebrtc.R;
import com.apps.mywebrtc.Utils;
import com.apps.mywebrtc.retrofit.ApiClient;
import com.apps.mywebrtc.retrofit.ApiInterface;
import com.apps.mywebrtc.retrofit.SignupRequest;
import com.apps.mywebrtc.retrofit.SignupResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {
    private EditText txtEmail;
    private EditText txtPassword;
    private EditText txtConfirmPassword;
    private Button btnCreateAccount;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        initializeViews();
    }

    private void initializeViews() {
        txtEmail=findViewById(R.id.txtEmail);
        txtPassword=findViewById(R.id.txtPassword);
        txtConfirmPassword=findViewById(R.id.txtConfirmPassword);
        btnCreateAccount=findViewById(R.id.btnCreateAccount);
        progressDialog=new ProgressDialog(SignupActivity.this);

        btnCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(txtEmail.getText().toString().isEmpty()){
                    txtEmail.setError("email required");
                }else{
                    if(txtPassword.getText().toString().isEmpty()){
                        txtPassword.setError("password required");
                    }else{
                        if(txtConfirmPassword.getText().toString().isEmpty()){
                            txtConfirmPassword.setError("retype password");
                        }else{
                            if(txtPassword.getText().toString().compareTo(txtConfirmPassword.getText().toString())!=0){
                                txtConfirmPassword.setError("passwords do not match");
                            }else{
                                signup(txtEmail.getText().toString(),txtPassword.getText().toString());
                            }
                        }
                    }
                }
            }
        });
    }

    private void signup(String email, String password) {
        progressDialog.setTitle("Creating Account");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        Utils.hideKeyboard(SignupActivity.this);
        ApiInterface service=ApiClient.getRetrofit().create(ApiInterface.class);
        Call<SignupResponse> call=service.signup(new SignupRequest(email,password));
        call.enqueue(new Callback<SignupResponse>() {
            @Override
            public void onResponse(Call<SignupResponse> call, final Response<SignupResponse> response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        if(response.isSuccessful()){
                            if(response.body().isSuccess()){
                                Toast.makeText(SignupActivity.this, "Account created successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            }else{
                                Toast.makeText(SignupActivity.this, response.body().getData(), Toast.LENGTH_SHORT).show();
                            }
                        }else{
                            Toast.makeText(SignupActivity.this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
                        }

                    }
                });
            }

            @Override
            public void onFailure(Call<SignupResponse> call, Throwable t) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(SignupActivity.this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                });
            }
        });
    }
}
