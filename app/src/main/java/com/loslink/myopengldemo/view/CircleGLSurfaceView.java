package com.loslink.myopengldemo.view;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import com.loslink.myopengldemo.renderer.NewRenderer;

/**
 * Created by chenfangyi on 17-5-11.
 */

public class CircleGLSurfaceView extends GLSurfaceView {

    NewRenderer mRenderer;

    public CircleGLSurfaceView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
    }

    public CircleGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
    }

    public void init(){
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void setRenderer(Renderer renderer) {
        super.setRenderer(renderer);
        mRenderer = (NewRenderer) renderer;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int mode = event.getAction();
        float curX = event.getX();
        float curY = event.getY();
        switch(mode){
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                mRenderer.setLightPosition(curX, curY);
                requestRender();
                break;
            default:
                break;
        }
        return true;
    }
}
