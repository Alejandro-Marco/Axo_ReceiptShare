package com.axolotl.receiptmanager.activity

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import com.axolotl.receiptmanager.R
import com.axolotl.receiptmanager.utility.launchActivity
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.axolotl.receiptmanager.model.ReceiptData
import com.axolotl.receiptmanager.utility.UPLOAD_ACTIVITY
import com.axolotl.receiptmanager.utility.showToast
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_upload.*
import java.util.*
import kotlin.collections.ArrayList

class UploadActivity : AppCompatActivity() {
    // Firebase
    private val firestoreDB = Firebase.firestore
    private val firebaseStorage = FirebaseStorage.getInstance()
    private val firebaseStorageImage = firebaseStorage.getReference("Receipts")

    private var accountImageBitmap: Bitmap? = null
    private var accountImageURI: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        btnUpload.setOnClickListener {
            var upload = true
            if (etAmount.text.isNullOrBlank()) {
                etAmount.error = "Required"
                upload = false
            }
            if (etType.text.isNullOrBlank()) {
                etType.error = "Required"
                upload = false
            }
            if (accountImageBitmap == null) {
                showToast("No Receipt Image", 1000)
                upload = false
            }
            val type = ArrayList(
                etType.text
                    .toString()
                    .lowercase(Locale.getDefault())
                    .replace(" ", "")
                    .split(',')
            )
            if (upload) {
                val amount = etAmount.text.toString().toDouble()
                val date = "${datePicker.year}/${datePicker.month + 1}/${datePicker.dayOfMonth}"
                uploadReceiptData(
                    type,
                    amount,
                    date
                )
            }
        }

        ivReceipt.setOnClickListener {
            loadReceiptImage()
        }
    }

    override fun onBackPressed() {
//        super.onBackPressed()
        launchActivity<FrontActivity> {

        }
    }

    private fun uploadReceiptData(
        type: ArrayList<String>,
        amount: Double,
        date: String
    ) {
        val uid = firestoreDB.collection("Receipts").document().id
        val receiptData = ReceiptData(type, amount, date, uid)
        Log.d(UPLOAD_ACTIVITY, "Uploading Receipt Data")
        showToast("Uploading Receipt", 1000)
        firestoreDB.collection("Receipts")
            .document(uid)
            .set(receiptData)
            .addOnSuccessListener {
                Log.d(UPLOAD_ACTIVITY, "Receipt Data uploaded")
                val fileRef = firebaseStorageImage.child("$uid.jpg")
                val uploadTask = fileRef.putFile(accountImageURI!!)
                Log.d(UPLOAD_ACTIVITY, "Uploading Receipt Image")
                val urlTask = uploadTask.continueWithTask { task ->
                    if (task.isSuccessful) {
                        fileRef.downloadUrl
                    } else {
                        task.exception?.let {
                            throw it
                        }
                    }
                }
                urlTask.addOnSuccessListener {
                    accountImageBitmap = null
                    accountImageURI = null
                    etAmount.text?.clear()
                    etType.text?.clear()
                    ivReceipt.setImageResource(R.drawable.invoice)
                    ImageViewCompat.setImageTintList(
                        ivReceipt,
                        ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
                    )
                    showToast("Receipt Uploaded Thank you", 1500)
                }
                urlTask.addOnFailureListener {
                    showToast("Failed Uploading", 1000)
                }
            }
            .addOnFailureListener {
                Log.d(UPLOAD_ACTIVITY, "Receipt Data failed uploading")
            }
    }

    private fun loadReceiptImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            ImageViewCompat.setImageTintList(ivReceipt, null)
            accountImageURI = data.data!!
            accountImageBitmap = MediaStore.Images.Media.getBitmap(contentResolver, accountImageURI)
            ivReceipt.setImageBitmap(accountImageBitmap)
        }
    }
}