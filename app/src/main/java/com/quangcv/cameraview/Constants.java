package com.quangcv.cameraview;

import android.support.annotation.IntDef;

public interface Constants {

    AspectRatio DEFAULT_ASPECT_RATIO = AspectRatio.of(4, 3);

    int LANDSCAPE_90 = 90;
    int LANDSCAPE_270 = 270;

    @IntDef({Facing.FACING_BACK, Facing.FACING_FRONT})
    @interface Facing {
        int FACING_BACK = 0;
        int FACING_FRONT = 1;
    }

    @IntDef({Flash.FLASH_OFF, Flash.FLASH_ON, Flash.FLASH_TORCH, Flash.FLASH_AUTO, Flash.FLASH_RED_EYE})
    @interface Flash {
        int FLASH_OFF = 0;
        int FLASH_ON = 1;
        int FLASH_TORCH = 2;
        int FLASH_AUTO = 3;
        int FLASH_RED_EYE = 4;
    }

}