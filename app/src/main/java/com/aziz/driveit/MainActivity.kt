package com.aziz.driveit

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import com.aziz.drive_it.DriveUtils.DICallBack
import com.aziz.drive_it.DriveUtils.model.DIFile
import com.aziz.drive_it.DriveUtils.DriveIt
import com.aziz.drive_it.DriveUtils.model.DIBackupDetails
import com.aziz.drive_it.DriveUtils.model.Frequency
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        signIn.setOnClickListener {
            //            DriveIt.getInstance().setIcon(this@MainActivity,R.drawable.speed)
            DriveIt.getInstance().signIn(this@MainActivity, object : DICallBack<String> {
                override fun success(DIObject: String?) {

                }

                override fun failure(error: String?) {

                }
            })
        }
        signOut.setOnClickListener {

            DriveIt.getInstance().signOut()
        }
        delete.setOnClickListener {
            DriveIt.getInstance().deleteBackup(this@MainActivity, true, object : DICallBack<DIFile> {
                override fun success(file: DIFile?) {
                    Log.d("MAIN", "DELETED file " + file)

                }

                override fun failure(error: String?) {
                    Log.d("MAIN", "Failed DELETE " + error)
                }
            })
        }
        updateOne.setOnClickListener {
            val file = File(Environment.getExternalStorageDirectory().absolutePath + "/Upload", "DATA2.txt")
            val diFile = DIFile()
            diFile.file = file
            DriveIt.getInstance().createOrUpdateOne(diFile, object : DICallBack<DIFile> {
                override fun success(DIObject: DIFile?) {
                    Log.d("MAIN", "Updated " + DIObject!!.name)
                }

                override fun failure(error: String?) {
                    Log.d("MAIN", "Update failed " + error)
                }
            })
        }
        auto_backup.setOnClickListener {

            val fileList = ArrayList<File>()
            val uploads = File(Environment.getExternalStorageDirectory().absolutePath + "/Upload")
            Log.d("MAIN", "FILE exists " + uploads.exists())
            val paths = arrayOfNulls<String>(uploads!!.list().size)
            for (i in 0 until uploads.listFiles().size) {
                paths[i] = uploads.listFiles()[i].absolutePath
            }
            DriveIt.getInstance().setAutoBackup(Frequency.TEST, paths, object : DICallBack<DIBackupDetails> {
                override fun success(DIObject: DIBackupDetails?) {

                }

                override fun failure(error: String?) {

                }
            })
        }

        details.setOnClickListener {
            backup_size.setText("updating")
            DriveIt.getInstance().getBackupSize(applicationContext, object : DICallBack<DIBackupDetails> {
                override fun success(details: DIBackupDetails?) {
                    backup_size.setText("size " + details!!.backupSize + " time " + format.format(Date(details!!.lastBackup)))
                }

                override fun failure(error: String?) {
                    backup_size.setText("Error " + error)
                }
            })

        }
        backup.setOnClickListener {
            val fileList = ArrayList<DIFile>()
            val uploads = File(Environment.getExternalStorageDirectory().absolutePath + "/Upload")
            Log.d("MAIN", "FILE exists " + uploads.exists())
            for (file: File in uploads.listFiles()) {
                val diFile = DIFile()
                diFile.setFile(file, "desc-${file.name}")
                fileList.add(diFile)
            }
            DriveIt.getInstance().setIcon(R.drawable.ic_gdrive)
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
            DriveIt.getInstance().setIcon(R.drawable.ic_gdrive)

            DriveIt.getInstance().startRestore(this@MainActivity, object : DICallBack<DIFile> {
                override fun success(file: DIFile?) {
                    Log.d("MAIN", "DONE ${file!!.name}")
                    DriveIt.getInstance()
                        .writeFile(
                            file.file,
                            Environment.getExternalStorageDirectory().absolutePath + "/demo/" + file.name
                        )
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
