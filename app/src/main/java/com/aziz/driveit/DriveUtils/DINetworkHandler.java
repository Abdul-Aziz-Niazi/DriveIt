package com.aziz.driveit.DriveUtils;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.util.Log;
import com.aziz.driveit.DriveUtils.utils.DIUtils;
import com.google.gson.reflect.TypeToken;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class DINetworkHandler {
    private static final String TAG = DINetworkHandler.class.getSimpleName();
    private String AuthToken = "";
    private static DINetworkHandler INSTANCE;
    private final WebService webService;
    HashMap<String, String> headers = new HashMap<>();
    private Activity activity;
    private DICallBack callBack;
    private Context context;
    private int count = 0;

    public static DINetworkHandler getInstance() {
        if (INSTANCE == null)
            INSTANCE = new DINetworkHandler();

        return INSTANCE;
    }

    public HashMap<String, String> getHeaders() {
        headers.put("Authorization", getAuthToken());
        headers.put("Accept", "application/json");

//        headers.put("clientId", DIConstants.ClIENT_ID);
//        headers.put("apiKey", DIConstants.API_KEY);
        Log.d(TAG, "getHeaders: " + headers);
        return headers;
    }

    public WebService getWebService() {
        return webService;
    }

    private DINetworkHandler() {
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(0, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        Retrofit retrofit;
        retrofit = new Retrofit.Builder().client(okHttpClient).baseUrl(DIConstants.BASE_URL).build();
        webService = retrofit.create(WebService.class);
    }

    String getAuthToken() {
        return AuthToken;
    }

    void setAuthToken(String authToken) {
        AuthToken = authToken;
    }


    public void startBackup(ArrayList<File> fileArrayList, final DICallBack diCallBack) {
    }




}
