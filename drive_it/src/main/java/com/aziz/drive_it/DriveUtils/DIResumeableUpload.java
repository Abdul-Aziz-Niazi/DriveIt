package com.aziz.drive_it.DriveUtils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
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

import static android.app.Notification.FLAG_FOREGROUND_SERVICE;

public class DIResumeableUpload {

    private static final String TAG = DIResumeableUpload.class.getSimpleName();
    private static final String DATA_UPLOAD = "UPLOAD";
    private static DIResumeableUpload instance;
    private DIBackupService context;
    private ArrayList<DIFile> fileArrayList;
    SharedPreferences preferences;
    int count;
    private NotificationCompat.Builder notificationCompat;
    private int icon;
    private NotificationManager notificationManager;
    private int chunkSizeInMb = 2;
    private Intent retryOnErrorIntent;

    public static DIResumeableUpload getInstance() {
        if (instance == null)
            instance = new DIResumeableUpload();
        return instance;
    }

    private DIResumeableUpload() {

    }

    public void setResumeData(DIBackupService context, ArrayList<DIFile> fileArrayList) {
        this.context = context;
        this.fileArrayList = fileArrayList;
        retryOnErrorIntent = new Intent(context, ButtonReceiver.class);
        preferences = context.getSharedPreferences(DIConstants.PREF_KEY, Context.MODE_PRIVATE);
        count = 0;
        ArrayList<DIFile> diFileArrayList = filterOutFiles(fileArrayList);
        this.fileArrayList = diFileArrayList;
        Log.d(TAG, "setResumeData: " + diFileArrayList);
        if (diFileArrayList.size() != 0) {
            createMetadata(diFileArrayList.get(count));
        }
    }

    public void setCount(int count) {
        this.count = count;
    }

