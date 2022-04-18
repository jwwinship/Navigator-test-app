package org.pytorch.demo.objectdetection;

import android.graphics.Rect;

import java.util.ArrayList;

public class Utilities {

    /**
     * Oliver
     * setup a virtual box in the middle of the screen for detection purpose
     * The box takes up 1/4 of the total screen size and is located in the exact center of the screen
     * @param screenHeight
     * @param screenWidth
     */
    public Rect setupVirtualBox(int screenHeight, int screenWidth){
        int centerX = screenWidth/2;
        int centerY = screenHeight/2;

        int topLeftX = centerX - centerX/2;
        int topLeftY = centerY - centerY/2;
        int bottomRightX = centerX + centerX/2;
        int bottomRightY = centerY + centerY/2;

        return new Rect(topLeftX,topLeftY,bottomRightX,bottomRightY);
    }

}
