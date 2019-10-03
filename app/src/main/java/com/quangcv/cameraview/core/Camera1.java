package com.quangcv.cameraview.core;

import android.hardware.Camera;

import com.quangcv.cameraview.lib.AspectRatio;
import com.quangcv.cameraview.lib.CameraView;
import com.quangcv.cameraview.lib.Constants;
import com.quangcv.cameraview.lib.Size;
import com.quangcv.cameraview.lib.SizeMap;
import com.quangcv.cameraview.lib.SurfaceCallback;

import java.io.IOException;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class Camera1 extends BaseCamera {

    private static final int INVALID_CAMERA_ID = -1;

    private final AtomicBoolean isPictureCaptureInProgress = new AtomicBoolean(false);
    private final Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();
    private final SizeMap mPreviewSizes = new SizeMap();
    private final SizeMap mPictureSizes = new SizeMap();

    private int mCameraId;
    private int mDisplayOrientation;
    private boolean mShowingPreview;

    private Camera camera;
    private Camera.Parameters mCameraParameters;
    private AspectRatio mAspectRatio;

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
        chooseCamera();
        openCamera();
        if (cameraView.isReady()) {
            setUpPreview();
        }
        mShowingPreview = true;
        camera.startPreview();
        return true;
    }

    @Override
    public void stop() {
        if (camera != null) {
            camera.stopPreview();
        }
        mShowingPreview = false;
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
    public Set<AspectRatio> getSupportedAspectRatios() {
        SizeMap idealAspectRatios = mPreviewSizes;
        for (AspectRatio aspectRatio : idealAspectRatios.ratios()) {
            if (mPictureSizes.sizes(aspectRatio) == null) {
                idealAspectRatios.remove(aspectRatio);
            }
        }
        return idealAspectRatios.ratios();
    }

    @Override
    public boolean setAspectRatio(AspectRatio ratio) {
        if (mAspectRatio == null || !isCameraOpened()) {
            mAspectRatio = ratio;
            return true;
        } else if (!mAspectRatio.equals(ratio)) {
            final Set<Size> sizes = mPreviewSizes.sizes(ratio);
            if (sizes == null) {
                throw new UnsupportedOperationException(ratio + " is not supported");
            } else {
                mAspectRatio = ratio;
                adjustCameraParameters();
                return true;
            }
        }
        return false;
    }

    @Override
    public AspectRatio getAspectRatio() {
        return mAspectRatio;
    }

    @Override
    public void takePicture() {
        if (!isCameraOpened()) {
            throw new IllegalStateException("Camera is not ready. Call start() before takePicture().");
        }
        takePictureInternal();
    }

    void takePictureInternal() {
        if (!isPictureCaptureInProgress.getAndSet(true)) {
            camera.takePicture(null, null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    isPictureCaptureInProgress.set(false);
                    callback.onPictureTaken(data);
                    camera.cancelAutoFocus();
                    camera.startPreview();
                }
            });
        }
    }

    @Override
    public void setDisplayOrientation(int displayOrientation) {
        if (mDisplayOrientation != displayOrientation) {
            mDisplayOrientation = displayOrientation;
            if (isCameraOpened()) {
                mCameraParameters.setRotation(calcCameraRotation(displayOrientation));
                camera.setParameters(mCameraParameters);
                camera.setDisplayOrientation(calcDisplayOrientation(displayOrientation));
            }
        }
    }

    /**
     * This rewrites {@link #mCameraId} and {@link #mCameraInfo}.
     */
    private void chooseCamera() {
        if (Camera.getNumberOfCameras() > 0) {
            Camera.getCameraInfo(0, mCameraInfo);
            mCameraId = 0;
            return;
        }
        mCameraId = INVALID_CAMERA_ID;
    }

    private void openCamera() {
        if (camera != null) {
            releaseCamera();
        }
        camera = Camera.open(mCameraId);
        mCameraParameters = camera.getParameters();
        mPreviewSizes.clear();
        for (Camera.Size size : mCameraParameters.getSupportedPreviewSizes()) {
            mPreviewSizes.add(new Size(size.width, size.height));
        }

        mPictureSizes.clear();
        for (Camera.Size size : mCameraParameters.getSupportedPictureSizes()) {
            mPictureSizes.add(new Size(size.width, size.height));
        }

        if (mAspectRatio == null) {
            mAspectRatio = Constants.DEFAULT_ASPECT_RATIO;
        }

        adjustCameraParameters();
        camera.setDisplayOrientation(calcDisplayOrientation(mDisplayOrientation));
        callback.onCameraOpened();
    }

    private AspectRatio chooseAspectRatio() {
        AspectRatio r = null;
        for (AspectRatio ratio : mPreviewSizes.ratios()) {
            r = ratio;
            if (ratio.equals(Constants.DEFAULT_ASPECT_RATIO)) {
                return ratio;
            }
        }
        return r;
    }

    void adjustCameraParameters() {
        SortedSet<Size> sizes = mPreviewSizes.sizes(mAspectRatio);
        if (sizes == null) { // Not supported
            mAspectRatio = chooseAspectRatio();
            sizes = mPreviewSizes.sizes(mAspectRatio);
        }
        Size size = chooseOptimalSize(sizes);

        // Always re-apply camera parameters
        // Largest picture size in this ratio
        final Size pictureSize = mPictureSizes.sizes(mAspectRatio).last();
        if (mShowingPreview) {
            camera.stopPreview();
        }
        // TODO quangcv: méo hình preview
        mCameraParameters.setPreviewSize(size.getWidth(), size.getHeight());
        mCameraParameters.setPictureSize(pictureSize.getWidth(), pictureSize.getHeight());
        mCameraParameters.setRotation(calcCameraRotation(mDisplayOrientation));
        camera.setParameters(mCameraParameters);
        if (mShowingPreview) {
            camera.startPreview();
        }
    }

    private Size chooseOptimalSize(SortedSet<Size> sizes) {
        if (!cameraView.isReady()) {
            return sizes.first();
        }

        int surfaceWidth = cameraView.getSurfaceWidth();
        int surfaceHeight = cameraView.getSurfaceHeight();
        int dW;
        int dH;
        if (isLandscape(mDisplayOrientation)) {
            dW = surfaceHeight;
            dH = surfaceWidth;
        } else {
            dW = surfaceWidth;
            dH = surfaceHeight;
        }
        Size result = null;
        for (Size size : sizes) {
            if (dW <= size.getWidth() && dH <= size.getHeight()) {
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

    /**
     * Calculate display orientation
     * https://developer.android.com/reference/android/hardware/Camera.html#setDisplayOrientation(int)
     * <p>
     * This calculation is used for orienting the preview
     * <p>
     * Note: This is not the same calculation as the camera rotation
     *
     * @param screenOrientationDegrees Screen orientation in degrees
     * @return Number of degrees required to rotate preview
     */
    private int calcDisplayOrientation(int screenOrientationDegrees) {
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return (360 - (mCameraInfo.orientation + screenOrientationDegrees) % 360) % 360;
        } else {  // back-facing
            return (mCameraInfo.orientation - screenOrientationDegrees + 360) % 360;
        }
    }

    /**
     * Calculate camera rotation
     * <p>
     * This calculation is applied to the output JPEG either via Exif Orientation tag
     * or by actually transforming the bitmap. (Determined by vendor camera API implementation)
     * <p>
     * Note: This is not the same calculation as the display orientation
     *
     * @param screenOrientationDegrees Screen orientation in degrees
     * @return Number of degrees to rotate image in order for it to view correctly.
     */
    private int calcCameraRotation(int screenOrientationDegrees) {
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return (mCameraInfo.orientation + screenOrientationDegrees) % 360;
        } else {  // back-facing
            final int landscapeFlip = isLandscape(screenOrientationDegrees) ? 180 : 0;
            return (mCameraInfo.orientation + screenOrientationDegrees + landscapeFlip) % 360;
        }
    }

    private boolean isLandscape(int d) {
        return d == Constants.LANDSCAPE_90 || d == Constants.LANDSCAPE_270;
    }

}
