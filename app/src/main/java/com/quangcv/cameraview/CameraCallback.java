package com.quangcv.cameraview;

/**
 * Created by QuangCV on 31-Aug-2019
 **/

public interface CameraCallback {

    void onCameraOpened();

    void onCameraClosed();

    void onPictureTaken(byte[] data);

}