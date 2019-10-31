package com.quangcv.cameraview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.max;

public class CameraView extends FrameLayout {

    private int facing = Camera.CameraInfo.CAMERA_FACING_BACK;
    private double ratioPV, ratioPP;
    private boolean started = false;
    private boolean surfaceReady = false;
    private boolean takingPicture = false;

    private Camera camera;
    private Camera.CameraInfo cameraInfo;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;

    public CameraView(Context context) {
        this(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (isInEditMode()) return;

        surfaceView = new SurfaceView(context);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.CENTER;
        addView(surfaceView, lp);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                surfaceReady = true;
                if (started) {
                    start();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                surfaceReady = false;
                stop();
            }
        });
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (isInEditMode()) {
            String tag = "<CameraView />";
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.BLACK);
            paint.setTextSize(0.1f * getWidth());
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            float x = getWidth() / 2f - paint.measureText(tag) / 2f;
            float y = getHeight() / 2f + paint.descent();
            canvas.drawText(tag, x, y, paint);
        }
    }

    public void start() {
        started = true;
        if (camera == null) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            int count = Camera.getNumberOfCameras();
            for (int i = 0; i < count; i++) {
                Camera.getCameraInfo(i, info);
                if (info.facing == facing) {
                    cameraInfo = info;
                    camera = Camera.open(i);
                    break;
                }
            }
        }

        // start preview when surface is ready
        if (surfaceReady) {
            try {
                Camera.Parameters params = updateParameters(camera.getParameters(),
                        getWidth(), getHeight());
                updateViewSize(params.getPreviewSize());
                camera.setParameters(params);
                // TODO only support portrait: orientation of preview
                camera.setDisplayOrientation(facing == Camera.CameraInfo.CAMERA_FACING_BACK ?
                        (cameraInfo.orientation + 360) % 360 :
                        (360 - cameraInfo.orientation % 360) % 360);
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        started = false;
        takingPicture = false;
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    public void takePicture(final OnPictureTakenListener listener) {
        if (camera != null && !takingPicture) {
            takingPicture = true;
            camera.takePicture(null, null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(final byte[] data, final Camera camera) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            takingPicture = false;

                            // preprocess picture
                            int left = (int) (abs(getLeft() - surfaceView.getLeft()) / ratioPV * ratioPP);
                            int top = (int) (abs(getTop() - surfaceView.getTop()) / ratioPV * ratioPP);
                            int width = (int) (getWidth() / ratioPV * ratioPP);
                            int height = (int) (getHeight() / ratioPV * ratioPP);

                            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                            Matrix matrix = new Matrix();
                            matrix.postRotate(cameraInfo.orientation % 360);
                            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                            bitmap = Bitmap.createBitmap(bitmap, left, top, width, height);

                            final Bitmap finalBitmap = bitmap;
                            post(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onPictureTaken(finalBitmap);
                                }
                            });
                        }
                    }).start();
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

    private Camera.Parameters updateParameters(Camera.Parameters params,
                                               int viewWidth,
                                               int viewHeight) {
        // preview size
        Camera.Size preSize = getPreviewSize(params.getSupportedPreviewSizes(),
                viewWidth,
                viewHeight);
        params.setPreviewSize(preSize.width, preSize.height);

        // picture size
        Camera.Size picSize = getPictureSize(params.getSupportedPictureSizes(),
                preSize.width,
                preSize.height);
        params.setPictureSize(picSize.width, picSize.height);

        ratioPP = (double) picSize.width / preSize.width;

        // enable auto focus
        if (params.getSupportedFocusModes()
                .contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        // TODO only support portrait: orientation of picture file
        params.setRotation(cameraInfo.orientation % 360);

        return params;
    }

    private void updateViewSize(Camera.Size previewSize) {
        int viewW = getWidth();
        int viewH = getHeight();

        int expectedW, expectedH;
        if (cameraInfo.orientation == 90 || cameraInfo.orientation == 270) {
            expectedW = previewSize.height;
            expectedH = previewSize.width;
        } else {
            expectedW = previewSize.width;
            expectedH = previewSize.height;
        }

        if (expectedW < viewW || expectedH < viewH) {
            ratioPV = max((double) viewW / expectedW, (double) viewH / expectedH);
            expectedW = (int) (expectedW * ratioPV);
            expectedH = (int) (expectedH * ratioPV);
        } else {
            ratioPV = 1;
        }

        LayoutParams lp = (LayoutParams) surfaceView.getLayoutParams();
        lp.width = expectedW;
        lp.height = expectedH;
        surfaceView.setLayoutParams(lp);
    }

    public interface OnPictureTakenListener {
        void onPictureTaken(Bitmap result);
    }

}