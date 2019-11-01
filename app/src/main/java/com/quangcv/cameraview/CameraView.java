package com.quangcv.cameraview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.max;

public class CameraView extends FrameLayout implements Camera.PictureCallback {

    private static final String TAG = CameraView.class.getSimpleName();
    private int facing = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private double ratioPV, ratioPP;
    private boolean started = false;
    private boolean surfaceReady = false;
    private boolean takingPicture = false;
    private long startTime;

    private Camera camera;
    private Camera.CameraInfo cameraInfo;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private OnPictureTakenListener listener;

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
            Camera.Parameters params = updateParameters(camera.getParameters(), getWidth(), getHeight());
            updateViewSize(params.getPreviewSize());
            camera.setParameters(params);
            camera.setDisplayOrientation(getDisplayOrientation());
            try {
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
            this.listener = listener;
            startTime = System.currentTimeMillis();
            camera.takePicture(null, null, null, this);
        }
    }

    @Override
    public void onPictureTaken(final byte[] data, Camera camera) {
        Log.d(TAG, "onPictureTaken: complete in " + (System.currentTimeMillis() - startTime) + "ms");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Bitmap result = preprocessPicture(data);
                savePicture(result);

                // callback on UI thread
                post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onPictureTaken(result);
                        listener = null;
                    }
                });

                takingPicture = false;
            }
        }).start();
    }

    private Bitmap preprocessPicture(byte[] data) {
        long time = System.currentTimeMillis();
        int left = (int) (abs(getLeft() - surfaceView.getLeft()) / ratioPV * ratioPP);
        int top = (int) (abs(getTop() - surfaceView.getTop()) / ratioPV * ratioPP);
        int width = (int) (getWidth() / ratioPV * ratioPP);
        int height = (int) (getHeight() / ratioPV * ratioPP);

        Bitmap result = BitmapFactory.decodeByteArray(data, 0, data.length);
        Matrix matrix = new Matrix();
        // make sure the orientation is correct
        matrix.preRotate(-getDisplayOrientation());
        if (facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            // flip horizontal to look more natural
            matrix.postScale(-1, 1, result.getWidth() / 2, result.getHeight() / 2);
        }
        result = Bitmap.createBitmap(result, 0, 0, result.getWidth(), result.getHeight(), matrix, true);
        result = Bitmap.createBitmap(result, left, top, width, height);
        Log.d(TAG, "onPictureTaken: preprocess in " + (System.currentTimeMillis() - time) + "ms");

        return result;
    }

    @SuppressLint("WrongThread")
    private void savePicture(Bitmap result) {
        File file = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "picture.jpg");
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            result.compress(Bitmap.CompressFormat.JPEG, 100, os);
            os.close();
        } catch (Exception e) {
            Log.w(TAG, "Cannot write to " + file, e);
        }

        if (os != null) {
            try {
                os.close();
            } catch (Exception ignore) {
            }
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
                int sum1 = l.width + l.height;
                int sum2 = r.width + r.height;
                return sum1 - sum2;
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

        Log.d(TAG, "updateParameters: view size = " + viewWidth + "x" + viewHeight);
        Log.d(TAG, "updateParameters: preview size = " + preSize.width + "x" + preSize.height);
        Log.d(TAG, "updateParameters: picture size = " + picSize.width + "x" + picSize.height);

        ratioPP = (double) picSize.width / preSize.width;

        // enable auto focus
        if (params.getSupportedFocusModes()
                .contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        return params;
    }

    private void updateViewSize(Camera.Size previewSize) {
        RectF r = new RectF(0, 0, previewSize.width, previewSize.height);
        Matrix m = new Matrix();
        m.setRotate(getDisplayOrientation(), r.width() / 2, r.height() / 2);
        m.mapRect(r);
        int expectedW = (int) r.width();
        int expectedH = (int) r.height();
        int viewW = getWidth();
        int viewH = getHeight();

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

    private int getDisplayOrientation() {
        int rotation = 0;
        try {
            WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            if (wm != null) {
                int[] array = new int[]{0, 90, 180, 270};
                rotation = array[wm.getDefaultDisplay().getRotation()];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return facing == Camera.CameraInfo.CAMERA_FACING_FRONT ?
                (360 - (cameraInfo.orientation + rotation) % 360) % 360 :
                (cameraInfo.orientation - rotation + 360) % 360;
    }

    public interface OnPictureTakenListener {
        void onPictureTaken(Bitmap result);
    }

}