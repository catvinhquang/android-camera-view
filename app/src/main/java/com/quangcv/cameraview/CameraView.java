package com.quangcv.cameraview;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.quangcv.cameraview.core.BaseCamera;
import com.quangcv.cameraview.core.Camera1;
import com.quangcv.cameraview.core.Camera2;

import java.util.Set;

public class CameraView extends SurfaceView implements SurfaceHolder.Callback {

    private DisplayOrientationDetector mDisplayOrientationDetector;
    private BaseCamera mImpl;

    private SurfaceCallback surfaceCallback;
    private int surfaceWidth;
    private int surfaceHeight;

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

        if (Build.VERSION.SDK_INT < 21) {
            mImpl = new Camera1(this);
        } else {
            mImpl = new Camera2(this, context);
        }

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.CameraView,
                defStyleAttr,
                R.style.Widget_CameraView);
        String aspectRatio = a.getString(R.styleable.CameraView_aspectRatio);
        setAspectRatio(aspectRatio != null ? AspectRatio.parse(aspectRatio) : Constants.DEFAULT_ASPECT_RATIO);
        setFacing(a.getInt(R.styleable.CameraView_facing, Constants.Facing.FACING_BACK));
        setAutoFocus(a.getBoolean(R.styleable.CameraView_autoFocus, true));
        setFlash(a.getInt(R.styleable.CameraView_flash, Constants.Flash.FLASH_AUTO));
        a.recycle();

        mDisplayOrientationDetector = new DisplayOrientationDetector(context) {
            @Override
            public void onDisplayOrientationChanged(int displayOrientation) {
                mImpl.setDisplayOrientation(displayOrientation);
            }
        };
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        surfaceWidth = width;
        surfaceHeight = height;
        if (!ViewCompat.isInLayout(CameraView.this)) {
            surfaceCallback.onSurfaceChanged();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceWidth = surfaceHeight = 0;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            mDisplayOrientationDetector.enable(ViewCompat.getDisplay(this));
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (!isInEditMode()) {
            mDisplayOrientationDetector.disable();
        }
    }

    public void start() {
        if (!mImpl.start()) {
            // Fall back to Camera1
            // Camera2 uses legacy hardware layer
            CameraCallback c = mImpl.getCallback();
            mImpl = new Camera1(this);
            mImpl.setCallback(c);
            mImpl.start();
        }
    }

    public void stop() {
        mImpl.stop();
    }

    public void takePicture() {
        mImpl.takePicture();
    }

    public void setFacing(@Constants.Facing int facing) {
        mImpl.setFacing(facing);
    }

    @Constants.Facing
    public int getFacing() {
        return mImpl.getFacing();
    }

    public Set<AspectRatio> getSupportedAspectRatios() {
        return mImpl.getSupportedAspectRatios();
    }

    public void setAspectRatio(@NonNull AspectRatio ratio) {
        if (mImpl.setAspectRatio(ratio)) {
            requestLayout();
        }
    }

    @Nullable
    public AspectRatio getAspectRatio() {
        return mImpl.getAspectRatio();
    }

    public void setAutoFocus(boolean autoFocus) {
        mImpl.setAutoFocus(autoFocus);
    }

    public boolean getAutoFocus() {
        return mImpl.getAutoFocus();
    }

    public void setFlash(@Constants.Flash int flash) {
        mImpl.setFlash(flash);
    }

    @Constants.Flash
    public int getFlash() {
        return mImpl.getFlash();
    }

    public void setCallback(@NonNull CameraCallback callback) {
        mImpl.setCallback(callback);
    }


    // ==== surfaceview preview
    public void setSurfaceCallback(SurfaceCallback callback) {
        surfaceCallback = callback;
    }

    public int getSurfaceWidth() {
        return surfaceWidth;
    }

    public int getSurfaceHeight() {
        return surfaceHeight;
    }

    public boolean isReady() {
        return surfaceWidth != 0 && surfaceHeight != 0;
    }

    public void setDisplayOrientation(int displayOrientation) {
    }

    public void setBufferSize(int width, int height) {
    }

}