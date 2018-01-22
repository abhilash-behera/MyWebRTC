package com.apps.mywebrtc.retrofit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Abhilash on 07-11-2017
 */

public class ApiClient {
    public static final String BASE_URL="https://web-rtc-server-abhilash.herokuapp.com";
    private static Retrofit retrofit=null;

    public static Retrofit getRetrofit(){
        if(retrofit==null){
            HttpLoggingInterceptor interceptor=new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient.Builder httpClient=new OkHttpClient.Builder();
            httpClient.addInterceptor(interceptor);
            retrofit=new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(httpClient.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
