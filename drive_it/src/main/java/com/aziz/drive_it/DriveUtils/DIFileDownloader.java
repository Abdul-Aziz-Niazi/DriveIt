package com.aziz.drive_it.DriveUtils;


import android.util.Log;
import com.aziz.drive_it.DriveUtils.model.DIFile;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

class DIFileDownloader {

    private static final String TAG = DIFileDownloader.class.getSimpleName();

    public static void downloadFile(final String path, DIFile file, final DICallBack<File> callBack) {
        final String name = file.getName();
        Call<ResponseBody> call = DINetworkHandler.getInstance().getWebService().get(DIConstants.LIST_FILES + file.getId() + "?alt=media", DINetworkHandler.getInstance().getHeaders());
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        byte[] bytes = response.body().bytes();
                        Log.d(TAG, "onResponse: Success ");
                        File file = writeFileToPath(bytes, path, name);
                        callBack.success(file);
                    } else {
                        String error = response.errorBody().string();
                        Log.d(TAG, "onResponse: Failure " + error);
                        callBack.failure("error " + error);
                    }
                } catch (Exception e) {
                    callBack.failure("exception " + e.getMessage());
                    Log.d(TAG, "onResponse: Failure E " + e.getMessage());

                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "onResponse: Failure T " + t.getMessage());
                callBack.failure("exception " + t.getMessage());
            }
        });
    }
    private static File writeFileToPath(byte[] bytes, String path, String name) throws IOException {

        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, name);
        if (!file.exists()) {
            file.createNewFile();
            Log.d(TAG, "onResponse: NEWFILE");
        }
        FileOutputStream outputStream = new FileOutputStream(file);
        outputStream.write(bytes);
        outputStream.close();
        outputStream.flush();
        return file;
    }
}
