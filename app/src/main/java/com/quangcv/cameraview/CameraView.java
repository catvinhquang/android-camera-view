package com.quangcv.cameraview;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.abs;

public class CameraView extends SurfaceView implements SurfaceHolder.Callback {

    private boolean isReady = false;
    private boolean isTakingPicture = false;
    private Camera camera;
    private CameraCallback callback;

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
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        isReady = width != 0 && height != 0;
        if (!ViewCompat.isInLayout(CameraView.this)) {
            startPreview();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isReady = false;
    }

    public void start() {
        if (camera == null) {
            camera = Camera.open(0);
            callback.onCameraOpened();
            startPreview();
        }
    }

    private void startPreview() {
        if (camera != null && isReady) {
            try {
                Camera.Parameters params = camera.getParameters();

                Camera.Size preSize = getPreviewSize(params.getSupportedPreviewSizes(),
                        getWidth(),
                        getHeight());
                params.setPreviewSize(preSize.width, preSize.height);

                // output size
                Camera.Size picSize = getPictureSize(params.getSupportedPictureSizes(),
                        preSize.width,
                        preSize.height);
                params.setPictureSize(picSize.width, picSize.height);

                // TODO quang: check focus feature is available
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE); // Auto focus

                // TODO quang: rotation formular
                params.setRotation(90); // Để xuất hình portrait
                camera.setDisplayOrientation(90); // Portrait preview

                camera.setParameters(params);
                camera.setPreviewDisplay(getHolder());
                camera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        if (camera != null) {
            camera.stopPreview();
        }
        release();
    }

    public void takePicture() {
        if (camera != null && !isTakingPicture) {
            isTakingPicture = true;
            camera.takePicture(null, null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    isTakingPicture = false;
                    callback.onPictureTaken(data);
                    camera.cancelAutoFocus();
                    camera.startPreview();
                }
            });
        }
    }

    private Camera.Size getPreviewSize(List<Camera.Size> supportedSizes,
                                       final int viewWidth,
                                       final int viewHeight) {
        return Collections.min(supportedSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size l, Camera.Size r) {
                int diff1 = abs(viewWidth - l.width) + abs(viewHeight - l.height);
                int diff2 = abs(viewWidth - r.width) + abs(viewHeight - r.height);
                return diff1 - diff2;
            }
        });
    }

    private Camera.Size getPictureSize(List<Camera.Size> supportedSizes,
                                       final int previewWidth,
                                       final int previewHeight) {
        List<Camera.Size> filtered = new ArrayList<>();
        double previewRatio = previewWidth / (double) previewHeight;
        for (Camera.Size s : supportedSizes) {
            double itemRatio = s.width / (double) s.height;
            if (itemRatio == previewRatio) {
                filtered.add(s);
            }
        }
        return Collections.max(filtered, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size l, Camera.Size r) {
                int diff1 = abs(previewWidth - l.width) + abs(previewHeight - l.height);
                int diff2 = abs(previewWidth - r.width) + abs(previewHeight - r.height);
                return diff1 - diff2;
            }
        });
    }

    private void release() {
        if (camera != null) {
            camera.release();
            camera = null;
            callback.onCameraClosed();
        }
    }

    public void setCallback(@NonNull CameraCallback callback) {
        this.callback = callback;
    }

}