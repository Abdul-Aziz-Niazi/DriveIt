package com.aziz.drive_it.DriveUtils;

import android.Manifest;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import androidx.work.*;
import com.aziz.drive_it.DriveUtils.model.DIBackupDetails;
import com.aziz.drive_it.DriveUtils.model.DIFile;
import com.aziz.drive_it.DriveUtils.model.Frequency;
import com.aziz.drive_it.DriveUtils.utils.DIUtils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.reflect.TypeToken;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import pub.devrel.easypermissions.EasyPermissions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;


public class DriveIt {
    private static final String TAG = DriveIt.class.getSimpleName();
    private static DriveIt INSTANCE;
    private GoogleSignInClient signInClient;
    private DICallBack<String> signInCallBack;
    private Context context;
    private String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static DICallBack<DIBackupDetails> autoBackupCallback;

    private DriveIt() {
    }

    synchronized public static DriveIt getInstance() {
        if (INSTANCE == null)
            INSTANCE = new DriveIt();
        return INSTANCE;
    }

    public void signIn(Context context, DICallBack<String> signInCallBack) {
        signInClient = buildSignInClient(context);
        this.signInCallBack = signInCallBack;
        DIBackupDetailsRepository.getINSTANCE().setBackupChanged(true);
        if (context instanceof Activity) {
            ((Activity) context).startActivityForResult(signInClient.getSignInIntent(), DIConstants.REQUEST_BACKUP);
        }
    }

    public void setIcon(Context context, @DrawableRes int id) {
        Log.d(TAG, "setIcon: " + context.getApplicationInfo().packageName + " " + context.getResources().getResourceEntryName(id));
        Log.d(TAG, "setIcon: " + context.getResources().getIdentifier(context.getResources().getResourceName(id), context.getResources().getResourceTypeName(id), context.getPackageName()));
//        DIConstants.icon = BitmapFactory.decodeResource(context.getResources(), id);
        DIConstants.SMALL_ICON = id;
        DIBackupService.getInstance().setIcon(id);
        DIDeleteBackupService.getInstance().setIcon(id);
        DIRestoreService.getInstance().setIcon(id);
    }

    public void signIn(Fragment host, DICallBack<String> signInCallBack) {
        this.signInCallBack = signInCallBack;
        DIBackupDetailsRepository.getINSTANCE().setBackupChanged(true);
        if (host != null) {
            signInClient = buildSignInClient(host.getContext());
            host.startActivityForResult(signInClient.getSignInIntent(), DIConstants.REQUEST_BACKUP);
        }
    }

