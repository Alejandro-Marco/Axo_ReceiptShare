package com.axolotl.receiptmanager.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.axolotl.receiptmanager.R
import com.axolotl.receiptmanager.utility.launchActivity
import kotlinx.android.synthetic.main.activity_front.*
import android.util.Log

class FrontActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_front)

        btnUpload.setOnClickListener {
            launchActivity<UploadActivity> {

            }
        }

        btnDownload.setOnClickListener {
            launchActivity<MainActivity> {

            }
        }
    }
}