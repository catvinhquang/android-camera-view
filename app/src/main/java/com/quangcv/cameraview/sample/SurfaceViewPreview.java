package com.quangcv.cameraview.sample;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

class SurfaceViewPreview extends SurfaceView {

    private Callback mCallback;
    private int mWidth;
    private int mHeight;

    public SurfaceViewPreview(Context context) {
        this(context, null);
    }

    public SurfaceViewPreview(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SurfaceViewPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        SurfaceHolder holder = getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder h) {
            }

            @Override
            public void surfaceChanged(SurfaceHolder h, int format, int width, int height) {
                setSize(width, height);
                if (!ViewCompat.isInLayout(SurfaceViewPreview.this)) {
                    dispatchSurfaceChanged();
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder h) {
                setSize(0, 0);
            }
        });
    }

    void setSurfaceCallback(Callback callback) {
        mCallback = callback;
    }

    void dispatchSurfaceChanged() {
        mCallback.onSurfaceChanged();
    }

    void setSize(int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
    }

    int getViewWidth() {
        return mWidth;
    }

    int getViewHeight() {
        return mHeight;
    }

    boolean isReady() {
        return mWidth != 0 && mHeight != 0;
    }

    public interface Callback {
        void onSurfaceChanged();
    }

    void setDisplayOrientation(int displayOrientation) {
    }

    void setBufferSize(int width, int height) {
    }

}
