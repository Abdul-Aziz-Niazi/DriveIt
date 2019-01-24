package com.aziz.drive_it.DriveUtils;

import android.util.Log;
import okhttp3.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class DIFileUploader {

    private static final String TAG = DIFileUploader.class.getSimpleName();

    public static void uploadFile(DIFile diFile, File file, final DICallBack<DIFile> callBack) {
//        MultipartBody.Part part = MultipartBody.Part.create(RequestBody.create(MediaType.parse("text/*"), file));
        RequestBody body = RequestBody.create(MediaType.parse("image/*"), file);
        HashMap<String, String> headers = new HashMap<>(DINetworkHandler.getInstance().getHeaders());
        Log.d(TAG, "uploadFile: headers " + headers);
        retrofit2.Call<DIFile> call = DINetworkHandler.getInstance().getWebService()
                .patch(DIConstants.UPLD_FILES + diFile.getId()+"?uploadType=media",
                        DINetworkHandler.getInstance().getHeaders(), body);
        call.enqueue(new Callback<DIFile>() {
            @Override
            public void onResponse(Call<DIFile> call, Response<DIFile> response) {
                if (response.isSuccessful()) {
                    DIFile responseFile = response.body();
                    Log.d(TAG, "onResponse: " + responseFile);
                    callBack.success(responseFile);
                } else {
                    try {
                        callBack.failure("Upload error: " + response.errorBody().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<DIFile> call, Throwable t) {
                callBack.failure("Upload failure " + t.getMessage());
            }
        });
    }
}