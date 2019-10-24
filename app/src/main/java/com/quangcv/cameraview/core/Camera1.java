package com.quangcv.cameraview.core;

import android.hardware.Camera;

import com.quangcv.cameraview.lib.CameraView;

import java.io.IOException;
import java.util.List;

public class Camera1 extends BaseCamera {

    private boolean isPictureCaptureInProgress = false;

    private Camera camera;
    private Camera.Parameters parameters;

    public Camera1(CameraView preview) {
        super(preview);
    }

    @Override
    public void onSurfaceChanged() {
        if (camera != null) {
            setUpPreview();
            adjustCameraParameters();
        }
    }

    @Override
    public boolean start() {
        if (camera == null) {
            // open camera
            camera = Camera.open(0);
            parameters = camera.getParameters();

            adjustCameraParameters();
            camera.setDisplayOrientation(90);
            callback.onCameraOpened();

            // start preview
            if (cameraView.isReady()) {
                setUpPreview();
            }
            camera.startPreview();
        }
        return true;
    }

    @Override
    public void stop() {
        if (camera != null) {
            camera.stopPreview();
        }
        release();
    }

    @Override
    public void takePicture() {
        if (camera != null && !isPictureCaptureInProgress) {
            isPictureCaptureInProgress = true;
            camera.takePicture(null, null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    isPictureCaptureInProgress = false;
                    callback.onPictureTaken(data);
                    camera.cancelAutoFocus();
                    camera.startPreview();
                }
            });
        }
    }

    private void setUpPreview() {
        try {
            camera.setPreviewDisplay(cameraView.getHolder());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void adjustCameraParameters() {
        // TODO quangcv: méo hình preview
        List<Camera.Size> list = parameters.getSupportedPreviewSizes();
        Camera.Size s = list.get(list.size() - 1);
        parameters.setPreviewSize(s.width, s.height);

        list = parameters.getSupportedPictureSizes();
        s = list.get(list.size() - 1);
        parameters.setPictureSize(s.width, s.height);

        parameters.setRotation(90);
        camera.setParameters(parameters);
    }

    private void release() {
        if (camera != null) {
            camera.release();
            camera = null;
            callback.onCameraClosed();
        }
    }

}