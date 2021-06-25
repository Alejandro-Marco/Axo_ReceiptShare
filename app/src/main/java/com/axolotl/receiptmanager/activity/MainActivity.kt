package com.axolotl.receiptmanager.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.axolotl.receiptmanager.R
import com.axolotl.receiptmanager.utility.launchActivity
import android.util.Log

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onBackPressed() {
//        super.onBackPressed()
        launchActivity<FrontActivity> {

        }
    }
}