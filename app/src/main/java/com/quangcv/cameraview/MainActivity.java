package com.quangcv.cameraview;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.quangcv.cameraview.CameraCallback;
import com.quangcv.cameraview.CameraView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This demo app saves the taken picture to a constant file.
 * $ adb pull /sdcard/Android/data/com.google.android.cameraview.demo/files/Pictures/picture.jpg
 */

public class MainActivity extends Activity
        implements ActivityCompat.OnRequestPermissionsResultCallback, CameraCallback {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 1;

    private CameraView cameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cameraView = new CameraView(this);
        cameraView.setCallback(this);
        cameraView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.takePicture();
            }
        });

        setContentView(cameraView);
        Toast.makeText(this, "Touch screen to capture", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraView.start();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.stop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (permissions.length != 1 || grantResults.length != 1) {
                throw new RuntimeException("Error on requesting camera permission.");
            }
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera app cannot do anything without camera permission", Toast.LENGTH_SHORT).show();
            }
            // No need to start camera here; it is handled by onResume
        }
    }

    @Override
    public void onCameraOpened() {
        Log.d(TAG, "onCameraOpened");
    }

    @Override
    public void onCameraClosed() {
        Log.d(TAG, "onCameraClosed");
    }

    @Override
    public void onPictureTaken(final byte[] data) {
        Log.d(TAG, "onPictureTaken " + data.length);
        Toast.makeText(this, "Picture taken", Toast.LENGTH_SHORT).show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "picture.jpg");
                OutputStream os = null;
                try {
                    os = new FileOutputStream(file);
                    os.write(data);
                    os.close();
                } catch (Exception e) {
                    Log.w(TAG, "Cannot write to " + file, e);
                } finally {
                    if (os != null) {
                        try {
                            os.close();
                        } catch (IOException ignore) {
                        }
                    }
                }
            }
        }).start();
    }

}