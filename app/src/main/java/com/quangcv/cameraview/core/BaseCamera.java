package com.quangcv.cameraview.core;

import com.quangcv.cameraview.lib.AspectRatio;
import com.quangcv.cameraview.lib.CameraCallback;
import com.quangcv.cameraview.lib.CameraView;

import java.util.Set;

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

    public abstract boolean start();

    public abstract void stop();

    public abstract boolean isCameraOpened();

    public abstract Set<AspectRatio> getSupportedAspectRatios();

    public abstract boolean setAspectRatio(AspectRatio ratio);

    public abstract AspectRatio getAspectRatio();

    public abstract void takePicture();

    public abstract void setDisplayOrientation(int displayOrientation);

}
