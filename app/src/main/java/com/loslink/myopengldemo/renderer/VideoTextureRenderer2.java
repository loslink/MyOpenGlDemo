package com.loslink.myopengldemo.renderer;


import android.content.Context;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;
import android.widget.ImageView;

import com.loslink.myopengldemo.R;
import com.loslink.myopengldemo.texture.VideoTexture;
import com.loslink.myopengldemo.utils.IConfigChangeListener;
import com.loslink.myopengldemo.utils.Utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.LinkedList;
import java.util.Queue;

public class VideoTextureRenderer2 extends TextureSurfaceRenderer {

    private Context ctx;
    private OnStateListner onStateListner;
    private IConfigChangeListener mConfigChangeListener;

    private Queue<Runnable> mPreDrawTask;//绘制之前的任务
    private Queue<Runnable> mAfterDrawTask;//绘制之后的任务

    private VideoTexture videoTexture;

    public VideoTextureRenderer2(Context context, SurfaceTexture texture, int width, int height) {
        super(texture, width, height);
        this.ctx = context;
        mPreDrawTask = new LinkedList<>();
        mAfterDrawTask = new LinkedList<>();
        String vertexShaderCode = Utils.readTextFileFromResource(context, R.raw.simple_vertex_shader);
        String fragmentShaderCode =Utils.readTextFileFromResource(context,R.raw.simple_fragment_shader);
        videoTexture =new VideoTexture(vertexShaderCode,fragmentShaderCode,width,height);
    }

    public OnStateListner getOnStateListner() {
        return onStateListner;
    }

    public void setOnStateListner(OnStateListner onStateListner) {
        this.onStateListner = onStateListner;
        videoTexture.setOnStateListner(onStateListner);
    }


    /**
     * 子线程定时回调
     * @return
     */
    @Override
    protected boolean draw() {
        return videoTexture.draw();
    }

    public void setCropRect(RectF cropRect) {
        videoTexture.setCropRect(cropRect);
    }


    @Override
    protected void initGLComponents() {
        videoTexture.initGLComponents();
    }

    @Override
    protected void deinitGLComponents() {
        videoTexture.deinitGLComponents();
    }

    public void setVideoSize(int width, int height) {
        videoTexture.setVideoSize(width,height);
    }

    public SurfaceTexture getVideoSurfaceTexture() {
        return videoTexture.getVideoSurfaceTexture();
    }

    private void addPreDrawTask(Runnable runnable){
        synchronized (mPreDrawTask){
            mPreDrawTask.add(runnable);
        }
    }

    private void addAfterDrawTask(Runnable runnable){
        synchronized (mAfterDrawTask){
            mAfterDrawTask.add(runnable);
        }
    }

    private void runPreDrawTask(){
        synchronized (mPreDrawTask){
            while(!mPreDrawTask.isEmpty()){
                mPreDrawTask.poll().run();
            }
        }
    }

    private void runAfterDrawTask(){
        synchronized (mAfterDrawTask){
            while(!mAfterDrawTask.isEmpty()){
                mAfterDrawTask.poll().run();
            }
        }
    }

//    public void setScaleType(final ImageView.ScaleType scaleType){
//        addPreDrawTask(new Runnable() {
//            @Override
//            public void run() {
//                mImageTexture.setScaleType(scaleType);
//            }
//        });
//    }
//
//    public void setRotation(final int rotation){
//        if(mConfigChangeListener != null){
//            mConfigChangeListener.onRotateChanged(rotation);
//        }
//        addPreDrawTask(new Runnable() {
//            @Override
//            public void run() {
//                mImageTexture.setRotation(rotation);
//            }
//        });
//    }
//
//    public void setFlip(final boolean flipH, final boolean flipV){
//        if(mConfigChangeListener != null){
//            mConfigChangeListener.onFlipChanged(flipH, flipV);
//        }
//        addPreDrawTask(new Runnable() {
//            @Override
//            public void run() {
//                mImageTexture.setFlip(flipH, flipV);
//            }
//        });
//    }

    public void setOnConfigChangeListener(IConfigChangeListener configChangeListener){
        mConfigChangeListener = configChangeListener;
    }

    public interface OnStateListner {
        void onTextureInitSuccess();
    }
}
