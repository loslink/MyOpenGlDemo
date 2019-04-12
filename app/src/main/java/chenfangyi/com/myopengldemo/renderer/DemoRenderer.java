package chenfangyi.com.myopengldemo.renderer;

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

import chenfangyi.com.myopengldemo.texture.ImageFilterTexture;
import chenfangyi.com.myopengldemo.utils.IConfigChangeListener;
import chenfangyi.com.myopengldemo.utils.OpenGlUtils;
import chenfangyi.com.myopengldemo.texture.ImageTexture;

/**
 * Created by chenfangyi on 17-5-11.
 */

public class DemoRenderer extends AbstractBitmapRenderer {

    public static float[] projMatrix = new float[16];// 投影
    public static float[] viewMatrix = new float[16];// 相机
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
            mImageTexture = new ImageFilterTexture();
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
        Matrix.frustumM(projMatrix, 0, -1, 1, -1, 1, 3, 7);//投影矩阵设置
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, -3, 0, 0, 0, 0.0f, 1.0f, 0.0f);
        Matrix.setIdentityM(mViewPjMatrix, 0);
        Matrix.multiplyMM(mViewPjMatrix, 0, viewMatrix,0, mViewPjMatrix, 0);
        Matrix.multiplyMM(mViewPjMatrix, 0, projMatrix,0, mViewPjMatrix, 0);
        mImageTexture.onDisplaySizeChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        runPreDrawTask();
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        mImageTexture.draw(mViewPjMatrix);
        runAfterDrawTask();
    }

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
                    if (bitmap.getWidth() % 2 == 1) {//奇数宽度的时候需要做这个处理， 原因是因为某些机型如果存在奇数宽度生成纹理后绘制出来完全不正确
                        hasAddPaddingX = true;
                    }

                    if(bitmap.getHeight() % 2 == 1){
                        hasAddPaddingY = true;
                    }

                    if(hasAddPaddingX || hasAddPaddingY){
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
