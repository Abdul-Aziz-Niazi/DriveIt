package com.aziz.drive_it.DriveUtils;

import android.accounts.Account;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;

import java.io.IOException;

public class AccountTask extends AsyncTask<String, Void, Void> {
    private static final String TAG = AccountTask.class.getSimpleName();
    private Context context;

    public AccountTask(Context context) {
        this.context = context;
    }

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
//            ((Activity) context).startActivityForResult(((UserRecoverableAuthException) e).getIntent(), 10);
        }
        return null;
    }
}