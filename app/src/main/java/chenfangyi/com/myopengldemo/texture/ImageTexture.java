package chenfangyi.com.myopengldemo.texture;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.widget.ImageView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import chenfangyi.com.myopengldemo.utils.OpenGlUtils;
import chenfangyi.com.myopengldemo.utils.Utils;
import chenfangyi.com.myopengldemo.renderer.DemoRenderer;

/**
 * Created by chenfangyi on 17-5-12.
 * 绘制图片的类
 * 总结：
 * 顶点坐标控制绘制的范围
 * 纹理坐标控制纹理的哪一部分绘制
 * 两个坐标的整合就是：控制纹理的某一部分绘制到的某个位置
 */

public class ImageTexture implements ITexture {
    protected static final String VERTEX_SHADER = "" +
            "uniform mat4 uMVPMatrix;\n" +
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            " \n" +
            "varying vec2 textureCoordinate;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position * uMVPMatrix;\n" +//顶点坐标,像素点通过矩阵转换后在屏幕上的位置
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +//纹理坐标，每个像素点
            "}";
    public static final String NO_FILTER_FRAGMENT_SHADER = "" +
            "varying highp vec2 textureCoordinate;\n" +
            " \n" +
            "uniform sampler2D inputImageTexture;\n" +//inputImageTexture 纹理资源
            " \n" +
            "void main()\n" +
            "{\n" +
            "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "}";

    protected static final Utils.VERTEX_MODE mMode = Utils.VERTEX_MODE.DCAB;
    public static final float VERTEX_COORD[] = Utils.getVertexCoord(mMode);//根据不同顶点顺序获取顶点坐标数组

    protected static final float TEXTURE_COORD[] = Utils.getTextureCoord(false, false, 0, mMode);

    protected FloatBuffer mVertexBuffer;//顶点坐标
    protected FloatBuffer mTextureBuffer;//纹理坐标
    protected int mProgram;
    protected int mImageTextureId;
    protected boolean mHasAddPaddingX;
    protected boolean mHasAddPaddingY;

    protected int mMatrixHandle;
    protected int mPositionHandle;
    protected int mTextureCoordHandle;
    protected int mTextureHandle;

    protected int mTextureWidth;
    protected int mTextureHeight;
    protected int mDisplayWidth;
    protected int mDisplayHeight;

    //支持Fitcenter 和 centerCrop 还有 CENTER_INSIDE
    protected ImageView.ScaleType mScaleType = ImageView.ScaleType.FIT_CENTER;
    protected boolean mFlipV = false;//上下翻转
    protected boolean mFilpH = false;//左右翻转
    protected int mRotation = 0;//旋转
    protected RectF mShowRectF;

    protected final int[] mFrameBuffers = new int[1];
    protected final int[] mFrameBufferTextures = new int[1];
    protected IntBuffer mBitmapBuffer;
    protected Bitmap mBufferBitmap;
    protected Rect mReadPixelRect;
