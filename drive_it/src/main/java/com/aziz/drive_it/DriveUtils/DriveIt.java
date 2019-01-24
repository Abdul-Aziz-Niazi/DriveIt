package com.aziz.drive_it.DriveUtils;

import android.Manifest;
import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresPermission;
import android.util.Log;
import com.aziz.drive_it.DriveUtils.model.DIFile;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.tasks.Task;
import pub.devrel.easypermissions.EasyPermissions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class DriveIt {
    private static final String TAG = DriveIt.class.getSimpleName();
    private static DriveIt INSTANCE;
    private GoogleSignInClient signInClient;
    private ArrayList<File> fileArrayList = new ArrayList<>();
    private Context context;
    private String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private DriveIt() {
    }

    synchronized public static DriveIt getInstance() {
        if (INSTANCE == null)
            INSTANCE = new DriveIt();
        return INSTANCE;
    }

    public void signIn(Context context) {
        signInClient = buildSignInClient(context);
        if (context instanceof Activity) {
            ((Activity) context).startActivityForResult(signInClient.getSignInIntent(), DIConstants.REQUEST_BACKUP);
        }
    }

    public void signOut() {
        if (signInClient != null)
            signInClient.signOut();
    }

    private GoogleSignInClient buildSignInClient(Context context) {
        Scope SCOPE_DRIVE = new Scope("https://www.googleapis.com/auth/drive");
        Scope SCOPE_METADATA = new Scope("https://www.googleapis.com/auth/drive.metadata");

        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(SCOPE_DRIVE, SCOPE_METADATA, Drive.SCOPE_APPFOLDER, Drive.SCOPE_FILE)
                .requestIdToken(DIConstants.ClIENT_ID)
                .build();
        return GoogleSignIn.getClient(context, options);
    }

    public void onActivityResult(Context context, int requestCode, int resultCode, Intent data) throws IOException, GoogleAuthException {
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
        new AccountTask().execute(getAccountTask.getResult().getEmail());
    }

    @RequiresPermission(allOf = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE})
    public void startBackup(Activity activity, ArrayList<File> files, DICallBack<DIFile> listener) {
        if (EasyPermissions.hasPermissions(activity, permissions)) {
            initiateBackup(activity, files, listener);
        } else {
            Log.e(TAG, "Process Stopped !! Storage permissions error.");
        }
    }

    private void initiateBackup(Activity activity, ArrayList<File> files, DICallBack<DIFile> listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.startForegroundService(new Intent(activity, DIRestoreService.class));
        } else {
            activity.startService(new Intent(activity, DIRestoreService.class));
        }
        DIBackupService.getInstance().startBackup(activity, files, listener);
    }

    @RequiresPermission(allOf = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE})
    public void startRestore(Activity activity, DICallBack<File> listener) {
        if (EasyPermissions.hasPermissions(activity, permissions)) {
            initiateRestore(activity, listener);
        } else {
            Log.e(TAG, "Process Stopped !! Storage permissions error.");
        }
    }

    private void initiateRestore(Activity activity, DICallBack<File> listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.startForegroundService(new Intent(activity, DIRestoreService.class));
        } else {
            activity.startService(new Intent(activity, DIRestoreService.class));
        }
        DIRestoreService.getInstance().startRestore(activity, listener);
    }

    public class AccountTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... account) {
            try {
                String token = GoogleAuthUtil.getToken(context, new Account(account[0], GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE),
                        "oauth2:profile email https://www.googleapis.com/auth/drive.appdata "
                                + "https://www.googleapis.com/auth/drive.file "
                                + "https://www.googleapis.com/auth/drive.metadata "
                                + "https://www.googleapis.com/auth/drive"
                );
                Log.d(TAG, "onActivityResult: TOKEN " + token);
                DINetworkHandler.getInstance().setAuthToken("Bearer " + token);


            } catch (IOException e) {
                e.printStackTrace();
            } catch (GoogleAuthException e) {
                ((Activity) context).startActivityForResult(((UserRecoverableAuthException) e).getIntent(), 10);
            }
            return null;
        }
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

        try (FileChannel source = new FileInputStream(sourceFile).getChannel(); FileChannel destination = new FileOutputStream(destFile).getChannel()) {
            destination.transferFrom(source, 0, source.size());
            sourceFile.delete();
        }
    }

}
