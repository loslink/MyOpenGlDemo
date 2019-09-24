package com.loslink.myopengldemo.renderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.widget.ImageView;

import java.util.LinkedList;
import java.util.Queue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.loslink.myopengldemo.texture.ImageFilterTexture;
import com.loslink.myopengldemo.utils.IConfigChangeListener;
import com.loslink.myopengldemo.utils.OpenGlUtils;
import com.loslink.myopengldemo.texture.ImageTexture;

/**
 * 负责渲染任务工作，监听GLSurfaceView 尺寸变化，任务到来，操作纹理 mImageTexture
 */

public class DemoRenderer extends AbstractBitmapRenderer {

    public static float[] projMatrix = new float[16];// 投影
    public static float[] cameraMatrix = new float[16];// 相机
    public static float[] mViewPjMatrix = new float[16];// 总变换矩阵
    public static float[] matrixs = new float[16];
    Context mContext;
    private ImageTexture mImageTexture;

    public final static int ADD_SIZE = 1;

    private IConfigChangeListener mConfigChangeListener;

    private Queue<Runnable> mPreDrawTask;//绘制之前的任务
    private Queue<Runnable> mAfterDrawTask;//绘制之后的任务

    public DemoRenderer(Context context){
        this(context, false);
    }

    public DemoRenderer(Context context, boolean useFilter){
        mContext = context;
        mPreDrawTask = new LinkedList<>();
        mAfterDrawTask = new LinkedList<>();
        if(useFilter) {
            mImageTexture = new ImageFilterTexture(context);
        } else{
            mImageTexture = new ImageTexture();
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Set the background color to black ( rgba ).
        GLES20.glClearColor(0.5f, 0.0f, 0.0f, 1f);  // OpenGL docs.

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        mImageTexture.onInit();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        //坐标轴和数学的一样，y为上，x为右，z为屏幕正向外
        //Matrix.frustumM 截锥体
        //left、right、bottom、top是近截面的离截面中心点的坐标，near：近截面的距离，far远截面距离一般比较大要容纳整个物体（即在物体后面之后）
        Matrix.frustumM(projMatrix, 0, -1, 1, -1, 1, 3, 7);//投影矩阵设置

//https://blog.csdn.net/jamesshaoya/article/details/54342241
//        setLookAtM 参数（设置摄像机位置）
//        float cx, //摄像机位置x
//        float cy, //摄像机位置y
//        float cz, //摄像机位置z
//        float tx, //摄像机目标点x
//        float ty, //摄像机目标点y
//        float tz, //摄像机目标点z
//        float upx, //摄像机UP向量X分量 三个参数决定相机的顶部方向，决定相机摆放姿势
//        float upy, //摄像机UP向量Y分量
//        float upz //摄像机UP向量Z分量
        Matrix.setLookAtM(cameraMatrix, 0, 0, 0, -3, 0, 0, 0, 0.0f, 1.0f, 0.0f);
        Matrix.setIdentityM(mViewPjMatrix, 0);
        Matrix.multiplyMM(mViewPjMatrix, 0, cameraMatrix,0, mViewPjMatrix, 0);
        Matrix.multiplyMM(mViewPjMatrix, 0, projMatrix,0, mViewPjMatrix, 0);
        mImageTexture.onDisplaySizeChanged(width, height);
    }

    /**
     * gl回调绘制，mGLSurfaceView.requestRender()就会执行
     * @param gl
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        runPreDrawTask();//绘制前任务
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        mImageTexture.draw(mViewPjMatrix);//执行绘制任务
        runAfterDrawTask();//绘制后任务
    }

    /**
     * 设置图片资源任务
     * @param bitmap
     * @param isRecycle
     */
    public void setImageBitmap(final Bitmap bitmap, final boolean isRecycle){
        if (bitmap == null || bitmap.isRecycled()) {
            return;
        }
        addPreDrawTask(new Runnable() {
            @Override
            public void run() {
                if (bitmap == null || bitmap.isRecycled()) {
                    return;
                }
                try {
                    boolean hasAddPaddingX = false;
                    boolean hasAddPaddingY = false;
                    Bitmap resizedBitmap = null;
                    if (bitmap.getWidth() % 2 == 1) {//奇数宽度 的时候需要做这个处理， 原因是因为某些机型如果存在奇数宽度生成纹理后绘制出来完全不正确
                        hasAddPaddingX = true;
                    }

                    if(bitmap.getHeight() % 2 == 1){
                        hasAddPaddingY = true;
                    }

                    if(hasAddPaddingX || hasAddPaddingY){//需要把奇数长度转成偶数
                        resizedBitmap = Bitmap.createBitmap(bitmap.getWidth() + (hasAddPaddingX ? ADD_SIZE : 0), bitmap.getHeight() + (hasAddPaddingY ? ADD_SIZE : 0), Bitmap.Config.ARGB_8888);
                        Canvas can = new Canvas(resizedBitmap);
                        can.drawColor(Color.GREEN);
                        can.drawBitmap(bitmap, 0, 0, null);
                    }

                    boolean resized = (resizedBitmap != null);
                    int textureId = OpenGlUtils.loadTexture(resized ? resizedBitmap : bitmap, mImageTexture.getImageTextureId(), isRecycle);
                    if (resizedBitmap != null) {
                        resizedBitmap.recycle();
                    }
                    int imageWidth = resized ? resizedBitmap.getWidth() : bitmap.getWidth();
                    int imageHeight = resized ? resizedBitmap.getHeight() : bitmap.getHeight();
                    mImageTexture.setTexture(textureId, imageWidth, imageHeight, hasAddPaddingX, hasAddPaddingY);
                } catch (Exception e) {
                }
            }
        });
    }

    public void setImageBitmap(Bitmap bitmap){
        setImageBitmap(bitmap, false);
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

    public void setScaleType(final ImageView.ScaleType scaleType){
        addPreDrawTask(new Runnable() {
            @Override
            public void run() {
                mImageTexture.setScaleType(scaleType);
            }
        });
    }

    public void setRotation(final int rotation){
        if(mConfigChangeListener != null){
            mConfigChangeListener.onRotateChanged(rotation);
        }
        addPreDrawTask(new Runnable() {
            @Override
            public void run() {
                mImageTexture.setRotation(rotation);
            }
        });
    }

    public void setFlip(final boolean flipH, final boolean flipV){
        if(mConfigChangeListener != null){
            mConfigChangeListener.onFlipChanged(flipH, flipV);
        }
        addPreDrawTask(new Runnable() {
            @Override
            public void run() {
                mImageTexture.setFlip(flipH, flipV);
            }
        });
    }

    public void setOnConfigChangeListener(IConfigChangeListener configChangeListener){
        mConfigChangeListener = configChangeListener;
    }

    @Override
    public Bitmap getBitmap() {
        runPreDrawTask();
        Bitmap result = mImageTexture.getBitmap(mViewPjMatrix);
        runAfterDrawTask();
        return result;
    }

    /**
     * 删除纹理
     */
    public void deleteTexture(){
        addAfterDrawTask(new Runnable() {
            @Override
            public void run() {
                mImageTexture.deleteTexture();
            }
        });
    }

    @Override
    public void destory() {

    }
}