//    protected RectF mActionRect;
    protected FloatBuffer mFrameBufferVertexBuffer;//顶点坐标缓冲区
    protected FloatBuffer mFrameBufferTextureBuffer;//纹理坐标缓冲区
    protected int mFrameBufferRotationTextureWidth;
    protected int mFrameBufferRotationTextureHeight;

    protected String mVertexShader;
    protected String mFragmentShader;

    public ImageTexture(){
        mHasAddPaddingX = false;
        mHasAddPaddingY = false;
        mImageTextureId = OpenGlUtils.NO_TEXTURE;
        mVertexShader = VERTEX_SHADER;
        mFragmentShader = NO_FILTER_FRAGMENT_SHADER;
    }

    public ImageTexture(String vertexShader, String fragmentShader){
        mHasAddPaddingX = false;
        mHasAddPaddingY = false;
        mImageTextureId = OpenGlUtils.NO_TEXTURE;
        mVertexShader = vertexShader;
        mFragmentShader = fragmentShader;
    }

    @Override
    public void onInit() {//初始化各种句柄，为了之后赋值
        mProgram = OpenGlUtils.loadProgram(mVertexShader, mFragmentShader);//链接程序
        mMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "position");
        mTextureCoordHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
        mTextureHandle = GLES20.glGetUniformLocation(mProgram, "inputImageTexture");

        mVertexBuffer = ByteBuffer.allocateDirect(VERTEX_COORD.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mVertexBuffer.put(VERTEX_COORD).position(0);
        mTextureBuffer = ByteBuffer.allocateDirect(4 * 2 * 4)//4点，各点两位表示
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mTextureBuffer.put(TEXTURE_COORD).position(0);

        mFrameBufferVertexBuffer = ByteBuffer.allocateDirect(VERTEX_COORD.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mFrameBufferTextureBuffer = ByteBuffer.allocateDirect(VERTEX_COORD.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mShowRectF = new RectF();
        mReadPixelRect = new Rect();
    }

    @Override
    public void setTexture(int textureId, int width, int height, boolean hasAddPaddingX, boolean hasAddPaddingY) {
        mImageTextureId = textureId;
        mTextureWidth = width;
        mTextureHeight = height;
        mHasAddPaddingX = hasAddPaddingX;
        mHasAddPaddingY = hasAddPaddingY;
        onSizeChanged();
    }

    @Override
    public void onDisplaySizeChanged(int width, int height) {
        mDisplayWidth = width;
        mDisplayHeight = height;
        onSizeChanged();
    }

    @Override
    public void draw(float[] mvpMatrix) {
        if(mImageTextureId == OpenGlUtils.NO_TEXTURE) return;
//        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
//        //这里需要注意， 要先GLES20.glClearColor， 然后在glClear GLES20.GL_COLOR_BUFFER_BIT才会使清除的颜色生效
//        //glBindFramebuffer后执行这两句代码，作用的范围就是FrameBuffer，frameBuffer宽高有多大作用范围就有多大
//        //后面绘制纹理则不是按照FrameBuffer的大小绘制的，而已按照GLSurfaceView的大小（也就是GL的环境）绘制的
//        GLES20.glClearColor(0.0f, 0.5f, 0.0f, 1f);//清除颜色设为黑色
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);//整个窗口清除为当前的清除颜色  执行这一句才会显示效果
//        drawToFrameBuffer(mvpMatrix);
        GLES20.glViewport(0, 0, mDisplayWidth, mDisplayHeight);
//        GLES20.glViewport((int)mShowRectF.left, (int)mShowRectF.top, (int)mShowRectF.width(), (int)mShowRectF.height());
        //以下设置混合模式
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);//混合叠加
        GLES20.glUseProgram(mProgram);

        GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);//激活纹理单元0
        //mImageTextureId 为相当于加载纹理对象到内存中后的引用
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mImageTextureId);//绑定纹理为2D纹理
        GLES20.glUniform1i(mTextureHandle, 0);//绑定纹理 由于是GL_TEXTURE0所以绑定到0上

        mVertexBuffer.position(0);//顶点回归初始位置
        GLES20.glEnableVertexAttribArray(mPositionHandle);//激活句柄
        GLES20.glVertexAttribPointer(mPositionHandle, 2, GLES20.GL_FLOAT, false, 2 * 4, mVertexBuffer);//为句柄赋值，stride:每次读取的字节长度

        mTextureBuffer.position(0);
        GLES20.glEnableVertexAttribArray(mTextureCoordHandle);
        GLES20.glVertexAttribPointer(mTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 2 * 4, mTextureBuffer);//纹理坐标赋值

        onPreDraw();

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);//真正绘画，count：顶点数量四个顶点


