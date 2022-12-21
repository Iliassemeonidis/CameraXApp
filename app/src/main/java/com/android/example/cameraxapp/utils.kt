package com.android.example.cameraxapp

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import java.io.FileInputStream
import java.io.FileOutputStream

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

/*  private fun startCamera() {
      val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
          ProcessCameraProvider.getInstance(this)

      imageCapture = ImageCapture.Builder().build();
      cameraProviderFuture.addListener({
          val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get();

          val preview = Preview.Builder()
              .build()

          val mCameraManager = getSystemService(CAMERA_SERVICE) as CameraManager


          var a =    getNextCameraId(mCameraManager, "2")

          val cameraSelector : CameraSelector =
              CameraSelector.Builder().addCameraFilter(MyCameraFilter1(a!!)).build()

          preview.setSurfaceProvider(viewBinding.previewView.surfaceProvider)

          try {
              cameraProvider.unbindAll()
              cameraProvider.bindToLifecycle(
                  this,
                  cameraSelector,
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
  }*/


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
                        thisCamId == cam2Infos[2].cameraId
                    }
                }.build()
        }
        else -> null
    }
}





fun filterCompatibleCameras(cameraIds: Array<String>,
                            cameraManager: CameraManager
): List<String> {
    return cameraIds.filter {
        val characteristics = cameraManager.getCameraCharacteristics(it)
        characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.contains(
            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE) ?: false
    }
}

fun filterCameraIdsFacing(cameraIds: List<String>, cameraManager: CameraManager,
                          facing: Int): List<String> {
    return cameraIds.filter {
        val characteristics = cameraManager.getCameraCharacteristics(it)
        characteristics.get(CameraCharacteristics.LENS_FACING) == facing
    }
}

@RequiresApi(Build.VERSION_CODES.M)
fun getNextCameraId(cameraManager: CameraManager, currCameraId: String? = null): String? {
    // Get all front, back and external cameras in 3 separate lists
    val cameraIds = filterCompatibleCameras(cameraManager.cameraIdList, cameraManager)
    val backCameras = filterCameraIdsFacing(
        cameraIds, cameraManager, CameraMetadata.LENS_FACING_BACK)
    val frontCameras = filterCameraIdsFacing(
        cameraIds, cameraManager, CameraMetadata.LENS_FACING_FRONT)
    val externalCameras = filterCameraIdsFacing(
        cameraIds, cameraManager, CameraMetadata.LENS_FACING_EXTERNAL)

    // The recommended order of iteration is: all external, first back, first front
    val allCameras = (externalCameras + listOf(
        backCameras.firstOrNull(), frontCameras.firstOrNull())).filterNotNull()

    // Get the index of the currently selected camera in the list
    val cameraIndex = allCameras.indexOf(currCameraId)

    // The selected camera may not be in the list, for example it could be an
    // external camera that has been removed by the user
    return if (cameraIndex == -1) {
        // Return the first camera from the list
        allCameras.getOrNull(0)
    } else {
        // Return the next camera from the list, wrap around if necessary
        allCameras.getOrNull((cameraIndex + 1) % allCameras.size)
    }
}




//private fun openAccessory() {
//    Log.d(MainActivity.TAG, "openAccessory: $accessory")
//    fileDescriptor = usbManager.openAccessory(accessory)
//    fileDescriptor?.fileDescriptor?.also { fd ->
//        inputStream = FileInputStream(fd)
//        outputStream = FileOutputStream(fd)
//        val thread = Thread(  "AccessoryThread")
//        thread.start()
//    }
//}