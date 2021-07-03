package com.axolotl.receiptshare.activity

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import com.axolotl.receiptshare.R
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.axolotl.receiptshare.model.ReceiptData
import com.axolotl.receiptshare.utility.*
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_upload.*
import java.util.*
import kotlin.collections.ArrayList

class UploadActivity : AppCompatActivity() {
    // Firebase
    private val firestoreDB = Firebase.firestore
    private val firebaseStorage = FirebaseStorage.getInstance()
    private val firebaseStorageImage = firebaseStorage.getReference(PATH_RECEIPT)
    private val firebaseAnalytics = Firebase.analytics

    private var accountImageBitmap: Bitmap? = null
    private var accountImageURI: Uri? = null

    /**
     * Performance monitoring does not work as intended when using listeners
     * using pseudoTrace instead
     */
    private var uploadStartTime: Long? = null

    private var imageUploading = false

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
            } else {
                if (accountImageBitmap != null)
                    showToast("Missing Inputs")
            }
        }

        ivReceipt.setOnClickListener {
            Log.d(UPLOAD_ACTIVITY, "Click Image")
            loadReceiptImage()
        }
    }

    override fun onBackPressed() {
//        super.onBackPressed()
        if (!imageUploading) {
            launchActivity<FrontActivity> {

            }
        } else {
            showToast("Please wait")
        }
    }

    private fun uploadReceiptData(
        type: ArrayList<String>,
        amount: Double,
        date: String
    ) {
        uploadStartTime = System.currentTimeMillis()
        imageUploading = true
        val uid = firestoreDB.collection(PATH_RECEIPT).document().id
        val receiptData = ReceiptData(type, amount, date, uid)
        Log.d(UPLOAD_ACTIVITY, "Uploading Receipt Data")
        layoutLoading.visibility = VISIBLE
        showToast("Uploading Receipt", 1000)
        // Update the user after n seconds to say that image is still being uploaded
        val handler = Handler()
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (imageUploading) {
                    Log.d(MAIN_ACTIVITY, "Prompt that image is still uploading")
                    showToast(UPLOAD_PROMPTS.random(), 1500)
                    handler.postDelayed(this, UPLOAD_PROMPT_WAIT_TIME)
                }
            }
        }, UPLOAD_PROMPT_WAIT_TIME)

        firestoreDB.collection(PATH_RECEIPT)
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
                    ivReceipt.setImageResource(R.drawable.img_invoice)
                    ImageViewCompat.setImageTintList(
                        ivReceipt,
                        ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
                    )
                    showToast("Receipt Uploaded Thank you", 1500)
                    layoutLoading.visibility = GONE
                    imageUploading = false
                    firebaseAnalytics.logEvent("uploaded_receipt", Bundle().apply {
                        this.putString("uid", uid)
                        this.putString("amount", amount.toString())
                        this.putString("date", date)
                    })
                    pseudoTrace("upload_duration", uploadStartTime!!)
                }
                urlTask.addOnFailureListener {
                    showToast("Failed Uploading", 1000)
                    layoutLoading.visibility = GONE
                    imageUploading = false
                    pseudoTrace("failed_upload_duration", uploadStartTime!!)
                }
            }
            .addOnFailureListener {
                Log.d(UPLOAD_ACTIVITY, "Receipt Data failed uploading")
                layoutLoading.visibility = GONE
                imageUploading = false
                pseudoTrace("failed_upload_duration", uploadStartTime!!)
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
//            ivReceipt.setImageBitmap(accountImageBitmap)
            Picasso.with(this).load(accountImageURI).into(ivReceipt)
        }
    }
}