    public ArrayList<DIFile> getFileArrayList() {
        return fileArrayList;
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


    public void createMetadata(final DIFile diFile) {
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
                Log.d(TAG, "onResponse: " + response.code() + " " + diFile.getName() + "  ==>" + count);
                try {
                    if (response.isSuccessful()) {
                        String sessionUri = response.headers().get("location");
                        preferences.edit().putString(DIConstants.UPLD_POS, sessionUri).apply();
                        preferences.edit().putInt(DIConstants.FILE_NUM, count).apply();
                        Log.d(TAG, "onResponse: Success " + " " + diFile.getName());
                        continueUpload(sessionUri, diFile, 0);
                    } else {
                        String error = response.errorBody().string();
                        Log.d(TAG, "onUnsuccessful: " + error);
                        handleErrorResponse(error);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "onException: " + e.getMessage());
                }
            }

            private void handleErrorResponse(String string) {
                Log.d(TAG, "handleErrorResponse: " + string);
                DriveIt.backupFileFailed(string);
                retryOnErrorIntent.putExtra(DIConstants.ERROR_DETAILS, string);
                pauseNotification();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                handleErrorResponse("ON_FAILURE__CREATE_METADATA " + t.getMessage());
            }
        });
    }

    public void continueUpload(final String sessionUri, final DIFile diFile, final int chunkStart) {
        try {
            HashMap<String, String> headers = new HashMap<>(DINetworkHandler.getHeaders());
            headers.put("Content-Type", URLConnection.guessContentTypeFromName(diFile.getFile().getName()));
            Log.d(TAG, "continueUpload: headers-content-type"+diFile.getName());
            long uploadedBytes = chunkSizeInMb * 1024 * 1024;
            preferences.edit().putInt(DIConstants.CHNK_START, chunkStart).apply();
            if (chunkStart + uploadedBytes > diFile.getFile().length()) {
                uploadedBytes = (int) diFile.getFile().length() - chunkStart;
            }
            Log.d(TAG, "continueUpload: headers-chunk-calculation"+diFile.getName());
            headers.put("Content-Length", "" + uploadedBytes);
            headers.put("Content-Range", "bytes " + chunkStart + "-" + (chunkStart + uploadedBytes - 1) + "/" + diFile.getFile().length());
            Log.d(TAG, "continueUpload: headers-content-range"+diFile.getName());
            byte[] uploadArrayBytes = createUploadBytes(chunkStart, diFile, uploadedBytes);
            Log.d(TAG, "continueUpload: headers-create-upload-bytes"+diFile.getName());
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
                            craeteUpdatedNotification();
                            int start = Integer.parseInt(range.split("-")[1]);
                            preferences.edit().putInt(DIConstants.CHNK_START, start).apply();
                            preferences.edit().putInt(DIConstants.FILE_NUM, count).apply();
                            continueUpload(sessionUri, diFile, start);

                        } else if (response.code() == 200) {
                            //Completed-Choose Next File
                            Log.d(TAG, "onComplete: " + diFile.getName() + " " + count + " " + fileArrayList.size());
                            DriveIt.backupFileComplete(diFile);
                            if (fileArrayList.size() > count + 1) {
                                preferences.edit().putInt(DIConstants.FILE_NUM, count).apply();
                                createMetadata(fileArrayList.get(++count));
                            } else {
                                stopNotification();
                                DIBackupDetailsRepository.getINSTANCE().setBackupChanged(true);
                            }

                        } else if (!response.isSuccessful()) {
                            //Error-Pause Right there
                            handleErrorResponse(response.code() + " " + response.errorBody().string());
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "onException: " + e.getMessage());
                    }
                }

                private void handleErrorResponse(String error) {
                    Log.d(TAG, "handleErrorResponse: " + error);
                    DriveIt.backupFileFailed(error);
                    retryOnErrorIntent.putExtra(DIConstants.ERROR_DETAILS, error);
                    pauseNotification();
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    handleErrorResponse("ON_FAILURE__UPLOAD " + t.getMessage());
                }
            });
        } catch (Exception e) {
            pauseNotification();
        }
    }

    public byte[] createUploadBytes(int chunk, DIFile diFile, long uploadedBytes) {
        try {
            byte[] uploadArrayBytes = new byte[(int) uploadedBytes];
            FileInputStream fileInputStream = new FileInputStream(diFile.getFile());
            fileInputStream.getChannel().position(chunk);
            if (fileInputStream.read(uploadArrayBytes, 0, (int) uploadedBytes) == -1) {
                return uploadArrayBytes;
            }
            fileInputStream.close();
            return uploadArrayBytes;
        } catch (Exception e) {
            return new byte[(int) uploadedBytes];
        }
    }


    public void craeteUpdatedNotification() {
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(100);

        if (notificationCompat != null && notificationCompat.build().category == null) {
            notificationCompat.setProgress(fileArrayList.size(), count, false);
            notificationCompat.setContentText(Math.round((float) count * 100) / (fileArrayList.size()) + "%");
            notificationCompat.mActions.clear();
            notificationCompat.setSmallIcon(R.drawable.notificaiton_tello_icon);

            notificationManager.notify(DIConstants.NOTIFICATION_ID, notificationCompat.build());
            return;
        }
        notificationCompat = new NotificationCompat
                .Builder(context, DATA_UPLOAD)
                .setContentTitle("Backup in progress")
                .setProgress(10, 0, true)
                .setSound(null)
                .setOngoing(true)
                .setSmallIcon(R.drawable.notificaiton_tello_icon)

                .setContentText(Math.round((float) count * 100) / (fileArrayList.size()) + "%");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(DATA_UPLOAD) != null)
                notificationManager.deleteNotificationChannel(DATA_UPLOAD);

            notificationManager.createNotificationChannel(new NotificationChannel(DATA_UPLOAD, DATA_UPLOAD, NotificationManager.IMPORTANCE_LOW));
        }
        Notification notification = notificationCompat.build();
        notificationManager.notify(DIConstants.NOTIFICATION_ID, notification);
    }

    public void pauseNotification() {
        retryOnErrorIntent.setAction("drive_it.retry");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 90, retryOnErrorIntent, PendingIntent.FLAG_ONE_SHOT);
        Intent dismissIntent = new Intent(context, ButtonReceiver.class);
        dismissIntent.setAction("drive_it.cancel");
        PendingIntent dismiss = PendingIntent.getBroadcast(context, 90, dismissIntent, PendingIntent.FLAG_ONE_SHOT);

        notificationCompat = new NotificationCompat
                .Builder(context, DATA_UPLOAD)
                .setContentTitle("")
                .setSound(null)
                .setSmallIcon(R.drawable.notificaiton_tello_icon)

                .addAction(0, "Retry", pendingIntent)
                .addAction(0, "Cancel", dismiss)
                .setContentText("Backup error, try again later");
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(DATA_UPLOAD) != null)
                notificationManager.deleteNotificationChannel(DATA_UPLOAD);

            notificationManager.createNotificationChannel(new NotificationChannel(DATA_UPLOAD, DATA_UPLOAD, NotificationManager.IMPORTANCE_LOW));
        }
        Notification notification = notificationCompat.build();
        notificationManager.notify(DIConstants.NOTIFICATION_ID, notification);
    }


    public void stopNotification() {
        Intent dismissIntent = new Intent(context, ButtonReceiver.class);
        dismissIntent.setAction("drive_it.cancel");
        PendingIntent dismiss = PendingIntent.getBroadcast(context, 90, dismissIntent, PendingIntent.FLAG_ONE_SHOT);
        if (notificationCompat != null) {
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(DIConstants.NOTIFICATION_ID);
        }
        notificationCompat = new NotificationCompat
                .Builder(context, DATA_UPLOAD)
                .setContentTitle("Backup Complete")
                .setSound(null)
                .setCategory("final")
                .setSmallIcon(R.drawable.notificaiton_tello_icon)

                .setContentText("");
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(DATA_UPLOAD) != null)
                notificationManager.deleteNotificationChannel(DATA_UPLOAD);

            notificationManager.createNotificationChannel(new NotificationChannel(DATA_UPLOAD, DATA_UPLOAD, NotificationManager.IMPORTANCE_LOW));
        }
        Notification notification = notificationCompat.build();
        notificationManager.notify(100, notification);
        context.stopForeground(true);
        notificationCompat = null;

    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public int getIcon() {
        return icon;
    }
}
