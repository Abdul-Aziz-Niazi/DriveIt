package com.aziz.drive_it.DriveUtils;

import android.util.Log;
import com.aziz.drive_it.DriveUtils.model.DIFile;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;

class DIFileDeleter {

    private static final String TAG = DIFileDeleter.class.getSimpleName();

    static void deleteFile(final String fileId, final DICallBack<DIFile> diFileDICallBack) {
        Call<ResponseBody> call = DINetworkHandler.getWebService().delete(DIConstants.LIST_FILES + fileId, DINetworkHandler.getHeaders());

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "onResponse: DELETED " + fileId);
                    diFileDICallBack.success(new DIFile(fileId));
                } else {
                    try {
                        diFileDICallBack.failure(response.errorBody().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "onFailure: " + fileId + " " + t.getMessage());
                diFileDICallBack.failure("Failed to delete " + fileId + " : " + t.getMessage());
            }
        });
    }
}
