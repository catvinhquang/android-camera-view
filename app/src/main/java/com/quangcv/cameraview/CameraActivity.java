package com.quangcv.cameraview;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.quangcv.cameraview.sample.CameraView;
import com.quangcv.cameraview.sample.Constants;

/**
 * Created by QuangCV on 31-Aug-2019
 **/

public class CameraActivity extends Activity {

    private static final int REQUEST_CAMERA_PERMISSION = 1;

    // TODO quangcv
    // step 1: preview
    // step 2: mask
    // step 3: capture
    CameraView cameraView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cameraView = new CameraView(this);
        cameraView.setFacing(Constants.Facing.FACING_FRONT);
        setContentView(cameraView);
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
                Toast.makeText(this, R.string.camera_permission_not_granted, Toast.LENGTH_SHORT).show();
            }
            // No need to start camera here; it is handled by onResume
        }
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

}