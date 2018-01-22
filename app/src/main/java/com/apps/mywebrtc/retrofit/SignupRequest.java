package com.apps.mywebrtc.retrofit;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created by Abhilash on 07-11-2017
 */

public class SignupRequest implements Serializable {
    @SerializedName("email")
    @Expose
    private String email;

    @SerializedName("password")
    @Expose
    private String password;

    public SignupRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
