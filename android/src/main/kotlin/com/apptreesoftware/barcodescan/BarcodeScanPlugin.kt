package com.apptreesoftware.barcodescan

import android.app.Activity
import android.content.Intent
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.Registrar
import android.content.Context
import android.util.Log

class BarcodeScanPlugin(val activity: Activity, val registrar :Registrar): MethodCallHandler
     {
  var result : Result? = null


  companion object {
    @JvmStatic
    fun registerWith(registrar: Registrar): Unit {
      val channel = MethodChannel(registrar.messenger(), "com.apptreesoftware.barcode_scan")
        if (registrar.activity() != null) {
            val plugin = BarcodeScanPlugin(registrar.activity(), registrar)
            channel.setMethodCallHandler(plugin)
      }
    }
  }

  override fun onMethodCall(call: MethodCall, result: Result): Unit {
    if (call.method.equals("scan")) {
      this.result = result
        registrar.addActivityResultListener(BarcodeScanResultListener(activity, result))
      showBarcodeView()
    } else {
      result.notImplemented()
    }
  }

  private fun showBarcodeView() {
    val intent = Intent(activity, BarcodeScannerActivity::class.java)
    activity.startActivityForResult(intent, 100)
  }

}

class BarcodeScanResultListener(private val activity: Activity, private val result: Result?) : PluginRegistry.ActivityResultListener {

    // onActivityResult is being called twice in rare cases, this is a workaround for that.
    /** This was the stack trace.
     * Caused by java.lang.IllegalStateException: Reply already submitted
    at io.flutter.embedding.engine.dart.DartMessenger$Reply.reply(:35)
    at io.flutter.plugin.common.MethodChannel$IncomingMethodCallHandler$1.success(:14)
    at com.apptreesoftware.barcodescan.a.onActivityResult(:67)
    at io.flutter.app.FlutterPluginRegistry.onActivityResult(:18)
    at io.flutter.app.FlutterActivityDelegate.onActivityResult(:6)
    at io.flutter.app.FlutterActivity.onActivityResult(:2)
    at com.cronometer.cronometer.MainActivity.onActivityResult()
    at android.app.Activity.dispatchActivityResult(Activity.java:7305)
    at android.app.ActivityThread.deliverResults(ActivityThread.java:4385)
    at android.app.ActivityThread.handleSendResult(ActivityThread.java:4433)
    at android.app.ActivityThread.-wrap20()
    at android.app.ActivityThread$H.handleMessage(ActivityThread.java:1692)
    at android.os.Handler.dispatchMessage(Handler.java:106)
    at android.os.Looper.loop(Looper.java:164)
    at android.app.ActivityThread.main(ActivityThread.java:6647)
    at java.lang.reflect.Method.invoke(Method.java)
    at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:438)
    at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:811)
     */
    private var alreadyCalled : Boolean = false

    override fun onActivityResult(code: Int, resultCode: Int, data: Intent?): Boolean {
        if (!alreadyCalled && code == 100) {
            alreadyCalled = true
            if (resultCode == Activity.RESULT_OK) {
                val barcode = data?.getStringExtra("SCAN_RESULT")
                if (barcode != null) {
                    try {
                        val sharedPreferences = activity.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        val jsonString = """
            {
               "from":"BarcodeScanner",
               "data":" """ + barcode + """"
            }        
            """
                        editor.putString("flutter.external_result", jsonString)
                        editor.apply()
                    } catch (e : Exception) {
                        Log.e("BARCODESCAN", "Error writing to shared prefs", e);
                    }
                }
                barcode?.let { this.result?.success(barcode) }
            } else {
                val errorCode = data?.getStringExtra("ERROR_CODE")
                this.result?.error(errorCode, null, null)
            }
            return true
        }
        return false
    }

}
