package com.loslink.myopengldemo.texture;

import android.content.Context;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;
import android.widget.ImageView;

import com.loslink.myopengldemo.renderer.VideoTextureRenderer2;
import com.loslink.myopengldemo.utils.Utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by loslink on 2019/9/26.
 */

public class VideoTexture implements SurfaceTexture.OnFrameAvailableListener {

    private static  String vertexShaderCode ;
    private static String fragmentShaderCode ;

    private static float squareSize = 1.0f;
    //surface顶点 ，顶点数组
//    private float vertexCoords[] = {-squareSize, squareSize, 0.0f,   // top left
//            -squareSize, -squareSize, 0.0f,   // bottom left
//            squareSize, -squareSize, 0.0f,   // bottom right
//            squareSize, squareSize, 0.0f}; // top right

    private static short drawOrder[] = {0, 1, 2, 0, 2, 3};

    private Context ctx;

    // Texture to be shown in backgrund
    private FloatBuffer textureBuffer;
    //纹理数组（图片）
//    private float textureCoords[] = {0.0f, 1.0f, 0.0f, 1.0f,
//            0.0f, 0.0f, 0.0f, 1.0f,
//            1.0f, 0.0f, 0.0f, 1.0f,
//            1.0f, 1.0f, 0.0f, 1.0f};
    private int[] textures = new int[1];

    private int vertexShaderHandle;
    private int fragmentShaderHandle;
    private int shaderProgram;
    private FloatBuffer vertexBuffer;
    private ShortBuffer drawListBuffer;

    private SurfaceTexture videoTexture;
    private float[] videoTextureTransform;
    private boolean frameAvailable = false;

    private int videoWidth;
    private int videoHeight;
    private boolean adjustViewport = false;
    private RectF cropRect;
    private VideoTextureRenderer2.OnStateListner onStateListner;
    protected int width;
    protected int height;

    //支持Fitcenter 和 centerCrop 还有 CENTER_INSIDE
    protected ImageView.ScaleType mScaleType = ImageView.ScaleType.FIT_CENTER;
    protected boolean mFlipV = false;//上下翻转
    protected boolean mFilpH = false;//左右翻转
    protected int mRotation = 0;//旋转
    protected RectF mShowRectF;
    protected static final Utils.VERTEX_MODE mMode = Utils.VERTEX_MODE.DCAB;
    public static final float vertexCoords[] = Utils.getVertexCoord(mMode);//根据不同顶点顺序获取顶点坐标数组
    protected static final float textureCoords[] = Utils.getTextureCoord(false, false, 0, mMode);

    public VideoTexture(){
    }

    public VideoTexture(String vertexShader, String fragmentShader, int width, int height){
        vertexShaderCode = vertexShader;
        fragmentShaderCode = fragmentShader;
        videoTextureTransform = new float[16];
        mShowRectF = new RectF();
        this.width=width;
        this.height=height;
    }

    /**
     * 加载顶点/片段着色器
     */
    private void loadShaders() {
        vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);//创建顶点着色器
        GLES20.glShaderSource(vertexShaderHandle, vertexShaderCode);//着色器关联脚本
        GLES20.glCompileShader(vertexShaderHandle);//编译脚本
        checkGlError("Vertex shader compile");

        fragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fragmentShaderHandle, fragmentShaderCode);
        GLES20.glCompileShader(fragmentShaderHandle);
        checkGlError("Pixel shader compile");

        shaderProgram = GLES20.glCreateProgram();//创建程序
        GLES20.glAttachShader(shaderProgram, vertexShaderHandle);//绑定顶点着色器
        GLES20.glAttachShader(shaderProgram, fragmentShaderHandle);
        GLES20.glLinkProgram(shaderProgram);//gl环境关联该程序
        checkGlError("Shader program compile");

        int[] status = new int[1];
        GLES20.glGetProgramiv(shaderProgram, GLES20.GL_LINK_STATUS, status, 0);//检查脚本程序有没有问题
        if (status[0] != GLES20.GL_TRUE) {
            String error = GLES20.glGetProgramInfoLog(shaderProgram);
            Log.e("SurfaceTest", "Error while linking program:\n" + error);
        }
        setupTexture();
        setupVertexBuffer();
    }

    /**
     * 从新计算顶点缓冲区，供新变换使用
     * 通过更改顶点坐标实现
     */
    public void recountVertexBuffer(){
        int rotationTW = videoWidth;
        int rotationTH = videoHeight;
        if(mRotation == 90 || mRotation == 270){//这个旋转角度导致宽高调转
            int cache = rotationTW;
            rotationTW = rotationTH;
            rotationTH = cache;
        }

        float vertex[];
        if(mScaleType == ImageView.ScaleType.CENTER_CROP) {
            float textureRatio = 1.0f * rotationTW / rotationTH;
            float displayRatio = 1.0f * width / height;
            float newWidth;
            float newHeight;
            if (textureRatio >= displayRatio){//texture按照比例缩放后高会和display一样大
                newHeight = height;
                newWidth = 1.0f * height * rotationTW / rotationTH;
            } else{
                newWidth = width;
                newHeight = 1.0f * width * rotationTH / rotationTW;
            }
            float dx = (width - newWidth) / 2;
            float dy = (height - newHeight) / 2;
            mShowRectF.set(dx, dy, newWidth + dx, newHeight + dy);
            vertex = Utils.getStandardVertex(mShowRectF, width, height, mMode);//mShowRectF为把图片按照比例缩放后的显示框，一般会超出屏幕
        } else if (mScaleType == ImageView.ScaleType.CENTER_INSIDE){//如果原图小不会进行拉伸
            float newWidth = rotationTW;
            float newHeight = rotationTH;
            if(rotationTW > width){
                newWidth = width;
                newHeight = 1.0f * width * rotationTH / rotationTW;
                if(newHeight > height){//防止都大于的情况下要缩小到都小于或者等于
                    newHeight = height;
                    newWidth = 1.0f * height * rotationTW / rotationTH;
                }
            } else if(rotationTH > height){
                newHeight = height;
                newWidth = 1.0f * height * rotationTW / rotationTH;
            }
            float dx = (width - newWidth) / 2;
            float dy = (height - newHeight) / 2;
            mShowRectF.set(dx, dy, newWidth + dx, newHeight + dy);
            vertex = Utils.getStandardVertex(mShowRectF, width, height, mMode);
        } else{
            float textureRatio = 1.0f * rotationTW / rotationTH;
            float displayRatio = 1.0f * width / height;
            float newWidth;
            float newHeight;
            if (textureRatio >= displayRatio){//texture按照比例缩放后宽会和display一样大
                newWidth = width;
                newHeight = 1.0f * width * rotationTH / rotationTW;
            } else{
                newHeight = height;
                newWidth = 1.0f * height * rotationTW / rotationTH;
            }
            float dx = (width - newWidth) / 2;
            float dy = (height - newHeight) / 2;
            mShowRectF.set(dx, dy, newWidth + dx, newHeight + dy);
            vertex = Utils.getStandardVertex(mShowRectF, width, height, mMode);
        }

        System.arraycopy(vertex, 0, vertexCoords, 0, vertexCoords.length);//更新全局顶点坐标属性
        vertexBuffer.clear();
        vertexBuffer.put(vertex);//更新顶点缓冲区（顶点坐标只处理裁剪区域计算）
        vertexBuffer.position(0);

        float textureCoord[] = Utils.getTextureCoord(mFilpH, mFlipV, mRotation, mMode);//翻转和旋转只在纹理上处理
        System.arraycopy(textureCoord, 0, textureCoords, 0, textureCoords.length);
        textureBuffer.clear();
        textureBuffer.put(textureCoord);
        textureBuffer.position(0);
    }



    //创建缓冲区，并往缓冲区填充数据，供具柄使用（可以动态传递）
    private void setupVertexBuffer() {
        // Draw list buffer
        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        // Initialize the texture holder
        ByteBuffer bb = ByteBuffer.allocateDirect(vertexCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());

        //顶点缓冲区
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertexCoords);
        vertexBuffer.position(0);
    }


    private void setupTexture() {

        ByteBuffer texturebb = ByteBuffer.allocateDirect(textureCoords.length * 4);
        texturebb.order(ByteOrder.nativeOrder());
        //纹理缓冲区
        textureBuffer = texturebb.asFloatBuffer();
        textureBuffer.put(textureCoords);
        textureBuffer.position(0);

        // Generate the actual texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glGenTextures(1, textures, 0);
        checkGlError("Texture generate");

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        checkGlError("Texture bind");


        videoTexture = new SurfaceTexture(textures[0]);//SurfaceTexture绑定到纹理，当有视频帧原始数据，更新到该纹理供OpenGL
        videoTexture.setOnFrameAvailableListener(this);//监听有没数据到来
        if (onStateListner != null) {
            onStateListner.onTextureInitSuccess();
        }
    }

    /**
     * 子线程定时回调
     * @return
     */
    public boolean draw() {
        recountVertexBuffer();
        synchronized (this) {
            if (frameAvailable) {
                /*
                * SurfaceTexture从Android 3.0(API level 11)加入。和SurfaceView不同的是，它对图像流的处理并不直接显示，
                * 而是转为GL外部纹理，因此可用于图像流数据的二次处理（如Camera滤镜，桌面特效等）。比如Camera的预览数据，
                * 变成纹理后可以交给GLSurfaceView直接显示，也可以通过SurfaceTexture交给TextureView作为View heirachy中
                * 的一个硬件加速层来显示。首先，SurfaceTexture从图像流（来自Camera预览，视频解码，GL绘制场景等）中获得帧数据，
                * 当调用updateTexImage()时，根据内容流中最近的图像更新SurfaceTexture对应的GL纹理对象，接下来，就可以像操作普
                * 通GL纹理一样操作它了。从下面的类图中可以看出，它核心管理着一个BufferQueue的Consumer和Producer两端。
                * Producer端用于内容流的源输出数据，Consumer端用于拿GraphicBuffer并生成纹理。
                * SurfaceTexture.OnFrameAvailableListener用于让SurfaceTexture的使用者知道有新数据到来。
                * JNISurfaceTextureContext是OnFrameAvailableListener从Native到Java的JNI跳板。其中SurfaceTexture中
                * 的attachToGLContext()和detachToGLContext()可以让多个GL context共享同一个内容源。*/
                videoTexture.updateTexImage();//有数据到来，更新到gl纹理
                videoTexture.getTransformMatrix(videoTextureTransform);
                frameAvailable = false;
            } else {
                return false;
            }

        }

        if (adjustViewport)
            adjustViewport();//调整窗口

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);//背景颜色
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Draw texture
        GLES20.glUseProgram(shaderProgram);//使用该脚本程序
        //获取相应的参数具柄，供动态传值进去
        int textureParamHandle = GLES20.glGetUniformLocation(shaderProgram, "texture");
        int textureCoordinateHandle = GLES20.glGetAttribLocation(shaderProgram, "vTexCoordinate");
        int positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        int textureTranformHandle = GLES20.glGetUniformLocation(shaderProgram, "textureTransform");


        GLES20.glEnableVertexAttribArray(positionHandle);//启动顶点数组具柄
        //把缓冲区传递进该具柄(size:数组中每3个元素为一个顶点，stride:4 * 3即FLOAT字节大小*3个元素为一个顶点)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 4 * 2, vertexBuffer);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);//绑定0号纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);//激活0号纹理
        GLES20.glUniform1i(textureParamHandle, 0);

        GLES20.glEnableVertexAttribArray(textureCoordinateHandle);//启动纹理坐标具柄
        //传递纹理坐标进纹理具柄
        GLES20.glVertexAttribPointer(textureCoordinateHandle, 2, GLES20.GL_FLOAT, false, 2 * 4, textureBuffer);

        //传递纹理矩阵进去
        GLES20.glUniformMatrix4fv(textureTranformHandle, 1, false, videoTextureTransform, 0);

