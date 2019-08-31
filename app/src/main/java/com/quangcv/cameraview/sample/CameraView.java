package com.quangcv.cameraview.sample;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.quangcv.cameraview.R;

import java.util.Set;

public class CameraView extends FrameLayout {

    private final DisplayOrientationDetector mDisplayOrientationDetector;
    private BaseCamera mImpl;

    public CameraView(Context context) {
        this(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (isInEditMode()) {
            mDisplayOrientationDetector = null;
            return;
        }

        SurfaceViewPreview preview = createPreviewImpl(context);
        if (Build.VERSION.SDK_INT < 21) {
            mImpl = new Camera1(preview);
        } else {
            mImpl = new Camera2(preview, context);
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

    @NonNull
    private SurfaceViewPreview createPreviewImpl(Context context) {
        return new SurfaceViewPreview(context, this);
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

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (isInEditMode()) return;

        // Measure the TextureView
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        AspectRatio ratio = getAspectRatio();
        if (mDisplayOrientationDetector.getLastKnownDisplayOrientation() % 180 == 0) {
            ratio = ratio.inverse();
        }
        assert ratio != null;
        if (height < width * ratio.getY() / ratio.getX()) {
            mImpl.getView().measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(width * ratio.getY() / ratio.getX(),
                            MeasureSpec.EXACTLY));
        } else {
            mImpl.getView().measure(
                    MeasureSpec.makeMeasureSpec(height * ratio.getX() / ratio.getY(),
                            MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        }
    }

    public void start() {
        if (!mImpl.start()) {
            // Fall back to Camera1
            // Camera2 uses legacy hardware layer
            CameraCallback c = mImpl.getCallback();
            mImpl = new Camera1(createPreviewImpl(getContext()));
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
}