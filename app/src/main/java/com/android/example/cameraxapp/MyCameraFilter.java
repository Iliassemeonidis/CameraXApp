//package com.android.example.cameraxapp;
//
//import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;
//
//import android.annotation.SuppressLint;
//import android.util.Log;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.OptIn;
//import androidx.camera.core.Camera;
//import androidx.camera.core.CameraFilter;
//
//import java.util.Iterator;
//import java.util.LinkedHashSet;
//
//@SuppressLint("UnsafeExperimentalUsageError")
//@OptIn(markerClass = androidx.camera.core.ExperimentalCameraFilter.class)
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
//}