//        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);//真正绘画，count：顶点数量四个顶点
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(textureCoordinateHandle);

        return true;
    }

    private void adjustViewport() {

        GLES20.glViewport(0, 0, width, height);//显示视频中间并全屏
        adjustViewport = false;
    }

    public RectF getCropRect() {
        return cropRect;
    }

    public void setCropRect(RectF cropRect) {
        this.cropRect = cropRect;
    }


    public void initGLComponents() {
//        ajustVideo();
        setupVertexBuffer();
        setupTexture();
        loadShaders();
    }

    public void deinitGLComponents() {
        GLES20.glDeleteTextures(1, textures, 0);
        GLES20.glDeleteProgram(shaderProgram);
        videoTexture.release();
        videoTexture.setOnFrameAvailableListener(null);
    }

    public void setVideoSize(int width, int height) {
        this.videoWidth = width;
        this.videoHeight = height;
        adjustViewport = true;
    }

    public void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e("SurfaceTest", op + ": glError " + GLUtils.getEGLErrorString(error));
        }
    }

    public SurfaceTexture getVideoSurfaceTexture() {
        return videoTexture;
    }

    public VideoTextureRenderer2.OnStateListner getOnStateListner() {
        return onStateListner;
    }

    public void setOnStateListner(VideoTextureRenderer2.OnStateListner onStateListner) {
        this.onStateListner = onStateListner;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        synchronized (this) {
            frameAvailable = true;
        }
    }
}
