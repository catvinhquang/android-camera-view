package com.quangcv.cameraview;

import android.hardware.Camera;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.abs;

public class Camera1Wrapper {

    private CameraView cameraView;
    private CameraCallback callback;
    private boolean isTakingPicture = false;
    private Camera camera;

    public Camera1Wrapper(CameraView preview) {
        cameraView = preview;
    }

    public boolean start() {
        if (camera == null) {
            camera = Camera.open(0);
            callback.onCameraOpened();
            startPreview();
        }
        return true;
    }

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

    // TODO quangcv : preview size thì same same nhau là được
    // picture size thì phải tỉ lệ với preview size và lớn nhất có thể
    private Camera.Size getPictureSize(List<Camera.Size> supportedSizes,
                                       final int previewWidth,
                                       final int previewHeight) {
        // select element with smallest differrence
        return Collections.min(supportedSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size l, Camera.Size r) {
                int diff1 = abs(previewWidth - l.width) + abs(previewHeight - l.height);
                int diff2 = abs(previewWidth - r.width) + abs(previewHeight - r.height);
                return diff1 - diff2;
            }
        });
    }

//    private Camera.Size select(List<Camera.Size> sizes,
//                               int viewWidth,
//                               int viewHeight) {
//        if (sizes == null || sizes.isEmpty()) return null;
//
//        double viewRatio = (double) viewWidth / viewHeight;
//        for (Camera.Size i : sizes) {
//            double itemRatio = (double) i.width / i.height;
//            double diffRatio = abs(viewRatio - itemRatio);
//
//            double diffSize = abs(viewWidth - i.width) + abs(viewHeight - i.height);
//        }
//
//        // TODO quangcv WIP
//    }

    public void setCallback(CameraCallback callback) {
        this.callback = callback;
    }

    public CameraCallback getCallback() {
        return callback;
    }

}