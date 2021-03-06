package com.axolotl.receiptshare.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.axolotl.receiptshare.R
import com.axolotl.receiptshare.utility.launchActivity
import kotlinx.android.synthetic.main.activity_front.*
import android.util.Log
import com.axolotl.receiptshare.utility.FRONT_ACTIVITY

class FrontActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_front)

        Log.d(FRONT_ACTIVITY, "onCreate")

        btnUpload.setOnClickListener {
            Log.d(FRONT_ACTIVITY, "Launch Upload")
            launchActivity<UploadActivity> {

            }
        }

        btnDownload.setOnClickListener {
            Log.d(FRONT_ACTIVITY, "Launch Download")
            launchActivity<MainActivity> {

            }
        }
    }

    override fun onBackPressed() {
//        super.onBackPressed()
    }
}