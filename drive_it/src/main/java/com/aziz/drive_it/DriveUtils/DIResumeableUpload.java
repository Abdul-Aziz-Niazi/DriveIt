package com.aziz.drive_it.DriveUtils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.aziz.drive_it.DriveUtils.model.DIFile;
import com.aziz.drive_it.R;
import com.google.gson.Gson;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.FileInputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static android.app.Notification.EXTRA_NOTIFICATION_ID;

public class DIResumeableUpload {

    private static final String TAG = DIResumeableUpload.class.getSimpleName();
    private static final String DATA_UPLOAD = "UPLOAD";
    private static final int NOTIFICATION_ID = 908;
    private final Context context;
    private ArrayList<DIFile> fileArrayList;
    private final DICallBack<DIFile> fileDICallBack;
    SharedPreferences preferences;
    int count;
    private NotificationCompat.Builder notificationCompat;
    private int icon;
    private NotificationManager notificationManager;
    private int chunkSizeInMb = 5;

    public DIResumeableUpload(Context context, ArrayList<DIFile> fileArrayList, DICallBack<DIFile> fileDICallBack) {
        this.context = context;
        this.fileArrayList = fileArrayList;
        this.fileDICallBack = fileDICallBack;
        preferences = context.getSharedPreferences(DIConstants.PREF_KEY, Context.MODE_PRIVATE);
        count = 0;
        ArrayList<DIFile> diFileArrayList = filterOutFiles(fileArrayList);
        this.fileArrayList = diFileArrayList;
        if (diFileArrayList.size() != 0) {
            createMetadata(diFileArrayList.get(count));
        }
    }

    private ArrayList<DIFile> filterOutFiles(ArrayList<DIFile> fileArrayList) {
        ArrayList<DIFile> diFileArrayList = new ArrayList<>();
        for (DIFile file : fileArrayList) {
            if (file.getFile() != null && file.getFile().exists()) {
                diFileArrayList.add(file);
            }
        }
        return diFileArrayList;
    }


    private void createMetadata(final DIFile diFile) {
        craeteUpdatedNotification();
        Log.d(TAG, "createMetadata: requesting " + diFile.getName());
        HashMap<String, String> headers = new HashMap<>(DINetworkHandler.getHeaders());
        headers.put("X-Upload-Content-Length", "" + diFile.getFile().length());
        Gson gson = new Gson();
        headers.put("Content-Length", "" + gson.toJson(diFile).getBytes().length);
        headers.put("Content-Type", "application/json; charset=UTF-8");
        diFile.setParents(Collections.singletonList("appDataFolder"));
        Call<Void> post = DINetworkHandler.getInstance().getWebService().postResumable(DIConstants.UPLD_FILES_RESUMABLE, headers, diFile);
        post.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d(TAG, "onResponse: " + response.code() + " " + response.headers() + " " + response.message());
                try {
                    if (response.isSuccessful()) {
                        String sessionUri = response.headers().get("location");
                        preferences.edit().putString(DIConstants.UPLD_POS, sessionUri).apply();
                        Log.d(TAG, "onResponse: Success " + sessionUri + " ");
                        continueUpload(sessionUri, diFile, 0);
                    } else {
                        Log.d(TAG, "onUnsuccessful: " + response.errorBody().string());
                        handleErrorResponse(response.errorBody().string());
                    }
                } catch (Exception e) {
                    Log.d(TAG, "onException: " + e.getMessage());
                }
            }

