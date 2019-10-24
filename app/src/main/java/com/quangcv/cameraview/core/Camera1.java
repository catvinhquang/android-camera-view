package com.quangcv.cameraview.core;

import android.hardware.Camera;

import com.quangcv.cameraview.lib.AspectRatio;
import com.quangcv.cameraview.lib.CameraView;
import com.quangcv.cameraview.lib.Size;
import com.quangcv.cameraview.lib.SizeMap;
import com.quangcv.cameraview.lib.SurfaceCallback;

import java.io.IOException;
import java.util.SortedSet;

public class Camera1 extends BaseCamera {

    private boolean isPictureCaptureInProgress = false;
    private boolean showingPreview = false;

    private SizeMap previewSizes = new SizeMap();
    private SizeMap pictureSizes = new SizeMap();

    private Camera camera;
    private Camera.Parameters parameters;
    private AspectRatio aspectRatio = AspectRatio.of(4, 3);

    public Camera1(CameraView preview) {
        super(preview);
        preview.setSurfaceCallback(new SurfaceCallback() {
            @Override
            public void onSurfaceChanged() {
                if (camera != null) {
                    setUpPreview();
                    adjustCameraParameters();
                }
            }
        });
    }

    @Override
    public boolean start() {
        openCamera();
        if (cameraView.isReady()) {
            setUpPreview();
        }
        showingPreview = true;
        camera.startPreview();
        return true;
    }

    @Override
    public void stop() {
        if (camera != null) {
            camera.stopPreview();
        }
        showingPreview = false;
        releaseCamera();
    }

    private void setUpPreview() {
        try {
            camera.setPreviewDisplay(cameraView.getHolder());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isCameraOpened() {
        return camera != null;
    }

    @Override
    public void takePicture() {
        if (!isCameraOpened()) {
            throw new IllegalStateException("Camera is not ready. Call start() before takePicture().");
        }
        takePictureInternal();
    }

    private void takePictureInternal() {
        if (!isPictureCaptureInProgress) {
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

    private void openCamera() {
        if (camera != null) {
            releaseCamera();
        }
        camera = Camera.open(0);
        parameters = camera.getParameters();
        previewSizes.clear();
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            previewSizes.add(new Size(size.width, size.height));
        }

        pictureSizes.clear();
        for (Camera.Size size : parameters.getSupportedPictureSizes()) {
            pictureSizes.add(new Size(size.width, size.height));
        }

        adjustCameraParameters();
        camera.setDisplayOrientation(90);
        callback.onCameraOpened();
    }

    private void adjustCameraParameters() {
        SortedSet<Size> sizes = previewSizes.sizes(aspectRatio);
        Size size = chooseOptimalSize(sizes);

        // Always re-apply camera parameters
        // Largest picture size in this ratio
        final Size pictureSize = pictureSizes.sizes(aspectRatio).last();
        if (showingPreview) {
            camera.stopPreview();
        }
        // TODO quangcv: méo hình preview
        parameters.setPreviewSize(size.getWidth(), size.getHeight());
        parameters.setPictureSize(pictureSize.getWidth(), pictureSize.getHeight());
        parameters.setRotation(90);
        camera.setParameters(parameters);
        if (showingPreview) {
            camera.startPreview();
        }
    }

    private Size chooseOptimalSize(SortedSet<Size> sizes) {
        if (!cameraView.isReady()) {
            return sizes.first();
        }

        int surfaceWidth = cameraView.getSurfaceWidth();
        int surfaceHeight = cameraView.getSurfaceHeight();
        Size result = null;
        for (Size size : sizes) {
            if (surfaceWidth <= size.getWidth() && surfaceHeight <= size.getHeight()) {
                return size;
            }
            result = size;
        }
        return result;
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
            callback.onCameraClosed();
        }
    }

}