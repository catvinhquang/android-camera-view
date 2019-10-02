package com.quangcv.cameraview.core;

import com.quangcv.cameraview.lib.AspectRatio;
import com.quangcv.cameraview.lib.CameraCallback;
import com.quangcv.cameraview.lib.CameraView;

import java.util.Set;

public abstract class BaseCamera {

    protected CameraCallback callback;
    protected CameraView cameraView;

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

    public abstract void setFacing(int facing);

    public abstract int getFacing();

    public abstract Set<AspectRatio> getSupportedAspectRatios();

    public abstract boolean setAspectRatio(AspectRatio ratio);

    public abstract AspectRatio getAspectRatio();

    public abstract void setAutoFocus(boolean autoFocus);

    public abstract boolean getAutoFocus();

    public abstract void setFlash(int flash);

    public abstract int getFlash();

    public abstract void takePicture();

    public abstract void setDisplayOrientation(int displayOrientation);

}