            private void handleErrorResponse(String string) {
                Log.d(TAG, "handleErrorResponse: " + string);
                pauseNotification();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                handleErrorResponse(t.getMessage());
            }
        });
    }

    private void continueUpload(final String sessionUri, final DIFile diFile, final int chunkStart) throws Exception {
        HashMap<String, String> headers = new HashMap<>(DINetworkHandler.getHeaders());
        headers.put("Content-Type", URLConnection.guessContentTypeFromName(diFile.getFile().getName()));
        long uploadedBytes = chunkSizeInMb * 1024 * 1024;
        preferences.edit().putInt(DIConstants.CHNK_START, chunkStart).apply();
        if (chunkStart + uploadedBytes > diFile.getFile().length()) {
            uploadedBytes = (int) diFile.getFile().length() - chunkStart;
        }
        headers.put("Content-Length", "" + uploadedBytes);
        headers.put("Content-Range", "bytes " + chunkStart + "-" + (chunkStart + uploadedBytes - 1) + "/" + diFile.getFile().length());
        byte[] uploadArrayBytes = new byte[(int) uploadedBytes];
        FileInputStream fileInputStream = new FileInputStream(diFile.getFile());
        fileInputStream.getChannel().position(chunkStart);
        if (fileInputStream.read(uploadArrayBytes, 0, (int) uploadedBytes) == -1) {
            return;
        }
        fileInputStream.close();
        final RequestBody requestBody = RequestBody.create(MediaType.get("application/octet-stream"), uploadArrayBytes);
        Call<Void> post = DINetworkHandler.getInstance().getWebService().post(sessionUri, headers, requestBody);
        post.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                try {
                    if (response.code() == 308) {
                        //Incomplete-Continue
                        String range = response.headers().get("range");
                        Log.d(TAG, "onContinue: " + range);
                        int start = Integer.parseInt(range.split("-")[1]);
                        preferences.edit().putInt(DIConstants.CHNK_START, start).apply();
                        continueUpload(sessionUri, diFile, start);

                    } else if (response.code() == 200) {
                        //Completed-Choose Next File
                        Log.d(TAG, "onComplete: " + diFile.getName() + " " + count + " " + fileArrayList.size() + " " + response.headers());
                        if (fileArrayList.size() > count + 1) {
                            createMetadata(fileArrayList.get(++count));
                        } else {
                            stopNotification();
                            DIBackupDetailsRepository.getINSTANCE().setBackupChanged(true);
                        }

                    } else if (!response.isSuccessful()) {
                        //Error-Pause Right there
                        handleErrorResponse(response.code() + " " + response.errorBody().string());
                        pauseNotification();


                    }
                } catch (Exception e) {
                    Log.d(TAG, "onException: " + e.getMessage());
                }
            }

            private void handleErrorResponse(String error) {
                Log.d(TAG, "handleErrorResponse: " + error);
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                handleErrorResponse(t.getMessage());
            }
        });
    }


    public void craeteUpdatedNotification() {
        if (notificationCompat != null) {
            notificationCompat.setProgress(fileArrayList.size(), count, false);
            notificationCompat.setContentText(Math.round((float) count * 100) / (fileArrayList.size()) + "%");
            notificationManager.notify(NOTIFICATION_ID, notificationCompat.build());
            return;
        }
        notificationCompat = new NotificationCompat
                .Builder(context, DATA_UPLOAD)
                .setContentTitle("Backup in progress")
                .setProgress(10, 0, true)
                .setSound(null)
                .setOngoing(true)
                .setSmallIcon(icon == 0 ? R.drawable.ic_backup_drive : icon)
                .setContentText(Math.round((float) count * 100) / (fileArrayList.size()) + "%");
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(DATA_UPLOAD) != null)
                notificationManager.deleteNotificationChannel(DATA_UPLOAD);

            notificationManager.createNotificationChannel(new NotificationChannel(DATA_UPLOAD, DATA_UPLOAD, NotificationManager.IMPORTANCE_LOW));
        }
        Notification notification = notificationCompat.build();
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    public void pauseNotification() {
        notificationCompat = new NotificationCompat
                .Builder(context, DATA_UPLOAD)
                .setContentTitle("")
                .setProgress(10, 0, true)
                .setSound(null)
                .setOngoing(true)
                .setSmallIcon(icon == 0 ? R.drawable.ic_backup_drive : icon)
                .setContentText("Backup error, try again later");
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(DATA_UPLOAD) != null)
                notificationManager.deleteNotificationChannel(DATA_UPLOAD);

            notificationManager.createNotificationChannel(new NotificationChannel(DATA_UPLOAD, DATA_UPLOAD, NotificationManager.IMPORTANCE_LOW));
        }
        Notification notification = notificationCompat.build();
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    public void stopNotification() {
        notificationCompat = new NotificationCompat
                .Builder(context, DATA_UPLOAD)
                .setContentTitle("Backup Complete")
                .setProgress(10, 0, true)
                .setSound(null)
                .setOngoing(true)
                .setSmallIcon(icon == 0 ? R.drawable.ic_backup_drive : icon)
                .setContentText("");
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(DATA_UPLOAD) != null)
                notificationManager.deleteNotificationChannel(DATA_UPLOAD);

            notificationManager.createNotificationChannel(new NotificationChannel(DATA_UPLOAD, DATA_UPLOAD, NotificationManager.IMPORTANCE_LOW));
        }
        Notification notification = notificationCompat.build();
        notificationManager.cancel(NOTIFICATION_ID);
    }


// String sessionUri = response.headers().get("location");
//                        preferences.edit().putString(DIConstants.UPLD_POS, sessionUri).apply();
//                        if (count + 1 < fileArrayList.size()) {
//                            continueUpload(fileArrayList.get(++count));
//                        }

}
