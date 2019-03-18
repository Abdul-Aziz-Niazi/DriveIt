package com.aziz.drive_it.DriveUtils;

import android.Manifest;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
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
import com.google.gson.Gson;
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


public class DriveIt  {
    private static final String TAG = DriveIt.class.getSimpleName();
    private static DriveIt INSTANCE;
    private GoogleSignInClient signInClient;
    private DICallBack<String> signInCallBack;
    private Context context;
    private String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static DICallBack<DIBackupDetails> autoBackupCallback;
    private DIFile found = null;
    private static DICallBack<DIFile> backupCallback;
    private static DICallBack<DIFile> restoreCallback;

    private DriveIt() {

    }

    public Context getContext() {
        return context;
    }

    synchronized public static DriveIt getInstance() {
        if (INSTANCE == null)
            INSTANCE = new DriveIt();
        return INSTANCE;
    }

    public void signIn(Context context, DICallBack<String> signInCallBack) {
        signInClient = buildSignInClient(context);
        this.context = context;
        this.signInCallBack = signInCallBack;
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
        DIResumeableUpload.getInstance().setIcon(id);
    }

    public void signIn(Fragment host, DICallBack<String> signInCallBack) {
        if (host != null) {
            this.context = host.getContext();
            this.signInCallBack = signInCallBack;
            DIBackupDetailsRepository.getINSTANCE().setBackupChanged(true);
            signInClient = buildSignInClient(host.getContext());
            host.startActivityForResult(signInClient.getSignInIntent(), DIConstants.REQUEST_BACKUP);
        }
    }

    public void silentSignIn(final Context context, final DICallBack<GoogleSignInAccount> callBack) {
        this.context = context;
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

    public void startBackup(Context activity, ArrayList<DIFile> files, DICallBack<DIFile> backupCallback) {
        DriveIt.backupCallback = backupCallback;
        if (activity == null)
            return;
        if (EasyPermissions.hasPermissions(activity, permissions)) {
            initiateBackup(activity, files);
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

    private void initiateBackup(Context activity, ArrayList<DIFile> files) {
        Intent intent = new Intent(activity, DIBackupService.class);
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<DIFile>>() {
        }.getType();
        String data = gson.toJson(files, type);
        String[] fileData = new String[files.size()];
        for (int i = 0; i < files.size(); i++) {
            fileData[i] = files.get(i).getFile().getAbsolutePath();
        }

        intent.putExtra(DIConstants.DATA, data);
        intent.putExtra(DIConstants.DATA_FILES, fileData);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.startForegroundService(intent);
        } else {
            activity.startService(intent);
        }
//        DIBackupService.getInstance().startBackup(activity, files, listener);
    }

    public void startRestore(Context activity, DICallBack<DIFile> restoreCallback) {
        DriveIt.restoreCallback = restoreCallback;
        if (activity == null)
            return;
        if (EasyPermissions.hasPermissions(activity, permissions)) {
            initiateRestore(activity);
        } else {
            Log.e(TAG, "Process Stopped !! Storage permissions error.");
        }
    }


    private void initiateRestore(Context activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.startForegroundService(new Intent(activity, DIRestoreService.class));
        } else {
            activity.startService(new Intent(activity, DIRestoreService.class));
        }
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

    public void getBackupSize(Context context, final DICallBack<DIBackupDetails> diCallBack) {
        DIBackupDetailsRepository.getINSTANCE().getBackupDetails(context, new DICallBack<DIBackupDetails>() {
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

    public void deleteBackup(Context activity, boolean showNotification, DICallBack<DIFile> diFileDICallBack) {
        if (context == null)
            return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.startService(new Intent(activity, DIDeleteBackupService.class));
        } else {
            activity.startService(new Intent(activity, DIDeleteBackupService.class));
        }
        DIDeleteBackupService.getInstance().showNotification(showNotification);
        DIDeleteBackupService.getInstance().deleteAll(activity, diFileDICallBack);

    }

    public void downloadOne(final String query, final DICallBack<DIFile> callBack) {
        DIFileLister.list(new DICallBack<ArrayList<DIFile>>() {
            @Override
            public void success(ArrayList<DIFile> files) {
                found = null;
                for (DIFile diFile : files) {
                    if (diFile.getName().toLowerCase().contains(query.toLowerCase())) {
                        found = diFile;
                        break;
                    }
                }
                if (found == null) {
                    callBack.failure("File not found");
                    return;
                }
                DIFileDownloader.downloadFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/.driveIt/", found, new DICallBack<File>() {
                    @Override
                    public void success(File file) {
                        found.setFile(file);
                        callBack.success(found);
                    }

                    @Override
                    public void failure(String error) {
                        callBack.failure("" + error);
                    }
                });


            }

            @Override
            public void failure(String error) {
                callBack.failure("" + error);
            }
        });
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
            source.close();
            destination.close();
        } catch (Exception e) {

        }
    }

    static void backupFileComplete(DIFile diFile) {
        if (backupCallback != null)
            backupCallback.success(diFile);
    }

    static void backupFileFailed(String error) {
        if (backupCallback != null)
            backupCallback.failure(error);

    }

    static void restoreFileComplete(DIFile diFile) {
        if (restoreCallback != null)
            restoreCallback.success(diFile);
    }

    static void restoreFileFailed(String error) {
        if (restoreCallback != null)
            restoreCallback.failure(error);

    }


    static void autoBackupComplete(DIBackupDetails backupDetails) {
        if (autoBackupCallback != null)
            autoBackupCallback.success(backupDetails);
    }

    static void autoBackupFailed(DIBackupDetails backupDetails) {
        if (autoBackupCallback != null)
            autoBackupCallback.failure(backupDetails.getError());
    }
}
