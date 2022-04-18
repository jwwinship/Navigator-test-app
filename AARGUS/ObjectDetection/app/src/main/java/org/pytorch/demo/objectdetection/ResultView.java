// Copyright (c) 2020 Facebook, Inc. and its affiliates.
// All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package org.pytorch.demo.objectdetection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;


import java.util.ArrayList;


public class ResultView extends View {

    private final static int TEXT_X = 40;
    private final static int TEXT_Y = 35;
    private final static int TEXT_WIDTH = 260;
    private final static int TEXT_HEIGHT = 50;

    private Paint mPaintRectangle;
    private Paint mPaintText;
    private ArrayList<Result> mResults;

    public ResultView(Context context) {
        super(context);
    }

    public ResultView(Context context, AttributeSet attrs){
        super(context, attrs);
        mPaintRectangle = new Paint();
        mPaintRectangle.setColor(Color.YELLOW);
        mPaintText = new Paint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mResults == null) return;

        // drawing the virtual box
        mPaintRectangle.setStrokeWidth(5);
        mPaintRectangle.setStyle(Paint.Style.STROKE);
        Utilities helper = new Utilities();
        Rect VB = helper.setupVirtualBox(1977,1080);
        canvas.drawRect(VB,mPaintRectangle);

        Path mPath_VB = new Path();
        RectF mRectF_VB = new RectF(VB.left, VB.top, VB.left + TEXT_WIDTH,  VB.top + TEXT_HEIGHT);
        mPath_VB.addRect(mRectF_VB, Path.Direction.CW);
        mPaintText.setColor(Color.MAGENTA);
        canvas.drawPath(mPath_VB, mPaintText);

        mPaintText.setColor(Color.WHITE);
        mPaintText.setStrokeWidth(0);
        mPaintText.setStyle(Paint.Style.FILL);
        mPaintText.setTextSize(32);
        canvas.drawText("Virtual Box", VB.left + TEXT_X, VB.top + TEXT_Y, mPaintText);
        System.out.println("__________________________________________Test");

        for (Result result : mResults) {
            mPaintRectangle.setStrokeWidth(5);
            mPaintRectangle.setStyle(Paint.Style.STROKE);
            canvas.drawRect(result.rect, mPaintRectangle);

            Path mPath = new Path();
            RectF mRectF = new RectF(result.rect.left, result.rect.top, result.rect.left + TEXT_WIDTH,  result.rect.top + TEXT_HEIGHT);
            mPath.addRect(mRectF, Path.Direction.CW);
            mPaintText.setColor(Color.MAGENTA);
            canvas.drawPath(mPath, mPaintText);

            mPaintText.setColor(Color.WHITE);
            mPaintText.setStrokeWidth(0);
            mPaintText.setStyle(Paint.Style.FILL);
            mPaintText.setTextSize(32);
            canvas.drawText(String.format("%s %.2f", PrePostProcessor.mClasses[result.classIndex], result.score), result.rect.left + TEXT_X, result.rect.top + TEXT_Y, mPaintText);
        }
    }
    public void setResults(ArrayList<Result> results) {
        mResults = results;
    }
}
