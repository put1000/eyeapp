package com.yx.eyeapp.graphic;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.Log;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.yx.eyeapp.GraphicOverlay;

/**
 * 人脸特征点画笔
 */
public class FaceGraphic extends GraphicOverlay.Graphic {
    private static final float FACE_POSITION_RADIUS = 7.0f;
    private static final float FACE_BOUNDING_RADIUS = 5.0f;
    private static final float FACE_BOUNDING_GAP = 5.0f;
    private static final int FACE_POSITION_COLOR = Color.WHITE;
    private static final int FACE_BOX_COLOR = Color.WHITE;
    private final Paint facePositionPaint;
    private final Paint faceBoxPaint;
    private volatile Face face;
    private static final String TAG = "FaceGraphic";

    public FaceGraphic(Face face, GraphicOverlay overlay) {
        super(overlay);
        this.face = face;

        facePositionPaint = new Paint();
        facePositionPaint.setColor(FACE_POSITION_COLOR);
        faceBoxPaint = new Paint();
        faceBoxPaint.setStyle(Paint.Style.STROKE);
        faceBoxPaint.setStrokeWidth(FACE_BOUNDING_RADIUS);
        faceBoxPaint.setColor(FACE_BOX_COLOR);
    }


    @Override
    public void draw(Canvas canvas) {
        if(face == null) return;
        Log.d(TAG, "draw: facegraphic");

        //获取矩形框位置
        float top =  scale(face.getBoundingBox().top) - FACE_BOUNDING_GAP;
        float bottom = scale(face.getBoundingBox().bottom)- FACE_BOUNDING_GAP;
        float left = transcalteX(face.getBoundingBox().left + FACE_BOUNDING_GAP);
        float right = transcalteX(face.getBoundingBox().right + FACE_BOUNDING_GAP);

        //绘制矩形
        canvas.drawRect(left, top, right, bottom,faceBoxPaint);
        //绘制特诊点
        for (FaceContour contour : face.getAllContours()) {
            for (PointF point : contour.getPoints()) {
                canvas.drawCircle(
                        transcalteX(point.x), scale(point.y), FACE_POSITION_RADIUS, facePositionPaint);
                Log.d(TAG, "draw: x " + scale(point.x) + " y:" + scale(point.y));
            }
        }
    }
}
