/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.quangcv.cameraview.sample;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.quangcv.cameraview.R;

class SurfaceViewPreview {

    final SurfaceView mSurfaceView;

    private Callback mCallback;

    private int mWidth;

    private int mHeight;

    SurfaceViewPreview(Context context, ViewGroup parent) {
        final View view = View.inflate(context, R.layout.surface_view, parent);
        mSurfaceView = view.findViewById(R.id.surface_view);
        final SurfaceHolder holder = mSurfaceView.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder h) {
            }

            @Override
            public void surfaceChanged(SurfaceHolder h, int format, int width, int height) {
                setSize(width, height);
                if (!ViewCompat.isInLayout(mSurfaceView)) {
                    dispatchSurfaceChanged();
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder h) {
                setSize(0, 0);
            }
        });
    }

    void setCallback(Callback callback) {
        mCallback = callback;
    }

    Surface getSurface() {
        return getSurfaceHolder().getSurface();
    }

    SurfaceHolder getSurfaceHolder() {
        return mSurfaceView.getHolder();
    }

    View getView() {
        return mSurfaceView;
    }

    Class getOutputClass() {
        return SurfaceHolder.class;
    }

    boolean isReady() {
        return getWidth() != 0 && getHeight() != 0;
    }

    protected void dispatchSurfaceChanged() {
        mCallback.onSurfaceChanged();
    }

    void setDisplayOrientation(int displayOrientation) {
    }

    void setBufferSize(int width, int height) {
    }

    void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    int getWidth() {
        return mWidth;
    }

    int getHeight() {
        return mHeight;
    }

    public interface Callback {
        void onSurfaceChanged();
    }

}
