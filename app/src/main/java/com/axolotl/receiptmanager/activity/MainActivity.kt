package com.axolotl.receiptmanager.activity

import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View.*
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.axolotl.receiptmanager.R
import com.axolotl.receiptmanager.adapter.ReceiptAdapter
import com.axolotl.receiptmanager.model.ReceiptData
import com.axolotl.receiptmanager.utility.MAIN_ACTIVITY
import com.axolotl.receiptmanager.utility.launchActivity
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_upload.view.*
import kotlinx.android.synthetic.main.dialog_receipt.view.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(), ReceiptAdapter.ClickReceipt {
    // Firebase
    private val firestoreDB = Firebase.firestore
    private val firebaseStorage = FirebaseStorage.getInstance()
    private val firebaseStorageImage = firebaseStorage.getReference("Receipts")
    private var firestoreListener: ListenerRegistration? = null

    // Receipt Recycler View
    private lateinit var receiptList: ArrayList<ReceiptData>
    private lateinit var receiptAdapter: ReceiptAdapter

    private var receiptImageBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        receiptList = arrayListOf()
        receiptAdapter = ReceiptAdapter(
            this,
            receiptList,
            this
        )
        rvReceiptList.layoutManager = LinearLayoutManager(this)
        rvReceiptList.adapter = receiptAdapter

        btnSearch.setOnClickListener {
            val type = if (etType.text.toString().isEmpty()) arrayListOf<String>("any") else ArrayList(
                etType.text
                    .toString()
                    .lowercase(Locale.getDefault())
                    .replace(" ", "")
                    .split(',')
            )
            val minAmount = if (etAmount.text.toString().isEmpty()) 0.00 else etAmount.text.toString().toDouble()
            val minDate = "${datePicker.year}/${datePicker.month + 1}/${datePicker.dayOfMonth}"
            readFirestore(type, minAmount, minDate)

            if (etType.text.toString().isEmpty())
                etType.setText("any")
            if (etAmount.text.toString().isEmpty())
                etAmount.setText("0.00")
        }
    }

    private fun readFirestore(type: ArrayList<String>, minAmount: Double, minDate: String) {
        try {
            firestoreListener!!.remove()
            firestoreListener = null
            Log.d(MAIN_ACTIVITY, "Listener Removed")
        } catch (e: Exception) {
            Log.d(MAIN_ACTIVITY, "No Listener found")
        }
        firestoreDB.collection("Receipts")
            .get()
            .addOnSuccessListener { QuerySnapshots ->
                Log.d(MAIN_ACTIVITY, "Firestore read successful")
                receiptList.clear()
                for (QuerySnapshot in QuerySnapshots) {
                    val receiptData = QuerySnapshot.toObject(ReceiptData::class.java)
                    Log.d(MAIN_ACTIVITY, receiptData.uid)
                    Log.d(MAIN_ACTIVITY, receiptData.type.joinToString(separator = ","))
                    Log.d(MAIN_ACTIVITY, receiptData.amount.toString())
                    Log.d(MAIN_ACTIVITY, receiptData.date)
                    if (receiptData.amount >= minAmount
                        && isInArray(receiptData.type, type)
                        && isMinimumDate(
                            receiptData.date,
                            minDate
                        )
                    )
                        receiptList.add(receiptData)
                }
                receiptAdapter.notifyDataSetChanged()
                if (firestoreListener == null) {
                    addFirestoreListener(type, minAmount, minDate)
                }
            }
            .addOnFailureListener {
                Log.d(MAIN_ACTIVITY, "Error Reading Firestore")
            }
    }

    private fun addFirestoreListener(type: ArrayList<String>, minAmount: Double, minDate: String) {
        val query = firestoreDB.collection("Receipts")
        firestoreListener = query.addSnapshotListener { QuerySnapshots, e ->
            receiptList.clear()
            Log.d(MAIN_ACTIVITY, "CHANGES")
            if (QuerySnapshots != null)
                for (QuerySnapshot in QuerySnapshots) {
                    val receiptData = QuerySnapshot.toObject(ReceiptData::class.java)
                    Log.d(MAIN_ACTIVITY, receiptData.uid)
                    Log.d(MAIN_ACTIVITY, receiptData.type.joinToString(separator = ","))
                    Log.d(MAIN_ACTIVITY, receiptData.amount.toString())
                    Log.d(MAIN_ACTIVITY, receiptData.date)
                    if (receiptData.amount >= minAmount
                        && isInArray(receiptData.type, type)
                        && isMinimumDate(
                            receiptData.date,
                            minDate
                        )
                    )
                        receiptList.add(receiptData)
                }
            receiptAdapter.notifyDataSetChanged()
        }
    }

    /***
     * FUN FUN FUN
     * NEW TASK - Create function that outputs true if sub array exist in an array
     * 27 sec
     * Passed all test
     */
    private fun isSubArray(mainArray: ArrayList<String>, subArray: ArrayList<String>): Boolean {
        for (item in subArray) {
            if (!mainArray.contains(item))
                return false
        }
        return true
    }

    /***
     * FUN FUN FUN
     * NEW TASK - Create function that outputs true if sub array exist in an array
     * 12 sec
     * Passed all test
     */
    private fun isInArray(mainArray: ArrayList<String>, subArray: ArrayList<String>): Boolean {
        for (item in subArray) {
            if (mainArray.contains(item))
                return true
        }
        return false
    }

    /***
     * NEW TASK - Create function that check if date is less than minimum
     * 33 sec
     * Passed all test
     */
    private fun isMinimumDate(receiptDate: String, minDate: String): Boolean {
        val rdDate = receiptDate.split('/')
        val mDate = minDate.split('/')
        for (index in 0..2)
            if (rdDate[index].toDouble() < mDate[index].toDouble())
                return false
        return true
    }

    private fun saveMediaToStorage(bitmap: Bitmap, uid: String) {
        val filename = "${System.currentTimeMillis()}_$uid.jpg"
        var fos: OutputStream? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            this.contentResolver?.also { resolver ->
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                val imageUri: Uri? =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
            }
        } else {
            val imagesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
        }
        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }
    }

    override fun onBackPressed() {
//        super.onBackPressed()
        launchActivity<FrontActivity> {

        }
    }

    override fun onClickReceipt(uid: String) {
        Log.d(MAIN_ACTIVITY, "Create Prompt $uid")
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_receipt, null)
        val ivReceipt = dialogLayout.findViewById<ImageView>(R.id.promptImage)
        val layoutLoading = dialogLayout.findViewById<RelativeLayout>(R.id.layoutLoading)
        builder.setTitle(uid)
        builder.setView(dialogLayout)
        builder.setPositiveButton("Get") { _, _ ->
            firestoreDB.collection("Receipts")
                .document(uid)
                .delete()
                .addOnSuccessListener {
                    Log.d(MAIN_ACTIVITY, "(BUILDER) Get Receipt")
                    val imageRef = firebaseStorageImage.child("${uid}.jpg")
                    imageRef.delete().addOnSuccessListener {
                        Log.d(MAIN_ACTIVITY, "Receipt Deleted")
                        saveMediaToStorage(receiptImageBitmap!!, uid)
                        // Download receipt to phone
                    }.addOnFailureListener {
                        Log.d(MAIN_ACTIVITY, "There was an error")
                    }
                }
                .addOnFailureListener {

                }
        }
        builder.setNegativeButton("Cancel") { _, _ ->
            Log.d(MAIN_ACTIVITY, "(BUILDER) Cancel Get Receipt")
        }
        firebaseStorageImage.child("${uid}.jpg").getBytes(Long.MAX_VALUE)
            .addOnSuccessListener { imageData ->
                Log.d(MAIN_ACTIVITY, "Receipt Loaded")
                receiptImageBitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                layoutLoading.visibility = GONE
                ivReceipt.visibility = VISIBLE
                ivReceipt.setImageBitmap(receiptImageBitmap)
            }
            .addOnFailureListener {
                Log.d(MAIN_ACTIVITY, "There was an error")
            }
        builder.show()
    }
}