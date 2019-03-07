package com.aziz.drive_it.DriveUtils;

import android.util.Log;
import com.aziz.drive_it.DriveUtils.model.DIBackupDetails;
import com.aziz.drive_it.DriveUtils.model.DIFile;
import com.aziz.drive_it.DriveUtils.utils.DIUtils;
import com.google.gson.reflect.TypeToken;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class DIBackupDetailsRepository {
    private static final String TAG = DIBackupDetailsRepository.class.getSimpleName();
    private static DIBackupDetailsRepository INSTANCE;
    private boolean backupChanged = true;
    private int count = 0;
    private DIBackupDetails backupDetails;
    private int total;
    private Long totalSize = 0L;
    private ArrayList<Date> time = new ArrayList<>();
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);

    public static DIBackupDetailsRepository getINSTANCE() {
        if (INSTANCE == null)
            INSTANCE = new DIBackupDetailsRepository();
        return INSTANCE;
    }

    public void setBackupChanged(boolean backupChanged) {
        this.backupChanged = backupChanged;
    }


    public void getBackupDetails(final DICallBack<DIBackupDetails> callBack) {
        if (backupDetails != null && backupDetails.getError() == null && !backupChanged) {
            callBack.success(backupDetails);
            return;
        }
        DIFileLister.list(new DICallBack<ArrayList<DIFile>>() {
            @Override
            public void success(ArrayList<DIFile> fileArrayList) {
                long size = 0;
                long timestamp = 0;
                for (DIFile file : fileArrayList) {
                    try {
                        size += file.getSize();
                        timestamp = format.parse(file.getModifiedTime()).getTime();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                DIBackupDetails diBackupDetails = new DIBackupDetails();
                diBackupDetails.setBackupSize(size);
                diBackupDetails.setLastBackup(timestamp);
                backupDetails = diBackupDetails;
                backupChanged = false;
                callBack.success(diBackupDetails);
            }

            @Override
            public void failure(String error) {
                callBack.failure(error);
                Log.d(TAG, "failure: " + error);
                backupDetails = new DIBackupDetails();
                backupDetails.setError(error);
            }
        });
    }

}
