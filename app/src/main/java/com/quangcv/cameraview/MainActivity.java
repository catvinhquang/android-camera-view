package com.quangcv.cameraview;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * This demo app saves the taken picture to a constant file.
 * $ adb pull /sdcard/Android/data/com.quangcv.cameraview/files/Pictures/picture.jpg
 */

public class MainActivity extends Activity
        implements ActivityCompat.OnRequestPermissionsResultCallback, CameraView.OnPictureTakenListener {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 1;

    private CameraView cameraView;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraView = findViewById(R.id.camera_view);
        cameraView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.takePicture(MainActivity.this);
            }
        });

        imageView = findViewById(R.id.image_view);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            if (cameraView.getVisibility() == View.VISIBLE) {
                cameraView.start();
            }
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
        if (requestCode == REQUEST_CAMERA_PERMISSION
                && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this,
                    "Camera app cannot do anything without camera permission",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPictureTaken(final Bitmap result) {
        Log.d(TAG, "onPictureTaken");

        cameraView.stop();
        cameraView.setVisibility(View.GONE);
        imageView.setImageBitmap(result);
        imageView.setVisibility(View.VISIBLE);

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "picture.jpg");
//                OutputStream os = null;
//                try {
//                    os = new FileOutputStream(file);
//                    result.compress(Bitmap.CompressFormat.JPEG, 100, os);
//                    os.close();
//                } catch (Exception e) {
//                    Log.w(TAG, "Cannot write to " + file, e);
//                } finally {
//                    if (os != null) {
//                        try {
//                            os.close();
//                        } catch (IOException ignore) {
//                        }
//                    }
//                }
//            }
//        }).start();
    }

    @Override
    public void onBackPressed() {
        if (imageView.getVisibility() == View.VISIBLE) {
            imageView.setImageBitmap(null);
            imageView.setVisibility(View.GONE);
            cameraView.start();
            cameraView.setVisibility(View.VISIBLE);
        }
    }
}