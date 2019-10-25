package com.quangcv.cameraview.core;

import android.hardware.Camera;

import com.quangcv.cameraview.lib.CameraView;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.abs;

public class Camera1 extends BaseCamera {

    private boolean isTakingPicture = false;
    private Camera camera;

    public Camera1(CameraView preview) {
        super(preview);
    }

    @Override
    public boolean start() {
        if (camera == null) {
            camera = Camera.open(0);
            callback.onCameraOpened();
            startPreview();
        }
        return true;
    }

    @Override
    public void onSurfaceChanged() {
        startPreview();
    }

    private void startPreview() {
        if (camera != null && cameraView.isReady()) {
            try {
                Camera.Parameters params = camera.getParameters();

                Camera.Size s = getPreviewSize(params.getSupportedPreviewSizes(),
                        cameraView.getWidth(),
                        cameraView.getHeight());
                params.setPreviewSize(s.width, s.height);

                // output size
                s = getPictureSize(params.getSupportedPictureSizes(),
                        s.width,
                        s.height);
                params.setPictureSize(s.width, s.height);

                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE); // Auto focus
                params.setRotation(90); // Để xuất hình portrait

                camera.setParameters(params);
                camera.setDisplayOrientation(90); // Portrait preview
                camera.setPreviewDisplay(cameraView.getHolder());
                camera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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

    private void release() {
        if (camera != null) {
            camera.release();
            camera = null;
            callback.onCameraClosed();
        }
    }

    //////
    private Camera.Size getPreviewSize(List<Camera.Size> supportSizes,
                                       int viewWidth,
                                       int viewHeight) {
        Camera.Size previewSize = null;
        final float scrRatio = viewHeight / (float) viewWidth;
        if (supportSizes.size() > 0) {
            Collections.sort(supportSizes, new Comparator<Camera.Size>() {
                @Override
                public int compare(Camera.Size lhs, Camera.Size rhs) {
                    float lRatio = lhs.width / (float) lhs.height;
                    float rRatio = rhs.width / (float) rhs.height;
                    //compute the size ratio difference comparing to screen
                    lRatio = lRatio < scrRatio ? scrRatio / lRatio : lRatio / scrRatio;
                    rRatio = rRatio < scrRatio ? scrRatio / rRatio : rRatio / scrRatio;
                    if (lRatio < 0.9 * rRatio) {
                        return -1;
                    } else if (lRatio > 1.1 * rRatio) {
                        return 1;
                    } else {
                        if (lhs.width > rhs.width) {
                            return -1;
                        } else if (lhs.width < rhs.width) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                }
            });
            int i;
            for (i = 0; i < supportSizes.size(); i++) {
                if (supportSizes.get(i).width * supportSizes.get(i).height < viewWidth * viewHeight) {
                    break;
                }
            }
            i--;
            if (i < 0)
                i = 0; // get biggest size.

            previewSize = supportSizes.get(i);
        }

        return previewSize;
    }

    private static Camera.Size getPictureSize(List<Camera.Size> supportedSizes,
                                              final int previewWidth,
                                              final int previewHeight) {
        return Collections.min(supportedSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size o1, Camera.Size o2) {
                int v1 = abs(previewWidth - o1.width) + abs(previewHeight - o1.height);
                int v2 = abs(previewWidth - o2.width) + abs(previewHeight - o2.height);
                return v1 - v2;
            }
        });
    }

}