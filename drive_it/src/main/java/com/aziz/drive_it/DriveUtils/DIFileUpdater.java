package com.aziz.drive_it.DriveUtils;

import android.util.Log;
import com.aziz.drive_it.DriveUtils.model.DIFile;
import retrofit2.Call;

import java.io.File;
import java.util.ArrayList;

public class DIFileUpdater {
    private static final String TAG = DIFileUpdater.class.getSimpleName();

    public static void update(final File file, final DICallBack<DIFile> callBack) {
        DIFileLister.list(new DICallBack<ArrayList<DIFile>>() {
            @Override
            public void success(ArrayList<DIFile> fileArrayList) {
                for (DIFile diFile : fileArrayList) {
                    if (file.getName().equalsIgnoreCase("" + diFile.getName())) {
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
                        break;
                    }
                }
            }

            @Override
            public void failure(String error) {
                Log.d(TAG, "failure: " + error);
                callBack.failure(error);
            }
        });
    }

}