    public void silentSignIn(final Context context, final DICallBack<GoogleSignInAccount> callBack) {
        signInClient = buildSignInClient(context);
        final GoogleSignInAccount signedInAccount = GoogleSignIn.getLastSignedInAccount(context);
        if (signedInAccount != null) {
            new AccountTask(context, new DICallBack<String>() {
                @Override
                public void success(String DIObject) {
                    callBack.success(signedInAccount);
                }

                @Override
                public void failure(String error) {
                    callBack.failure(error);
                }
            }).execute(signedInAccount.getEmail());
        } else if (signInClient != null) {
            final Task<GoogleSignInAccount> googleSignInAccountTask = signInClient.silentSignIn();
            if (googleSignInAccountTask != null) {
                googleSignInAccountTask.addOnCompleteListener(new OnCompleteListener<GoogleSignInAccount>() {
                    @Override
                    public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                        if (task.isSuccessful()) {
                            final GoogleSignInAccount result = task.getResult();
                            if (result != null) {
                                new AccountTask(context, new DICallBack<String>() {
                                    @Override
                                    public void success(String DIObject) {
                                        callBack.success(result);
                                    }

                                    @Override
                                    public void failure(String error) {
                                        callBack.failure(error);
                                    }
                                }).execute(result.getEmail());
                            } else
                                callBack.failure("Silent Sign in failed");

                        } else {
                            callBack.failure("Silent Sign in failed");
                        }
                    }
                });
            }

        } else {
            callBack.failure("Silent Sign in failed");
        }
    }

    public void signOut() {
        if (signInClient != null) {
            signInClient.signOut();
        }
    }

    private GoogleSignInClient buildSignInClient(Context context) {

        Scope SCOPE_DRIVE = new Scope("https://www.googleapis.com/auth/drive");
        Scope SCOPE_METADATA = new Scope("https://www.googleapis.com/auth/drive.metadata");

        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(SCOPE_DRIVE, SCOPE_METADATA, Drive.SCOPE_APPFOLDER, Drive.SCOPE_FILE)
                .build();
        return GoogleSignIn.getClient(context, options);
    }

    public void onActivityResult(Context context, int requestCode, int resultCode, Intent data) {
        this.context = context;
        if (requestCode == 10) {
            Log.d(TAG, "onActivityResult: " + resultCode + " data " + data);
            return;
        }
        if (resultCode == Activity.RESULT_CANCELED || data == null)
            return;

        Task<GoogleSignInAccount> getAccountTask = GoogleSignIn.getSignedInAccountFromIntent(data);
        if (getAccountTask.getResult() == null)
            return;
        Log.d("RESULT:", "request: " + requestCode + " result:" + resultCode + " " + getAccountTask.getResult().getIdToken());
        new AccountTask(context, signInCallBack).execute(getAccountTask.getResult().getEmail());
    }

    public void startBackup(Activity activity, ArrayList<DIFile> files, DICallBack<DIFile> listener) {
        if (EasyPermissions.hasPermissions(activity, permissions)) {
            initiateBackup(activity, files, listener);
        } else {
            Log.e(TAG, "Process Stopped !! Storage permissions error.");
        }
    }

    public void createOrUpdateOne(DIFile file, DICallBack<DIFile> diFileDICallBack) {
        if (file.getFile().isDirectory()) {
            diFileDICallBack.failure("File is directory");
        }
        DIFileUpdater.update(file, diFileDICallBack);
    }

    private void initiateBackup(Activity activity, ArrayList<DIFile> files, DICallBack<DIFile> listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.startForegroundService(new Intent(activity, DIRestoreService.class));
        } else {
            activity.startService(new Intent(activity, DIRestoreService.class));
        }
        DIBackupService.getInstance().startBackup(activity, files, listener);
    }

    public void startRestore(Activity activity, DICallBack<DIFile> listener) {
        if (EasyPermissions.hasPermissions(activity, permissions)) {
            initiateRestore(activity, listener);
        } else {
            Log.e(TAG, "Process Stopped !! Storage permissions error.");
        }
    }

    private void initiateRestore(Activity activity, DICallBack<DIFile> listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.startForegroundService(new Intent(activity, DIRestoreService.class));
        } else {
            activity.startService(new Intent(activity, DIRestoreService.class));
        }
        DIRestoreService.getInstance().startRestore(activity, listener);
    }

    public void setAutoBackup(Frequency frequency, String[] filePaths, DICallBack<DIBackupDetails> autoBackupCallback) {
        setAutoBackup(frequency, filePaths, null, autoBackupCallback);
    }

    public void setAutoBackup(Frequency frequency, String[] filePaths, String[] desc, DICallBack<DIBackupDetails> autoBackupCallback) {
        DriveIt.autoBackupCallback = autoBackupCallback;
        Log.d(TAG, "setAutoBackup: Schedule for " + frequency.getFrequency() + " days");

        PeriodicWorkRequest.Builder workRequest = new PeriodicWorkRequest.Builder(DIAutoBackup.class, frequency.getFrequency(),
                frequency == Frequency.TEST ? TimeUnit.MINUTES : TimeUnit.DAYS, 5, TimeUnit.MINUTES);

        workRequest.addTag(DIConstants.BACKUP_SCHEDULE);
        Constraints workConstraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(false)
                .setRequiresStorageNotLow(false)
                .build();
        Data.Builder inputData = new Data.Builder();
        if (desc != null)
            inputData.putStringArray(DIConstants.DATA_DESC, desc);

        inputData.putStringArray(DIConstants.DATA, filePaths);
        if (DIConstants.SMALL_ICON != 0) {
            inputData.putInt(DIConstants.IC_NOTIFICATION, DIConstants.SMALL_ICON);
        }
        workRequest.setInputData(inputData.build());
        workRequest.setConstraints(workConstraints);
        WorkManager.getInstance().enqueueUniquePeriodicWork(DIConstants.BACKUP_SCHEDULE, ExistingPeriodicWorkPolicy.REPLACE, workRequest.build());
    }

    public void cancelAutoBackup() {
        WorkManager.getInstance().cancelAllWorkByTag(DIConstants.BACKUP_SCHEDULE);
    }

    public void getBackupSize(final DICallBack<DIBackupDetails> diCallBack) {
        DIBackupDetailsRepository.getINSTANCE().getBackupDetails(new DICallBack<DIBackupDetails>() {
            @Override
            public void success(DIBackupDetails details) {
                Log.d(TAG, "success: size:" + details.getBackupSize() + " time:" + details.getLastBackup());
                diCallBack.success(details);
            }

            @Override
            public void failure(String error) {
                diCallBack.failure(error);
                Log.d(TAG, "failure: " + error);
            }
        });
    }

    public void deleteBackup(Activity activity, boolean showNotification, DICallBack<DIFile> diFileDICallBack) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.startService(new Intent(activity, DIDeleteBackupService.class));
        } else {
            activity.startService(new Intent(activity, DIDeleteBackupService.class));
        }
        DIDeleteBackupService.getInstance().showNotification(showNotification);
        DIDeleteBackupService.getInstance().deleteAll(activity, diFileDICallBack);

    }

    public void writeFile(File sourceFile, String destFile) throws IOException {
        writeFile(sourceFile, new File(destFile));
    }

    public void writeFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.getParentFile().exists())
            destFile.getParentFile().mkdirs();

        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        try {
            FileChannel source = new FileInputStream(sourceFile).getChannel();
            FileChannel destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
            sourceFile.delete();
        } catch (Exception e) {

        }
    }

//    @Subscribe
//    public void autoBackupSuccessful(DIBackupDetails backupDetails) {
//        Log.d(TAG, "autoBackupSuccessful: " + backupDetails.getError());
//        if (backupDetails.getError() == null)
//            autoBackupCallback.success(backupDetails);
//        else
//            autoBackupCallback.failure(backupDetails.getError());
//    }

    static void autoBackupComplete(DIBackupDetails backupDetails) {
        if (autoBackupCallback != null)
            autoBackupCallback.success(backupDetails);
    }

    static void autoBackupFailed(DIBackupDetails backupDetails) {
        if (autoBackupCallback != null)
            autoBackupCallback.failure(backupDetails.getError());
    }
}
