package com.quangcv.cameraview.lib;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.quangcv.cameraview.core.BaseCamera;
import com.quangcv.cameraview.core.Camera1;
import com.quangcv.cameraview.core.Camera2;

public class CameraView extends SurfaceView implements SurfaceHolder.Callback {

    private BaseCamera impl;
    private SurfaceCallback surfaceCallback;
    private int surfaceWidth;
    private int surfaceHeight;

    public CameraView(Context context) {
        this(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (isInEditMode()) return;

        SurfaceHolder holder = getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.addCallback(this);

        // TODO quangcv
//        if (Build.VERSION.SDK_INT < 21) {
//            impl = new Camera1(this);
//        } else {
        impl = new Camera2(this, context);
//        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        surfaceWidth = width;
        surfaceHeight = height;
        if (!ViewCompat.isInLayout(CameraView.this)) {
            surfaceCallback.onSurfaceChanged();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceWidth = surfaceHeight = 0;
    }

    public void start() {
        if (!impl.start()) {
            CameraCallback c = impl.getCallback();
            impl = new Camera1(this);
            impl.setCallback(c);
            impl.start();
        }
    }

    public void stop() {
        impl.stop();
    }

    public void takePicture() {
        impl.takePicture();
    }

    public void setCallback(@NonNull CameraCallback callback) {
        impl.setCallback(callback);
    }

    public void setSurfaceCallback(SurfaceCallback callback) {
        surfaceCallback = callback;
    }

    public int getSurfaceWidth() {
        return surfaceWidth;
    }

    public int getSurfaceHeight() {
        return surfaceHeight;
    }

    public boolean isReady() {
        return surfaceWidth != 0 && surfaceHeight != 0;
    }

}