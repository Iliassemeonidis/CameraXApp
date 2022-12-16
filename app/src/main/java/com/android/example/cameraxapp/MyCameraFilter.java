//package com.android.example.cameraxapp;
//
//import android.annotation.SuppressLint;
//import android.util.Log;
//
//import androidx.annotation.NonNull;
//import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
//import androidx.camera.core.Camera;
//import androidx.camera.core.CameraFilter;
//import androidx.camera.core.ExperimentalCameraFilter;
//
//import java.util.Iterator;
//import java.util.LinkedHashSet;
//
//@SuppressLint("UnsafeExperimentalUsageError")
//@ExperimentalCamera2Interop
//@ExperimentalCameraFilter
//public class MyCameraFilter implements CameraFilter {
//    private static final String TAG = "myt";
//
//    @SuppressLint("RestrictedApi")
//    @NonNull
//    @Override
//    public LinkedHashSet<Camera> filter(@NonNull LinkedHashSet<Camera> cameras) {
//        Log.i(TAG, "cameras size: " + cameras.size());
//        Iterator<Camera> cameraIterator = cameras.iterator();
//        Camera camera = null;
//        while (cameraIterator.hasNext()) {
//            camera = cameraIterator.next();
//            String getImplementationType = camera.getCameraInfo().getImplementationType();
//            Log.i(TAG, "getImplementationType: " + getImplementationType);
//        }
//        LinkedHashSet linkedHashSet = new LinkedHashSet<>();
//        linkedHashSet.add(camera); // 最后一个camera
//        return linkedHashSet;
//    }
//
//}
