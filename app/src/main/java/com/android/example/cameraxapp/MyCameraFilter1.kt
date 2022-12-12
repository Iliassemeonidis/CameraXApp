package com.android.example.cameraxapp

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.pm.PackageManager.FEATURE_CAMERA_EXTERNAL
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraInfo
import androidx.camera.core.impl.CameraInfoInternal
import androidx.core.util.Preconditions
@SuppressLint("UnsafeOptInUsageError")
class MyCameraFilter1(private val mId: String) : CameraFilter {

    private val TAG = "CameraIdCameraFilter"

    @SuppressLint("RestrictedApi")
    override fun filter(cameraInfos: MutableList<CameraInfo>): MutableList<CameraInfo> {

        val result = mutableListOf<CameraInfo>()
        cameraInfos.forEach {
            Preconditions.checkArgument(
                it is CameraInfoInternal,
                "The camera info doesn't contain internal implementation."
            )

            it as CameraInfoInternal
            val id = it.cameraId

            Log.d(TAG, "id: $id")
            //测试发现有的ID为/dev/video0，有的为0；故mId传0，1即可
            if (id.contains(mId)) {
                result.add(it)
            }
        }

val packageManager = PackageManager.FEATURE_CAMERA_EXTERNAL

        Log.d(TAG, "PackageManager: $packageManager")
        Log.d(TAG, "PackageManager: $packageManager")
        return result
    }
}