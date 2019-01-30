package com.aziz.drive_it.DriveUtils;

import android.accounts.Account;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;

import java.io.IOException;

public class AccountTask extends AsyncTask<String, String, String> {
    private static final String TAG = AccountTask.class.getSimpleName();
    private Context context;
    private DICallBack<String> callBack;

    public AccountTask(Context context, DICallBack<String> callBack) {
        this.context = context;
        this.callBack = callBack;
    }

    @Override
    protected String doInBackground(String... account) {
        try {
            String token = GoogleAuthUtil.getToken(context, new Account(account[0], GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE),
                    "oauth2:profile email https://www.googleapis.com/auth/drive.appdata "
                            + "https://www.googleapis.com/auth/drive.file "
                            + "https://www.googleapis.com/auth/drive.metadata "
                            + "https://www.googleapis.com/auth/drive"
            );

            Log.d(TAG, "onActivityResult: TOKEN " + token);
            DINetworkHandler.getInstance().setAuthToken("Bearer " + token);
            return token;
        } catch (IOException e) {
            if (callBack != null)
                callBack.failure(e.getMessage());
            e.printStackTrace();
        } catch (GoogleAuthException e) {
            if (callBack != null)
                callBack.failure(e.getMessage());
//            ((Activity) context).startActivityForResult(((UserRecoverableAuthException) e).getIntent(), 10);
        }
        return null;
    }

    @Override
    protected void onPostExecute(String token) {
        super.onPostExecute(token);

        if (callBack != null)
            callBack.success(token);

    }
}