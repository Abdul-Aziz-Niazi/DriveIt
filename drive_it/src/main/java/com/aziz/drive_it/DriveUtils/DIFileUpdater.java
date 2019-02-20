package com.aziz.drive_it.DriveUtils;

import android.util.Log;
import com.aziz.drive_it.DriveUtils.model.DIFile;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class DIFileUpdater {
    private static final String TAG = DIFileUpdater.class.getSimpleName();
    private static boolean createNew = true;

    public static void update(final DIFile file, final DICallBack<DIFile> callBack) {
        DIFileLister.list(new DICallBack<ArrayList<DIFile>>() {
            @Override
            public void success(ArrayList<DIFile> fileArrayList) {
                createNew = true;
                for (DIFile diFile : fileArrayList) {
                    if (file.getName().equalsIgnoreCase("" + diFile.getName())) {
                        createNew = false;
                        updateFile(file, diFile, callBack);
                        break;
                    }
                }

                if (createNew) {
                    Log.d(TAG, "creating new file : " + file.getName());
                    DIFile diFile = new DIFile();
                    diFile.setName("" + file.getName());
                    diFile.setParents(Collections.singletonList("appDataFolder"));
                    Call<DIFile> post = DINetworkHandler.getInstance().getWebService().post(DIConstants.LIST_FILES, DINetworkHandler.getHeaders(), diFile);
                    post.enqueue(new Callback<DIFile>() {
                        @Override
                        public void onResponse(Call<DIFile> call, Response<DIFile> response) {
                            updateFile(file, response.body(), callBack);
                        }

                        @Override
                        public void onFailure(Call<DIFile> call, Throwable t) {
                            callBack.failure("" + t.getMessage());
                        }
                    });
                }
            }


            @Override
            public void failure(String error) {
                Log.d(TAG, "failure: " + error);
                callBack.failure(error);
            }
        });


    }

    private static void updateFile(DIFile file, DIFile diFile, final DICallBack<DIFile> callBack) {
        DIFileUploader.uploadFile(diFile, file, new DICallBack<DIFile>() {
            @Override
            public void success(DIFile file) {
                Log.d(TAG, "success: " + file.getName() + " updated");
                callBack.success(file);
            }

            @Override
            public void failure(String error) {
                Log.d(TAG, "failure: " + error);
                callBack.failure(error);
            }
        });
    }

}
