package com.quangcv.cameraview.sample;

/**
 * Created by QuangCV on 31-Aug-2019
 **/

public interface CameraCallback {

    void onCameraOpened();

    void onCameraClosed();

    void onPictureTaken(byte[] data);

}