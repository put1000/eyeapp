package com.yx.eyeapp;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 一个呈现最终检测结果的试图，通过添加他的子类Graphic绘制检测结果
 */
public class GraphicOverlay extends View {
    private final Object lock = new Object();
    private final List<Graphic> graphics = new ArrayList<>();
    private float scaleFactor = 1.0f;
    private int startx;
    private int scaledWidth;
    private final String TAG = "GraphicOverlay";

    /**
     * 抽象内部类，用于定义需要绘制的图像，由子类实现。
     */
    public abstract static class Graphic{
        private GraphicOverlay overlay;
        public Graphic(GraphicOverlay overlay){ this.overlay = overlay; }

        public float scale(float imagePixel) {return imagePixel * overlay.scaleFactor; }
        public abstract void draw(Canvas canvas);

        //
        public float transcalteX(float imagePixel){
           return overlay.scaledWidth -scale(imagePixel) + getStartx();
        }
        public float getScale(){ return overlay.getScaleFactor();}
        public int getStartx() { return overlay.getStartx(); }
    }

    public GraphicOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * 重绘视图
     */
    public void redraw(){
        postInvalidate();
        Log.d(TAG, "insssss: " + graphics.size());
    }

    public void clear(){
        synchronized (lock){
            graphics.clear();
        }
        postInvalidate();
    }

    public void add(Graphic graphic){
        synchronized(lock){
            graphics.add(graphic);
        }
    }

    public void remove(Graphic graphic){
        synchronized (lock){
            graphics.remove(graphic);
        }
    }

    /**
     *从imagereader到屏幕的坐标系转化，以下几个方法并不规范，不同手机显示效果可能不一样
     */
    public void setScaleFactor(Size screenSize){
        scaleFactor = (float)screenSize.getHeight()/600;

        int screenWidth = screenSize.getWidth();
        scaledWidth = (int)(600*scaleFactor);

        startx = (screenWidth - scaledWidth )/2;
    }

    public int getStartx(){return startx;}
    public float getScaleFactor(){
        return scaleFactor;
    }

    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        Log.d(TAG, "onDraw: graphics:" + graphics.size());
        synchronized(lock){
            for(Graphic graphic : graphics){
                graphic.draw(canvas);
            }
        }
    }
}