//        mBitmapBuffer.clear();
//        mBitmapBuffer.position(0);
//
//        //读取到的像素和原图是不一样的， 是原图的上下翻转的结果， 原图是从左上开始的， 读取到的图是从左下开始的
//        //这里需要注意一点， 如果在glReadPixels之前将frameBuffer绑定到了0, 则读取的Pixel不是FramBuffer的, 而是绘制到屏幕的， 这种情况下如果读取的大小大过GLSurfaceView的大小，则大于的部分是透明的
//        GLES20.glReadPixels((int)mActionRect.left, (int)mActionRect.top, (int)mActionRect.width(), (int)mActionRect.height(), GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mBitmapBuffer);
//        mBitmapBuffer.position(0);
//        mBufferBitmap.copyPixelsFromBuffer(mBitmapBuffer);
//
//        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTextureCoordHandle);
    }

    public void onPreDraw(){

    }

    /**
     * 把当前画面写到FrameBuffer中，再转成bitmap
     * @param mvpMatrix
     */
    @Override
    public void drawToFrameBuffer(float[] mvpMatrix) {
        if(mImageTextureId == OpenGlUtils.NO_TEXTURE) return;
        GLES20.glViewport(0, 0, mFrameBufferRotationTextureWidth, mFrameBufferRotationTextureHeight);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);//使用自定义帧缓冲对象
        //这里需要注意， 要先GLES20.glClearColor， 然后在glClear GLES20.GL_COLOR_BUFFER_BIT才会使清除的颜色生效
        //glBindFramebuffer后执行这两句代码，作用的范围就是FrameBuffer，frameBuffer宽高有多大作用范围就有多大
        //后面绘制纹理则不是按照FrameBuffer的大小绘制的，而已按照GLSurfaceView的大小（也就是GL的环境）绘制的
