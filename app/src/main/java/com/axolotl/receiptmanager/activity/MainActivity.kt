package com.axolotl.receiptmanager.activity

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.axolotl.receiptmanager.R
import com.axolotl.receiptmanager.adapter.ReceiptAdapter
import com.axolotl.receiptmanager.model.ReceiptData
import com.axolotl.receiptmanager.utility.*
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(), ReceiptAdapter.ClickReceipt {
    // Storage Permission
    private val STORAGE_PERMISSION_CODE = 1

    // Firebase
    private val firestoreDB = Firebase.firestore
    private val firebaseStorage = FirebaseStorage.getInstance()
    private val firebaseStorageImage = firebaseStorage.getReference(PATH_RECEIPT)
    private var firestoreListener: ListenerRegistration? = null
    private val firebaseAnalytics = Firebase.analytics

    // Receipt Recycler View
    private lateinit var receiptList: ArrayList<ReceiptData>
    private lateinit var receiptAdapter: ReceiptAdapter

    private var receiptImageBitmap: Bitmap? = null

    /**
     * Performance monitoring does not work as intended when using listeners
     * using pseudoTrace instead
     */
    private var searchStartTime: Long? = null
    private var loadImageStartTime: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermission()

        receiptList = arrayListOf()
        receiptAdapter = ReceiptAdapter(
            this,
            receiptList,
            this
        )
        rvReceiptList.layoutManager = LinearLayoutManager(this)
        rvReceiptList.adapter = receiptAdapter

        btnSearch.setOnClickListener {
            val type =
                if (etType.text.toString().isEmpty()) arrayListOf<String>("any") else ArrayList(
                    etType.text
                        .toString()
                        .lowercase(Locale.getDefault())
                        .replace(" ", "")
                        .split(',')
                )
            val minAmount =
                if (etAmount.text.toString().isEmpty()) 0.00 else etAmount.text.toString()
                    .toDouble()
            val minDate = "${datePicker.year}/${datePicker.month + 1}/${datePicker.dayOfMonth}"
            readFirestore(type, minAmount, minDate)

            if (etType.text.toString().isEmpty())
                etType.setText("any")
            if (etAmount.text.toString().isEmpty())
                etAmount.setText("0.00")
            showToast("Searching", 500)
        }
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(
                this, (
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            showToast("Storage Permission Granted", 1000)
        } else {
            requestStoragePermission()
        }
    }

    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            val permissionDialog = AlertDialog.Builder(this)
            permissionDialog.setTitle("Permission Needed")
            permissionDialog.setMessage("This app requires permission to use storage")
            permissionDialog.setPositiveButton("Allow") { _, _ ->
                showToast("Permission Granted", 1000)
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
            }
            permissionDialog.setNegativeButton("Deny") { _, _ ->
                showToast("Permission Denied", 1000)
                launchActivity<FrontActivity> {

                }
            }
            permissionDialog.setOnCancelListener {
                showToast("Permission Denied")
                launchActivity<FrontActivity> {

                }
            }.create().show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                showToast("Permission Granted", 1000)
            else {
                showToast("Permission not Granted", 1000)
                launchActivity<FrontActivity> {

                }
            }
        }
    }

    private fun readFirestore(type: ArrayList<String>, minAmount: Double, minDate: String) {
        searchStartTime = System.currentTimeMillis()
        try {
            firestoreListener!!.remove()
            firestoreListener = null
            Log.d(MAIN_ACTIVITY, "Listener Removed")
        } catch (e: Exception) {
            Log.d(MAIN_ACTIVITY, "No Listener found")
        }
        firestoreDB.collection(PATH_RECEIPT)
            .get()
            .addOnSuccessListener { QuerySnapshots ->
                Log.d(MAIN_ACTIVITY, "Firestore read successful")
                receiptList.clear()
                val tempReceiptList = ArrayList<ReceiptData>()
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
                        tempReceiptList.add(receiptData)
                }
                for (receipt in sortByDate(tempReceiptList)) {
                    receiptList.add(receipt)
                }
                receiptAdapter.notifyDataSetChanged()
                if (firestoreListener == null) {
                    addFirestoreListener(type, minAmount, minDate)
                }
                if (receiptList.size == 0)
                    showToast("No Receipts found", 1000)
                pseudoTrace("search_duration", searchStartTime!!)
            }
            .addOnFailureListener {
                Log.d(MAIN_ACTIVITY, "Error Reading Firestore")
            }
    }

    private fun addFirestoreListener(type: ArrayList<String>, minAmount: Double, minDate: String) {
        val query = firestoreDB.collection(PATH_RECEIPT)
        firestoreListener = query.addSnapshotListener { QuerySnapshots, e ->
            receiptList.clear()
            Log.d(MAIN_ACTIVITY, "CHANGES")
            val tempReceiptList = ArrayList<ReceiptData>()
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
                        tempReceiptList.add(receiptData)
                }
            for (receipt in sortByDate(tempReceiptList)) {
                receiptList.add(receipt)
            }
            receiptAdapter.notifyDataSetChanged()
        }
    }

    /***
     * NEW TASK - Create function that outputs true if sub array exist in an array
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
     * NEW TASK - Create function that outputs true if sub array exist in an array
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
     * NEW TASK - Create function that checks if date is less than minimum
     * Passed all test
     */
    private fun isMinimumDate(receiptDate: String, minDate: String): Boolean {
        val rDate = receiptDate.split('/')
        val mDate = minDate.split('/')
        var rDateNumerical = 0.00
        var mDateNumerical = 0.00
        for (index in 0..2) {
            rDateNumerical += rDate[index].toDouble()
            mDateNumerical += mDate[index].toDouble()
        }
        if (rDateNumerical < mDateNumerical)
            return false
        return true
    }

    /***
     * NEW TASK - Create function that that sorts the date of a ReceiptData object
     * passed all test
     */
    private fun sortByDate(receiptData: ArrayList<ReceiptData>): ArrayList<ReceiptData> {
        val receiptMap = mutableMapOf<ReceiptData, Double>()
        for (receipt in receiptData)
            receiptMap[receipt] = receipt.numericalDateValue()
        return ArrayList(receiptMap.toList().sortedBy { (_, value) -> value }.toMap().keys)
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
        loadImageStartTime = System.currentTimeMillis()
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_receipt, null)
        val ivReceipt = dialogLayout.findViewById<ImageView>(R.id.promptImage)
        val layoutLoading = dialogLayout.findViewById<RelativeLayout>(R.id.layoutLoading)
        builder.setTitle(uid)
        builder.setView(dialogLayout)
        builder.setPositiveButton("Get") { _, _ ->
            firestoreDB.collection(PATH_RECEIPT)
                .document(uid)
                .delete()
                .addOnSuccessListener {
                    Log.d(MAIN_ACTIVITY, "(BUILDER) Get Receipt")
                    val imageRef = firebaseStorageImage.child("${uid}.jpg")
                    imageRef.delete().addOnSuccessListener {
                        Log.d(MAIN_ACTIVITY, "Receipt Deleted")
                        showToast("Receipt Downloaded")
                        saveMediaToStorage(receiptImageBitmap!!, uid)
                        firebaseAnalytics.logEvent("get_receipt", Bundle().apply {
                            this.putString("uid", uid)
                        })
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
                pseudoTrace("receipt_load_image", loadImageStartTime!!)
            }
            .addOnFailureListener {
                Log.d(MAIN_ACTIVITY, "There was an error")
                layoutLoading.visibility = VISIBLE
                ivReceipt.visibility = GONE
                showToast("Error loading Receipt", 1500)
                builder.show().dismiss()
            }
        builder.show()
    }


}