package com.android.example.cameraxapp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
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
import com.android.example.cameraxapp.databinding.ActivityMainBinding
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


typealias LumaListener = (luma: Double) -> Unit

private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"

@ExperimentalCamera2Interop
class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var bytes: ByteArray = byteArrayOf()
    private val TIMEOUT = 0
    private val forceClaim = true
    lateinit var usbManager: UsbManager

    lateinit var accessory: UsbAccessory
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var inputStream: FileInputStream? = null
    private var outputStream: FileOutputStream? = null


//
   /* private val usbReceiver = object : BroadcastReceiver() {

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
    }*/



    private val usbReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                     accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY)!!

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        accessory.apply {
                            openAccessory() //call method to set up accessory communication
                        }
                    } else {
                        Log.d(TAG, "permission denied for accessory $accessory")
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

   /*     println(packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_EXTERNAL))
        println(packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_EXTERNAL))*/

        provide()

      //  chekPermishAndStartCanera()
    }

    private fun chekPermishAndStartCanera() {
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_EXTERNAL)) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }


    private fun openAccessory() {
        Log.d(TAG, "openAccessory: $accessory")
        fileDescriptor = usbManager.openAccessory(accessory)
        fileDescriptor?.fileDescriptor?.also { fd ->
            inputStream = FileInputStream(fd)
            outputStream = FileOutputStream(fd)
            val thread = Thread(  "AccessoryThread")
            thread.start()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun provide() {

        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbReceiver, filter)

//        usbManager = getSystemService(USB_SERVICE) as UsbManager
//
//        val deviceList: HashMap<String, UsbDevice> = usbManager.deviceList




//        val permissionIntent = PendingIntent.getBroadcast(
//            this,
//            0,
//            Intent(ACTION_USB_PERMISSION),
//            PendingIntent.FLAG_MUTABLE
//        )
//
//        deviceList.values.forEach { device ->
//            usbManager.requestPermission(device, permissionIntent)
//        }

    }

    //Варинат №1
/*      private fun startCamera() {
         val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

         cameraProviderFuture.addListener({
             // Used to bind the lifecycle of cameras to the lifecycle owner
             val cameraProvider = cameraProviderFuture.get()

             // Preview
             val preview = Preview.Builder()
                 .build()
                 .also {
                     it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                 }

             imageCapture = ImageCapture.Builder()
                 .build()

             // Select back camera as a default
             val cameraSelector = CameraSelector.Builder().addCameraFilter(MyCameraFilter1("0")).build()

             try {
                 // Unbind use cases before rebinding
                 cameraProvider.unbindAll()

                 // Bind use cases to camera
                 cameraProvider.bindToLifecycle(
                     this,
                     cameraSelector,
                     preview,
                     imageCapture
                 )

             } catch(exc: Exception) {
                 Log.e(TAG, "Use case binding failed", exc)
             }

         }, ContextCompat.getMainExecutor(this))
     }*/

    //Варинат №2
    /* private fun startCamera() {
         val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

         val preview = Preview.Builder()
             .build()
             .also {
                 it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
             }
         imageCapture = ImageCapture.Builder().build()
         cameraProviderFuture.addListener(
             {

                 val cameraProvider = cameraProviderFuture.get()
                 val cameraSelector =
                     CameraSelector.Builder().addCameraFilter(MyCameraFilter1("0")).build()
                 try {

                     cameraProvider?.unbindAll()
                     cameraProvider?.bindToLifecycle(this, cameraSelector, preview, imageCapture)

                 } catch (e: ExecutionException) {
                     e.printStackTrace()
                     Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                 } catch (e: InterruptedException) {
                     e.printStackTrace()
                     Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                 } catch (e: IllegalArgumentException) {
                     Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                 }
             }, ContextCompat.getMainExecutor(this)
         )
     }*/

    //Варинат №3
    private fun startCamera() {
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
            ProcessCameraProvider.getInstance(this)

        imageCapture = ImageCapture.Builder().build();
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get();

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            println(packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_EXTERNAL))
            val cameraSelector =
                selectExternalOrBestCamera(cameraProvider)

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector!!,
                    preview,
                    imageCapture
                )
            } catch (e: ExecutionException) {
                e.printStackTrace();
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show();
            } catch (e: InterruptedException) {
                e.printStackTrace();
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show();
            } catch (e: IllegalArgumentException) {
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this))
    }


//    //Варинат №4
    /*private fun startCamera() {
        // Привязка к жц
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)



        cameraProviderFuture.addListener({

            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }


            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                        Log.d(TAG, "Average luminosity: $luma")
                    })
                }


            imageCapture = ImageCapture.Builder()
                .build()

            // Select camera
            val cameraSelector = selectExternalOrBestCamera(cameraProvider)

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
//                cameraProvider.bindToLifecycle(
//                    this,
//                    cameraSelector!!,
//                    preview,
//                    imageCapture,
//                    imageAnalyzer
//                )
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector!!,
                    preview,
                    imageCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }*/


    private fun selectExternalOrBestCamera(provider: ProcessCameraProvider): CameraSelector? {
        val cam2Infos = provider.availableCameraInfos.map {
            Camera2CameraInfo.from(it)
        }.sortedByDescending {
            // HARDWARE_LEVEL is Int type, with the order of:
            // LEGACY < LIMITED < FULL < LEVEL_3 < EXTERNAL
            it.getCameraCharacteristic(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        }

        return when {
            cam2Infos.isNotEmpty() -> {
                CameraSelector.Builder()
                    .addCameraFilter {
                        it.filter { camInfo ->
                            // cam2Infos[0] is either EXTERNAL or best built-in camera
                            val thisCamId = Camera2CameraInfo.from(camInfo).cameraId
                            thisCamId == cam2Infos[0].cameraId
                        }
                    }.build()
            }
            else -> null
        }
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

//    override fun onRequestPermissionsResult(
//        requestCode: Int, permissions: Array<String>, grantResults:
//        IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == REQUEST_CODE_PERMISSIONS) {
//            if (allPermissionsGranted()) {
//        //        startCamera()
//            } else {
//                Toast.makeText(
//                    this,
//                    "Permissions not granted by the user.",
//                    Toast.LENGTH_SHORT
//                ).show()
//                finish()
//            }
//        }
//    }


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