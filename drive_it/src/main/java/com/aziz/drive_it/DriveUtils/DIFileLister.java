package com.aziz.drive_it.DriveUtils;

import android.util.Log;
import com.aziz.drive_it.DriveUtils.model.DIFile;
import com.aziz.drive_it.DriveUtils.utils.DIUtils;
import com.google.gson.reflect.TypeToken;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class DIFileLister {
    private static final String TAG = DIFileLister.class.getSimpleName();

    public static void list(final DICallBack<ArrayList<DIFile>> callBack) {
        DINetworkHandler.getInstance().getWebService()
                .get(DIConstants.LIST_FILES + "?spaces=appDataFolder&fields=files(id,name,modifiedTime,size,description,mimeType)",
                        DINetworkHandler.getHeaders())
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        try {
                            if (response.isSuccessful()) {
                                ArrayList<DIFile> diFileArrayList = new ArrayList<>();
                                String success = response.body().string();
                                Log.d(TAG, "onResponse: " + success);
                                JSONObject responseObject = new JSONObject(success);
                                JSONArray data = responseObject.getJSONArray("files");
                                Type typeToken = new TypeToken<ArrayList<DIFile>>() {
                                }.getType();
                                diFileArrayList = DIUtils.getGson().fromJson(data.toString(), typeToken);
                                callBack.success(diFileArrayList);
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

}
