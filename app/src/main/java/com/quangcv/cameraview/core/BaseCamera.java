package com.quangcv.cameraview.core;

import com.quangcv.cameraview.lib.CameraCallback;
import com.quangcv.cameraview.lib.CameraView;

public abstract class BaseCamera {

    protected CameraView cameraView;
    protected CameraCallback callback;

    protected BaseCamera(CameraView preview) {
        cameraView = preview;
    }

    public void setCallback(CameraCallback callback) {
        this.callback = callback;
    }

    public CameraCallback getCallback() {
        return callback;
    }

    public abstract void onSurfaceChanged();

    public abstract boolean start();

    public abstract void stop();

    public abstract void takePicture();

}