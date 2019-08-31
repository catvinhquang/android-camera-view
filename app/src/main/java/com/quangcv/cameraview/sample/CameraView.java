package com.quangcv.cameraview.sample;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.quangcv.cameraview.R;

import java.util.Set;

public class CameraView extends FrameLayout {

    private final DisplayOrientationDetector mDisplayOrientationDetector;
    private CameraViewImpl mImpl;

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

        PreviewImpl preview = createPreviewImpl(context);
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
    private PreviewImpl createPreviewImpl(Context context) {
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
        if (!isInEditMode()) {
            mDisplayOrientationDetector.disable();
        }
        super.onDetachedFromWindow();
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

    public void setCallback(@NonNull CameraCallback callback) {
        mImpl.setCallback(callback);
    }

    public void setFacing(@Constants.Facing int facing) {
        mImpl.setFacing(facing);
    }

    @Constants.Facing
    public int getFacing() {
        return mImpl.getFacing();
    }

    /**
     * Gets all the aspect ratios supported by the current camera.
     */
    public Set<AspectRatio> getSupportedAspectRatios() {
        return mImpl.getSupportedAspectRatios();
    }

    /**
     * Sets the aspect ratio of camera.
     *
     * @param ratio The {@link AspectRatio} to be set.
     */
    public void setAspectRatio(@NonNull AspectRatio ratio) {
        if (mImpl.setAspectRatio(ratio)) {
            requestLayout();
        }
    }

    /**
     * Gets the current aspect ratio of camera.
     *
     * @return The current {@link AspectRatio}. Can be {@code null} if no camera is opened yet.
     */
    @Nullable
    public AspectRatio getAspectRatio() {
        return mImpl.getAspectRatio();
    }

    /**
     * Enables or disables the continuous auto-focus mode. When the current camera doesn't support
     * auto-focus, calling this method will be ignored.
     *
     * @param autoFocus {@code true} to enable continuous auto-focus mode. {@code false} to
     *                  disable it.
     */
    public void setAutoFocus(boolean autoFocus) {
        mImpl.setAutoFocus(autoFocus);
    }

    /**
     * Returns whether the continuous auto-focus mode is enabled.
     *
     * @return {@code true} if the continuous auto-focus mode is enabled. {@code false} if it is
     * disabled, or if it is not supported by the current camera.
     */
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

    public void takePicture() {
        mImpl.takePicture();
    }

}