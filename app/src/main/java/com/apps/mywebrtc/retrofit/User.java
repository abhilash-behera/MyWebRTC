package com.apps.mywebrtc.retrofit;

import java.io.Serializable;

/**
 * Created by Abhilash on 09-11-2017
 */

public class User implements Serializable {
    private String email;
    private String status;
    private String socketId;

    public void setEmail(String email) {
        this.email = email;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setSocketId(String socketId) {
        this.socketId = socketId;
    }

    public User(String email, String status, String socketId) {

        this.email = email;
        this.status = status;
        this.socketId = socketId;
    }

    public String getEmail() {

        return email;
    }

    public String getStatus() {
        return status;
    }

    public String getSocketId() {
        return socketId;
    }
}
