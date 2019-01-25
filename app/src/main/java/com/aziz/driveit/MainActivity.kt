package com.aziz.driveit

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import com.aziz.drive_it.DriveUtils.DICallBack
import com.aziz.drive_it.DriveUtils.model.DIFile
import com.aziz.drive_it.DriveUtils.DriveIt
import com.aziz.drive_it.DriveUtils.model.Frequency
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
        auto_backup.setOnClickListener {
            val fileList = ArrayList<File>()
            val uploads = File(Environment.getExternalStorageDirectory().absolutePath + "/Upload")
            Log.d("MAIN", "FILE exists " + uploads.exists())
            val paths = arrayOfNulls<String>(uploads!!.list().size)
            for (i in 0 until uploads.listFiles().size) {
                paths[i] = uploads.listFiles()[i].absolutePath
            }
            DriveIt.getInstance().setAutoBackup(Frequency.TEST, paths, object : DICallBack<DIFile> {
                override fun success(file: DIFile?) {
                    Log.d("MAIN", "AUTO-Backup ${file!!.name}")
                }

                override fun failure(error: String?) {
                    Log.d("MAIN", "$error")
                }
            })
        }
        backup.setOnClickListener {
            val fileList = ArrayList<File>()
            val uploads = File(Environment.getExternalStorageDirectory().absolutePath + "/Upload")
            Log.d("MAIN", "FILE exists " + uploads.exists())
            for (file: File in uploads.listFiles()) {
                fileList.add(file)
            }
            DriveIt.getInstance().startBackup(this@MainActivity, fileList, object : DICallBack<DIFile> {
                override fun success(file: DIFile?) {
                    Log.d("MAIN", "Backup ${file!!.name}")
                }

                override fun failure(error: String?) {
                    Log.d("MAIN", "$error")
                }
            })
        }
        restore.setOnClickListener {
            DriveIt.getInstance().startRestore(this@MainActivity, object : DICallBack<File> {
                override fun success(file: File?) {
                    Log.d("MAIN", "DONE ${file!!.name}")
                    DriveIt.getInstance()
                        .writeFile(file, Environment.getExternalStorageDirectory().absolutePath + "/demo/" + file.name)
                }

                override fun failure(error: String?) {
                    Log.d("MAIN", "$error")
                }
            })
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        DriveIt.getInstance().onActivityResult(this, requestCode, resultCode, data)
    }
}
