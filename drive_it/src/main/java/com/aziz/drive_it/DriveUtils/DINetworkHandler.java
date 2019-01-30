package com.aziz.drive_it.DriveUtils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import com.google.gson.reflect.TypeToken;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

class DINetworkHandler {
    private static final String TAG = DINetworkHandler.class.getSimpleName();
    private static String AuthToken = "";
    private static DINetworkHandler INSTANCE;
    private static WebService webService = null;
    private static HashMap<String, String> headers = new HashMap<>();

    private DINetworkHandler() {
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(0, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        Retrofit retrofit;
        retrofit = new Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(DIConstants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        webService = retrofit.create(WebService.class);
    }

    public static DINetworkHandler getInstance() {
        if (INSTANCE == null)
            INSTANCE = new DINetworkHandler();

        return INSTANCE;
    }

    static HashMap<String, String> getHeaders() {
        headers.put("Authorization", getAuthToken());
        headers.put("Accept", "application/json");
        Log.d(TAG, "getHeaders: " + headers);
        return headers;
    }

    public WebService getWebService() {
        return webService;
    }


    private static String getAuthToken() {
        return AuthToken;
    }

    void setAuthToken(String authToken) {
        AuthToken = authToken;
    }


}
