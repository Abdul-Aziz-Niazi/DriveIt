package com.aziz.driveit

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.aziz.driveit.DriveUtils.DICallBack
import com.aziz.driveit.DriveUtils.DriveIt
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.drive.Drive
import com.google.android.gms.drive.DriveClient
import com.google.android.gms.drive.DriveResourceClient
import com.google.android.gms.drive.DriveStatusCodes
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {
    private val SIGN_IN_REQUEST: Int = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        signIn.setOnClickListener {
            DriveIt.getInstance().signIn(this@MainActivity)
        }
        signOut.setOnClickListener {
            DriveIt.getInstance().signOut()
        }
        backup.setOnClickListener {

            DriveIt.getInstance().startBackup(this@MainActivity, object : DICallBack<File> {
                override fun success(file: File?) {
                    Log.d("MAIN","DONE ${file!!.name}")
                }

                override fun failure(error: String?) {
                    Log.d("MAIN","$error")
                }
            })
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        DriveIt.getInstance().onActivityResult(this, requestCode, resultCode, data)
    }
}
