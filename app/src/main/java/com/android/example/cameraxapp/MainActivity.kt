package com.android.example.cameraxapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.example.cameraxapp.databinding.ActivityMainBinding
import com.google.common.util.concurrent.ListenableFuture
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


typealias LumaListener = (luma: Double) -> Unit

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

//         Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }


    private fun startCamera() {
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

            // Select back camera as a default
            val mCameraId = 0
            val cameraSelector = CameraSelector.Builder().addCameraFilter(MyCameraFilter1("$mCameraId")).build()

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
    }


//    private fun startCamera() {
//        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
//            ProcessCameraProvider.getInstance(this)
//
//        imageCapture = ImageCapture.Builder().build();
//        cameraProviderFuture.addListener({
//            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get();
//
//            val preview = Preview.Builder()
//                .build()
//                .also {
//                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
//                }
//            val mCameraId =0
//            Log.d(
//                "AJSHGdLAS",
//                "${packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_EXTERNAL)}"
//            )
//
//            val cameraSelector =
//                CameraSelector.Builder().addCameraFilter(MyCameraFilter1("$mCameraId")).build()
//
//            try {
//                cameraProvider.unbindAll()
//                cameraProvider.bindToLifecycle(
//                    this,
//                    cameraSelector,
//                    preview,
//                    imageCapture
//                )
//            } catch (e: ExecutionException) {
//                e.printStackTrace();
//                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show();
//            } catch (e: InterruptedException) {
//                e.printStackTrace();
//                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show();
//            } catch (e: IllegalArgumentException) {
//                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show();
//            }
//        }, ContextCompat.getMainExecutor(this))
//    }
//    private fun startCamera() {
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//
//
//        val preview = Preview.Builder()
//            .build()
//            .also {
//                it.setSurfaceProvider(viewBinding.viewFinder.createSurfaceProvider())
//            }
//
//
//
//        imageCapture = ImageCapture.Builder().build()
//        cameraProviderFuture.addListener({
//
//             cameraProvider = cameraProviderFuture.get()
//            val cameraSelector = CameraSelector.Builder().addCameraFilter(MyCameraFilter()).build()
//                try {
//
//                    cameraProvider?.unbindAll()
//                    cameraProvider?.bindToLifecycle(this, cameraSelector, preview, imageCapture)
//
//                } catch (e: ExecutionException) {
//                    e.printStackTrace()
//                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
//                } catch (e: InterruptedException) {
//                    e.printStackTrace()
//                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
//                } catch (e: IllegalArgumentException) {
//                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
//                }
//            }, ContextCompat.getMainExecutor(this)
//        )
//    }

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
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

}