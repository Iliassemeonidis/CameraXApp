package com.android.example.cameraxapp

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.hardware.usb.UsbManager.EXTRA_DEVICE
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.android.example.cameraxapp.databinding.ActivityMainBinding
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


typealias LumaListener = (luma: Double) -> Unit

private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"

@RequiresApi(Build.VERSION_CODES.M)
@ExperimentalCamera2Interop
class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var bytes: ByteArray = byteArrayOf()
    private val TIMEOUT = 100
    private val forceClaim = true
    lateinit var usbManager: UsbManager

    lateinit var accessory: UsbAccessory
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var inputStream: FileInputStream? = null
    private var outputStream: FileOutputStream? = null


    private val usbReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val device: UsbDevice? = intent.getParcelableExtra(EXTRA_DEVICE)

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {

                        device?.getInterface(0)?.also { intf ->
                            intf.getEndpoint(0)?.also { endpoint ->
                                usbManager.openDevice(device)?.apply {
                                    claimInterface(intf, forceClaim)
                                    lifecycleScope.launch(
                                        Dispatchers.Main
                                                + SupervisorJob()
                                                + CoroutineExceptionHandler{_,it ->Log.d("ERROR11", "${it.message}") }
                                    ) {
                                        withContext(Dispatchers.IO) {
                                            bulkTransfer(endpoint, bytes, bytes.size, TIMEOUT) //do in another thread
                                        }
                                    }

                                }
                            }
                        }
//                        device?.apply {
//
//
//                            //startCamera()
//                        }
                    } else {
                        Log.d(TAG, "permission denied for device $device")
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        chekPermishAndStartCanera()

        provide()

    }

    private fun chekPermishAndStartCanera() {
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }


    @RequiresApi(Build.VERSION_CODES.S)
    private fun provide() {

        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbReceiver, filter)

        usbManager = getSystemService(USB_SERVICE) as UsbManager

        val permissionIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_MUTABLE
        )
        val deviceList: HashMap<String, UsbDevice> = usbManager.deviceList
        var device1 : UsbDevice? = null


        deviceList.values.forEach { device ->
            device1  = device
            usbManager.requestPermission(device, permissionIntent)
        }
        println(device1)
        println(device1)

                    lifecycleScope.launch(
                        Dispatchers.Main
                                + SupervisorJob()
                                + CoroutineExceptionHandler{_,it ->Log.d("ERROR11", "${it.message}") }
                    ) {
                        withContext(Dispatchers.IO) {
                            device1?.getInterface(0)?.also { intf ->
                                intf.getEndpoint(0)?.also { endpoint ->
                                    usbManager.openDevice(device1)?.apply {
                                        claimInterface(intf, forceClaim)
                                        bulkTransfer(endpoint, bytes, bytes.size, TIMEOUT)
                        }
                    }

                }
            }
        }
    }

    //Варинат №1
      private fun startCamera() {
        // Получаем жц владельца для связки
         val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

         cameraProviderFuture.addListener({

             // Используется для привязки жизненного цикла камер к владельцу жизненного цикла.
             val cameraProvider = cameraProviderFuture.get()

             // Preview предворительный просмотр(наша вьюшка)
             val preview = Preview.Builder()
                 .build()
                 .also {
                     it.setSurfaceProvider(viewBinding.previewView.surfaceProvider)
                 }

             // Выбираем камеру
             val cameraSelector = CameraSelector.Builder().addCameraFilter(MyCameraFilter1("0")).build()

             try {
                 // Отменить привязку
                 cameraProvider.unbindAll()

                 // Bind use cases to camera
                 cameraProvider.bindToLifecycle(
                     this,
                     cameraSelector,
                     preview
                 )

             } catch(exc: Exception) {
                 Log.e(TAG, "Use case binding failed", exc)
             }

         }, ContextCompat.getMainExecutor(this))
     }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()

    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }




    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

}