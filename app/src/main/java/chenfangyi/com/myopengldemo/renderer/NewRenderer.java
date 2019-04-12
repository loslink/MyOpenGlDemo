package chenfangyi.com.myopengldemo.renderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.util.LinkedList;
import java.util.Queue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import chenfangyi.com.myopengldemo.texture.CircleTexture;
import chenfangyi.com.myopengldemo.utils.IConfigChangeListener;
import chenfangyi.com.myopengldemo.utils.OpenGlUtils;

/**
 * Created by chenfangyi on 17-5-11.
 */

public class NewRenderer extends AbstractBitmapRenderer {

    public static float[] projMatrix = new float[16];// 投影
    public static float[] viewMatrix = new float[16];// 相机
    public static float[] mViewPjMatrix = new float[16];// 总变换矩阵
    public static float[] matrixs = new float[16];
    Context mContext;
    private CircleTexture mImageTexture;

    public final static int ADD_SIZE = 50;

    private IConfigChangeListener mConfigChangeListener;

    private Queue<Runnable> mPreDrawTask;//绘制之前的任务
    private Queue<Runnable> mAfterDrawTask;//绘制之后的任务

    public NewRenderer(Context context){
        mContext = context;
        mPreDrawTask = new LinkedList<>();
        mAfterDrawTask = new LinkedList<>();
        mImageTexture = new CircleTexture();
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
        if(width > height){
            float ratio = (float) width / height;
            Matrix.frustumM(projMatrix, 0, -ratio, ratio, -1, 1, 1.5f, 10);//投影矩阵设置
        } else{
            float ratio = (float) height / width;
            Matrix.frustumM(projMatrix, 0, -1, 1, -ratio, ratio, 1.5f, 10);//投影矩阵设置
        }
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, 3, 0, 0, 0, 0.0f, 1.0f, 0.0f);
        Matrix.setIdentityM(mViewPjMatrix, 0);
//        Matrix.rotateM(mViewPjMatrix, 0, (float)Math.toRadians(190), 0, 1, 0);
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
                    int textureId = OpenGlUtils.loadTexture(resized ? resizedBitmap : bitmap, OpenGlUtils.NO_TEXTURE, isRecycle);
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
        addPreDrawTask(runnable, false);
    }

    private void addPreDrawTask(Runnable runnable, boolean removeSameRunnable){
        synchronized (mPreDrawTask){
            if(removeSameRunnable){
                removeSameRunnable(runnable);
            }
            mPreDrawTask.add(runnable);
        }
    }

    private void removeSameRunnable(Runnable runnable){
        synchronized (mPreDrawTask){
            while(!mPreDrawTask.isEmpty()){
                Runnable runnable1 = mPreDrawTask.peek();
                if(runnable1.getClass().equals(runnable.getClass())){
                    mPreDrawTask.poll();
                }
            }
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

    public void setLightPosition(float curX, float curY){
        addPreDrawTask(mImageTexture.getSetLightPositionRunnable(curX, curY), true);
    }

    @Override
    public void destory() {

    }

    @Override
    public Bitmap getBitmap() {
        return null;
    }
}
