package com.apps.mywebrtc.retrofit;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * Created by Abhilash on 07-11-2017
 */

public interface ApiInterface {
    @Headers("Content-Type: application/json")
    @POST("/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    @Headers("Content-Type: application/json")
    @POST("/signup")
    Call<SignupResponse> signup(@Body SignupRequest signupRequest);

}
