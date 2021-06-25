package com.axolotl.receiptmanager.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.axolotl.receiptmanager.R
import com.axolotl.receiptmanager.utility.launchActivity
import android.util.Log
import com.axolotl.receiptmanager.model.ReceiptData
import com.axolotl.receiptmanager.utility.UPLOAD_ACTIVITY
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_upload.*

class UploadActivity : AppCompatActivity() {
    private val firestoreDB = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        btnUpload.setOnClickListener {
            val type = etType.text.toString()
            val amount = etAmount.text.toString().toDouble()
            val day = datePicker.dayOfMonth
            val month = datePicker.month
            val year = datePicker.year
            val date = "$year/$month/$day"
            uploadReceiptData(
                type,
                amount,
                date
            )
        }
    }

    override fun onBackPressed() {
//        super.onBackPressed()
        launchActivity<FrontActivity> {

        }
    }

    private fun uploadReceiptData(
        type: String,
        amount: Double,
        date: String
    ) {
        val uid = firestoreDB.collection("Receipts").document().id
        val receiptData = ReceiptData(type, amount, date, uid)
        Log.d(UPLOAD_ACTIVITY, "Uploading Receipt Data")
        firestoreDB.collection("Receipts")
            .document(uid)
            .set(receiptData)
            .addOnSuccessListener {
                Log.d(UPLOAD_ACTIVITY, "Receipt Data uploaded")
            }
            .addOnSuccessListener {
                Log.d(UPLOAD_ACTIVITY, "Receipt Data failed uploading")
            }
    }
}