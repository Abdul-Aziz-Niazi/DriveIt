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
    private DIBackupDetailsRepository INSTANCE;
    private boolean backupChanged;
    private int count = 0;
    private int total;
    private Long totalSize;
    private ArrayList<Date> time;
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
    private boolean firstSuccess = true;

    public DIBackupDetailsRepository getINSTANCE() {
        if (INSTANCE == null)
            INSTANCE = new DIBackupDetailsRepository();
        return INSTANCE;
    }

    public void setBackupChanged(boolean backupChanged) {
        this.backupChanged = backupChanged;
    }


    public void getBackupDetails(final DICallBack<DIBackupDetails> callBack) {
        DIBackupDetails backupDetails = new DIBackupDetails();
        Call<ResponseBody> call = DINetworkHandler.getInstance().getWebService()
                .get(DIConstants.LIST_FILES + "?spaces=appDataFolder&OrderBy=timeModified desc",
                        DINetworkHandler.getInstance().getHeaders());
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful()) {
                        handleSuccess(response);
                    } else {
                        String failure = response.errorBody().string();
                        callBack.failure("listing-error " + failure);
                    }
                } catch (Exception e) {
                    callBack.failure("listing-exception " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                callBack.failure("listing-failure " + t.getMessage());
            }
        });
    }

    private void handleSuccess(Response<ResponseBody> response, final DICallBack<DIBackupDetails> listener) throws IOException, JSONException {
        ArrayList<DIFile> diFileArrayList = new ArrayList<>();

        String success = response.body().string();
        Log.d(TAG, "onResponse: " + success);

        JSONObject responseObject = new JSONObject(success);
        JSONArray data = responseObject.getJSONArray("files");
        Type typeToken = new TypeToken<ArrayList<DIFile>>() {
        }.getType();
        diFileArrayList = DIUtils.getGson().fromJson(data.toString(), typeToken);
        total = diFileArrayList.size();
        count = 0;

        for (DIFile file : diFileArrayList) {
            collectDetails(file.getId(), new DICallBack<DIFile>() {
                @Override
                public void success(DIFile file) {
                    count++;
                    totalSize += file.getSize();
                    try {
                        time.add(format.parse(file.getModifiedTime()));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    if (count == total) {
                        DIBackupDetails diBackupDetails = new DIBackupDetails();
                        diBackupDetails.setBackupSize(totalSize);
                        diBackupDetails.setLastBackup(time.get(0).getTime());
                        listener.success();
                    }

                }

                @Override
                public void failure(String error) {
                    count++;
                    if (count == total) {

                    }
                }
            });
        }
    }

    private void collectDetails(String id, final DICallBack<DIFile> diCallBack) {

        Call<DIFile> call = DINetworkHandler.getInstance().getWebService().getFile(DIConstants.LIST_FILES + id + "?fields=size,modifiedTime", DINetworkHandler.getInstance().getHeaders());

        call.enqueue(new Callback<DIFile>() {
            @Override
            public void onResponse(Call<DIFile> call, Response<DIFile> response) {
                if (!response.isSuccessful()) {
                    DIFile file = response.body();
                    Log.d(TAG, "onResponse: " + file.toString());
                    diCallBack.success(file);
                } else {
                    try {
                        diCallBack.failure("file size error " + response.errorBody().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }


            @Override
            public void onFailure(Call<DIFile> call, Throwable t) {
                diCallBack.failure("file size error " + t.getMessage());
                Log.d(TAG, "onFailure: file size error " + t.getMessage());
            }
        });
    }
}
