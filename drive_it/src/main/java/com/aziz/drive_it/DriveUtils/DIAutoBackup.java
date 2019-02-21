package com.aziz.drive_it.DriveUtils;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.aziz.drive_it.DriveUtils.model.DIFile;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.tasks.Task;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class DIAutoBackup extends Worker {

    private static final String TAG = DIAutoBackup.class.getSimpleName();
    @NonNull
    private final Context context;
    private GoogleSignInClient client;
    private GoogleSignInAccount result;
    private Result workResult;
    private ArrayList<DIFile> fileArrayList = new ArrayList<>();

    public DIAutoBackup(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        String[] array = getInputData().getStringArray(DIConstants.DATA);
        String[] desc = getInputData().getStringArray(DIConstants.DATA_DESC);
        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                DIFile file = new DIFile();
                file.setFile(new File(array[i]));
                if (desc != null) {
                    file.setDescription(desc[i]);
                    Log.d(TAG, "doWork: " + desc[i]);
                }
                fileArrayList.add(file);
            }
        }
        Log.d(TAG, "DIAutoBackup: " + fileArrayList);
        silentSignIn();
        return workResult;
    }

    private void silentSignIn() {
        client = buildSignInClient();
        GoogleSignInAccount signInAccount = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
        if (signInAccount != null) {
            result = signInAccount;
            Log.w(TAG, "Last Account: " + signInAccount.getEmail() + " Sign-in success, starting service");
            new AccountTask().execute(result.getEmail());
            return;
        }

        Task<GoogleSignInAccount> googleSignInAccountTask = client.silentSignIn();
        if (googleSignInAccountTask.isSuccessful()) {
            workResult = Result.success();
            result = googleSignInAccountTask.getResult();
            Log.w(TAG, "Silent Sign-in success, starting fetching token " + result.getEmail());
            new AccountTask().execute(result.getEmail());
        } else {
            workResult = Result.failure();
            Log.e(TAG, "Silent Sign-in Failed in Backup Job Service");
        }
    }

    private GoogleSignInClient buildSignInClient() {
        Scope SCOPE_DRIVE = new Scope("https://www.googleapis.com/auth/drive");
        Scope SCOPE_METADATA = new Scope("https://www.googleapis.com/auth/drive.metadata");

        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(SCOPE_DRIVE, SCOPE_METADATA, Drive.SCOPE_APPFOLDER, Drive.SCOPE_FILE)
                .build();
        return GoogleSignIn.getClient(context, options);
    }


    public class AccountTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... account) {
            Log.d(TAG, "doInBackground: Fetching Token");
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
                workResult = Worker.Result.failure();
                e.printStackTrace();
            } catch (GoogleAuthException e) {
                workResult = Worker.Result.failure();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.d(TAG, "onPostExecute: STARTING SERVICE");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(new Intent(context, DIBackupService.class));
            } else {
                context.startService(new Intent(context, DIBackupService.class));
            }

            DIBackupService.getInstance().startBackup(context, fileArrayList, new DICallBack<DIFile>() {
                @Override
                public void success(DIFile file) {
                    Log.d(TAG, "success: auto-backup");
                }

                @Override
                public void failure(String error) {
                    Log.d(TAG, "failure: auto-backup");
                }
            });
        }
    }
}
