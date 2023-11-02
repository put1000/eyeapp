package com.yx.eyeapp.graphic;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import com.yx.eyeapp.GraphicOverlay;

/**
 * 相机预览图画笔
 */
public class CameraGraphic extends GraphicOverlay.Graphic {
    private Bitmap bitmap;
    public CameraGraphic(Bitmap bitmap, GraphicOverlay overlay){
        super(overlay);
        this.bitmap = bitmap;
    }
    @Override
    public void draw(Canvas canvas) {
        Matrix matrix = new Matrix();
        matrix.setScale(getScale(), getScale());
        matrix.postTranslate(getStartx(), 0);
        canvas.drawBitmap(bitmap, matrix, new Paint());
    }
}
