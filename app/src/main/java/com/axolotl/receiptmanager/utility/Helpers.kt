package com.axolotl.receiptmanager.utility

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.IntRange
import java.util.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.perf.ktx.performance
import android.util.Log

private lateinit var toast: Toast
private lateinit var toastHandler: android.os.Handler

inline fun <reified T : Any> Context.launchActivity(
    noinline modify: Intent.() -> Unit = {}
) {
    val intent = Intent(this, T::class.java)
    intent.modify()
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

fun Activity.showToast(message: String?, @IntRange(from = 100, to = 4000) duration: Int = 2000) {
    try {
        toast.cancel()
        try {
            toastHandler.removeCallbacksAndMessages(null)
        } catch (e: Exception) {

        }
    } catch (e: Exception) {
    } finally {
        if (message.isNullOrBlank()) {
            try {
                toast.cancel()
            } catch (e: Exception) {
            }
        } else {
            toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()
            toastHandler = android.os.Handler(Looper.getMainLooper())
            toastHandler.postDelayed({
                try {
                    toast.cancel()
                } catch (e: Exception) {
                }
            }, duration.toLong())
        }
    }
}

fun isValidEmail(email: String): Boolean {
    if (email.isNullOrBlank()) {
        return false
    }
    val requiredSubString = arrayOf("@", ".com")
    if (email.isBlank()) {
        return false
    }
    requiredSubString.forEach {
        if (!email.contains(it)) return false
    }
    return true
}

fun Activity.hideKeyboard() {
    hideKeyboard(currentFocus ?: View(this))
}

fun Context.hideKeyboard(view: View) {
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

fun pseudoTrace(traceName: String, startTime: Long) {
    val traceDuration = System.currentTimeMillis() - startTime
    val initialHandler = android.os.Handler(Looper.getMainLooper())
    initialHandler.postDelayed({
        val traceHandler = android.os.Handler(Looper.getMainLooper())
        val trace = Firebase.performance.newTrace(traceName)
        Log.d(HELPER, "$traceName - $traceDuration")
        trace.start()
        traceHandler.postDelayed({
            trace.stop()
        }, traceDuration)
    }, 0)
}