//        GLES20.glClearColor(0.0f, 0.5f, 0.0f, 1f);//清除颜色设为黑色
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);//整个窗口清除为当前的清除颜色  执行这一句才会显示效果

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glUseProgram(mProgram);

        GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mImageTextureId);
        GLES20.glUniform1i(mTextureHandle, 0);//绑定纹理 由于是GL_TEXTURE0所以绑定到0上

        mFrameBufferVertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, 2, GLES20.GL_FLOAT, false, 2 * 4, mFrameBufferVertexBuffer);

        mFrameBufferTextureBuffer.position(0);
        GLES20.glEnableVertexAttribArray(mTextureCoordHandle);
        GLES20.glVertexAttribPointer(mTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 2 * 4, mFrameBufferTextureBuffer);

        onPreDraw();

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);


        mBitmapBuffer.clear();
        mBitmapBuffer.position(0);

        //读取到的像素和原图是不一样的， 是原图的上下翻转的结果， 原图是从左上开始的， 读取到的图是从左下开始的
        //这里需要注意一点， 如果在glReadPixels之前将frameBuffer绑定到了0, 则读取的Pixel不是FramBuffer的, 而是绘制到屏幕的， 这种情况下如果读取的大小大过GLSurfaceView的大小，则大于的部分是透明的
        GLES20.glReadPixels(mReadPixelRect.left, mReadPixelRect.top, mReadPixelRect.width(), mReadPixelRect.height(), GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mBitmapBuffer);
        mBitmapBuffer.position(0);
        mBufferBitmap.copyPixelsFromBuffer(mBitmapBuffer);//buffer转成bitmap

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTextureCoordHandle);
    }

    @Override
    public void onSizeChanged(){
        if(mImageTextureId == OpenGlUtils.NO_TEXTURE) return;
        recountVertexBuffer();//这里有两种方式， 一种是直接修改顶点坐标达到想要的效果， 第二种是通过修改视口达到效果
        recountFrameVertexBuffer();
    }

    public void setScaleType(ImageView.ScaleType scaleType){
        if(mScaleType == scaleType) return;
        mScaleType = scaleType;
        onSizeChanged();
    }

    public void setRotation(int rotation){
        rotation = (rotation % 360 + 360) % 360;//保证是正的
        if(mRotation == rotation) return;
        mRotation = rotation;
        onSizeChanged();
    }

    public void setFlip(boolean h, boolean v){
        if(mFilpH == h && mFlipV == v) return;
        mFilpH = h;
        mFlipV = v;
        onSizeChanged();
    }

    public int getImageTextureId(){
        return mImageTextureId;
    }

    /**
     * 从新计算顶点缓冲区，供新变换使用
     * 通过更改顶点坐标实现
     */
    public void recountVertexBuffer(){
        int rotationTW = mTextureWidth;
        int rotationTH = mTextureHeight;
        if(mRotation == 90 || mRotation == 270){//这个旋转角度导致宽高调转
            int cache = rotationTW;
            rotationTW = rotationTH;
            rotationTH = cache;
        }

        float vertex[];
        if(mScaleType == ImageView.ScaleType.CENTER_CROP) {
            float textureRatio = 1.0f * rotationTW / rotationTH;
            float displayRatio = 1.0f * mDisplayWidth / mDisplayHeight;
            float newWidth;
            float newHeight;
            if (textureRatio >= displayRatio){//texture按照比例缩放后高会和display一样大
                newHeight = mDisplayHeight;
                newWidth = 1.0f * mDisplayHeight * rotationTW / rotationTH;
            } else{
                newWidth = mDisplayWidth;
                newHeight = 1.0f * mDisplayWidth * rotationTH / rotationTW;
            }
            float dx = (mDisplayWidth - newWidth) / 2;
            float dy = (mDisplayHeight - newHeight) / 2;
            mShowRectF.set(dx, dy, newWidth + dx, newHeight + dy);
            vertex = Utils.getStandardVertex(mShowRectF, mDisplayWidth, mDisplayHeight, mMode);//mShowRectF为把图片按照比例缩放后的显示框，一般会超出屏幕
        } else if (mScaleType == ImageView.ScaleType.CENTER_INSIDE){//如果原图小不会进行拉伸
            float newWidth = rotationTW;
            float newHeight = rotationTH;
            if(rotationTW > mDisplayWidth){
                newWidth = mDisplayWidth;
                newHeight = 1.0f * mDisplayWidth * rotationTH / rotationTW;
                if(newHeight > mDisplayHeight){//防止都大于的情况下要缩小到都小于或者等于
                    newHeight = mDisplayHeight;
                    newWidth = 1.0f * mDisplayHeight * rotationTW / rotationTH;
                }
            } else if(rotationTH > mDisplayHeight){
                newHeight = mDisplayHeight;
                newWidth = 1.0f * mDisplayHeight * rotationTW / rotationTH;
            }
            float dx = (mDisplayWidth - newWidth) / 2;
            float dy = (mDisplayHeight - newHeight) / 2;
            mShowRectF.set(dx, dy, newWidth + dx, newHeight + dy);
            vertex = Utils.getStandardVertex(mShowRectF, mDisplayWidth, mDisplayHeight, mMode);
        } else{
            float textureRatio = 1.0f * rotationTW / rotationTH;
            float displayRatio = 1.0f * mDisplayWidth / mDisplayHeight;
            float newWidth;
            float newHeight;
            if (textureRatio >= displayRatio){//texture按照比例缩放后宽会和display一样大
                newWidth = mDisplayWidth;
                newHeight = 1.0f * mDisplayWidth * rotationTH / rotationTW;
            } else{
                newHeight = mDisplayHeight;
                newWidth = 1.0f * mDisplayHeight * rotationTW / rotationTH;
            }
            float dx = (mDisplayWidth - newWidth) / 2;
            float dy = (mDisplayHeight - newHeight) / 2;
            mShowRectF.set(dx, dy, newWidth + dx, newHeight + dy);
            vertex = Utils.getStandardVertex(mShowRectF, mDisplayWidth, mDisplayHeight, mMode);
        }

        System.arraycopy(vertex, 0, VERTEX_COORD, 0, VERTEX_COORD.length);//更新全局顶点坐标属性
        mVertexBuffer.clear();
        mVertexBuffer.put(vertex);//更新顶点缓冲区（顶点坐标只处理裁剪区域计算）
        mVertexBuffer.position(0);

        float textureCoord[] = Utils.getTextureCoord(mFilpH, mFlipV, mRotation, mMode);//翻转和旋转只在纹理上处理
        System.arraycopy(textureCoord, 0, TEXTURE_COORD, 0, TEXTURE_COORD.length);
        mTextureBuffer.clear();
        mTextureBuffer.put(textureCoord);
        mTextureBuffer.position(0);
    }

    /**
     * 通过更改视口实现
     */
    private void recountVertexBuffer2(){
        int rotationTW = mTextureWidth;
        int rotationTH = mTextureHeight;
        if(mRotation == 90 || mRotation == 270){
            int cache = rotationTW;
            rotationTW = rotationTH;
            rotationTH = cache;
        }
        if(mScaleType == ImageView.ScaleType.CENTER_CROP) {
            float textureRatio = 1.0f * rotationTW / rotationTH;
            float displayRatio = 1.0f * mDisplayWidth / mDisplayHeight;
            float newWidth;
            float newHeight;
            if (textureRatio >= displayRatio){//texture按照比例缩放后高会和display一样大
                newHeight = mDisplayHeight;
                newWidth = 1.0f * mDisplayHeight * rotationTW / rotationTH;
            } else{
                newWidth = mDisplayWidth;
                newHeight = 1.0f * mDisplayWidth * rotationTH / rotationTW;
            }
            float dx = (mDisplayWidth - newWidth) / 2;
            float dy = (mDisplayHeight - newHeight) / 2;
            mShowRectF.set(dx, dy, newWidth + dx, newHeight + dy);
        } else if (mScaleType == ImageView.ScaleType.CENTER_INSIDE){//如果原图小不会进行拉伸
            float newWidth = rotationTW;
            float newHeight = rotationTH;
            if(rotationTW > mDisplayWidth){
                newWidth = mDisplayWidth;
                newHeight = 1.0f * mDisplayWidth * rotationTH / rotationTW;
                if(newHeight > mDisplayHeight){//防止都大于的情况下要缩小到都小于或者等于
                    newHeight = mDisplayHeight;
                    newWidth = 1.0f * mDisplayHeight * rotationTW / rotationTH;
                }
            } else if(rotationTH > mDisplayHeight){
                newHeight = mDisplayHeight;
                newWidth = 1.0f * mDisplayHeight * rotationTW / rotationTH;
            }
            float dx = (mDisplayWidth - newWidth) / 2;
            float dy = (mDisplayHeight - newHeight) / 2;
            mShowRectF.set(dx, dy, newWidth + dx, newHeight + dy);
        } else{
            float textureRatio = 1.0f * rotationTW / rotationTH;
            float displayRatio = 1.0f * mDisplayWidth / mDisplayHeight;
            float newWidth;
            float newHeight;
            if (textureRatio >= displayRatio){//texture按照比例缩放后宽会和display一样大
                newWidth = mDisplayWidth;
                newHeight = 1.0f * mDisplayWidth * rotationTH / rotationTW;
            } else{
                newHeight = mDisplayHeight;
                newWidth = 1.0f * mDisplayHeight * rotationTW / rotationTH;
            }
            float dx = (mDisplayWidth - newWidth) / 2;
            float dy = (mDisplayHeight - newHeight) / 2;
            mShowRectF.set(dx, dy, newWidth + dx, newHeight + dy);
        }
        float textureCoord[] = Utils.getTextureCoord(mFilpH, mFlipV, mRotation, mMode);
        System.arraycopy(textureCoord, 0, TEXTURE_COORD, 0, TEXTURE_COORD.length);
        mTextureBuffer.clear();
        mTextureBuffer.put(textureCoord);
        mTextureBuffer.position(0);
    }

    private void recountFrameVertexBuffer(){
        //第一种方式
//        OpenGlUtils.initFBO(width, height, mFrameBuffers, mFrameBufferTextures);
//        mActionRect = Utils.vertexToActionRect(VERTEX_COORD, mDisplayWidth, mDisplayHeight, mMode);
//        mBufferBitmap = Bitmap.createBitmap((int)mActionRect.width(), (int)mActionRect.height(), Bitmap.Config.ARGB_8888);
//        mBitmapBuffer = IntBuffer.allocate(mBufferBitmap.getWidth() * mBufferBitmap.getHeight());

        int actionFrameTextureWidth;
        int actionFrameTextureHeight;
        mFrameBufferRotationTextureWidth = mTextureWidth;
        mFrameBufferRotationTextureHeight = mTextureHeight;
        if(mRotation == 90 || mRotation == 270){
            int cache = mFrameBufferRotationTextureWidth;
            mFrameBufferRotationTextureWidth = mFrameBufferRotationTextureHeight;
            mFrameBufferRotationTextureHeight = cache;

            actionFrameTextureWidth = mFrameBufferRotationTextureWidth;
            actionFrameTextureHeight = mFrameBufferRotationTextureHeight;
            if(mHasAddPaddingX){//旋转后其实是增加在高上的
                actionFrameTextureHeight = actionFrameTextureHeight - DemoRenderer.ADD_SIZE;
            }

            if(mHasAddPaddingY){
                actionFrameTextureWidth = actionFrameTextureWidth - DemoRenderer.ADD_SIZE;
            }
        } else{
            actionFrameTextureWidth = mFrameBufferRotationTextureWidth;
            actionFrameTextureHeight = mFrameBufferRotationTextureHeight;
            if(mHasAddPaddingX){
                actionFrameTextureWidth = actionFrameTextureWidth - DemoRenderer.ADD_SIZE;
            }

            if(mHasAddPaddingY){
                actionFrameTextureHeight = actionFrameTextureHeight - DemoRenderer.ADD_SIZE;
            }
        }
        //第二种方式
        OpenGlUtils.initFBO(mFrameBufferRotationTextureWidth, mFrameBufferRotationTextureHeight, mFrameBuffers, mFrameBufferTextures);
        RectF originalRect = new RectF(0, 0, mFrameBufferRotationTextureWidth, mFrameBufferRotationTextureHeight);
        float[] frameBufferVertexCoord = Utils.getStandardVertex(originalRect, mFrameBufferRotationTextureWidth, mFrameBufferRotationTextureHeight, mMode);
        mFrameBufferVertexBuffer.clear();
        mFrameBufferVertexBuffer.put(frameBufferVertexCoord);
        mFrameBufferVertexBuffer.position(0);
        //这里的参数增加进行了修正mFlipV -> !mFlipV  是因为从FrameBuffer   glReadPixels时图片会上下翻转
        float textureCoord[] = Utils.getTextureCoord(mFilpH, !mFlipV, mRotation, mMode);
        mFrameBufferTextureBuffer.clear();
        mFrameBufferTextureBuffer.put(textureCoord);
        mFrameBufferTextureBuffer.position(0);
        mBufferBitmap = Bitmap.createBitmap(actionFrameTextureWidth, actionFrameTextureHeight, Bitmap.Config.ARGB_8888);
        mBitmapBuffer = IntBuffer.allocate(mBufferBitmap.getWidth() * mBufferBitmap.getHeight());
        mReadPixelRect.set(0, 0, actionFrameTextureWidth, actionFrameTextureHeight);
        float textureCoordNotFlip[] = Utils.getTextureCoord(mFilpH, mFlipV, mRotation, mMode);
        Utils.countReadPixelRect(mReadPixelRect, DemoRenderer.ADD_SIZE, mHasAddPaddingX, mHasAddPaddingY, textureCoordNotFlip, mMode);
        System.out.println(mReadPixelRect);
    }

    public Bitmap getBitmap(float[] mvpMatrix) {
        drawToFrameBuffer(mvpMatrix);
        return mBufferBitmap;
    }

    public void deleteTexture(){
        GLES20.glDeleteTextures(1, new int[]{
                mImageTextureId
        }, 0);
        mImageTextureId = OpenGlUtils.NO_TEXTURE;
    }

    @Override
    public void destory() {

    }
}
