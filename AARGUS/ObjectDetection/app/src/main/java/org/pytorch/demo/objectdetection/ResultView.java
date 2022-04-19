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
    private Rect VB;

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
        drawBox(canvas,VB,"Virtual Box");

        // drawing the bounding boxes for each result along with its scores and class type
        for (Result result : mResults) {
            drawBox(canvas,result.rect,String.format("%s %.2f", PrePostProcessor.mClasses[result.classIndex], result.score));
        }
    }

    private void drawBox(Canvas canvas, Rect box,String text){
        mPaintRectangle.setStrokeWidth(5);
        mPaintRectangle.setStyle(Paint.Style.STROKE);
        canvas.drawRect(box,mPaintRectangle);

        Path mPath = new Path();
        RectF mRectF = new RectF(box.left, box.top, box.left + TEXT_WIDTH,  box.top + TEXT_HEIGHT);
        mPath.addRect(mRectF, Path.Direction.CW);
        mPaintText.setColor(Color.MAGENTA);
        canvas.drawPath(mPath, mPaintText);

        mPaintText.setColor(Color.WHITE);
        mPaintText.setStrokeWidth(0);
        mPaintText.setStyle(Paint.Style.FILL);
        mPaintText.setTextSize(32);
        canvas.drawText(text, box.left + TEXT_X, box.top + TEXT_Y, mPaintText);

    }

    public void setResults(ArrayList<Result> results) {
        mResults = results;
    }
    public void setBox(Rect box){VB = box;}
}
