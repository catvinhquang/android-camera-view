package com.quangcv.cameraview;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.quangcv.cameraview.sample.CameraView;

/**
 * Created by QuangCV on 31-Aug-2019
 **/

public class CameraActivity extends Activity {

    // TODO quangcv
    // step 1: preview
    // step 2: mask
    // step 3: capture
    CameraView v;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        v = new CameraView(this);
        v.setFacing(CameraView.FACING_FRONT);
        setContentView(v);
    }

    @Override
    protected void onResume() {
        super.onResume();
        v.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        v.stop();
    }


}