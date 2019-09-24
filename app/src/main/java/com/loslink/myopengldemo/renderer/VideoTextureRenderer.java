package com.loslink.myopengldemo.renderer;


import android.content.Context;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;


import com.loslink.myopengldemo.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class VideoTextureRenderer extends TextureSurfaceRenderer implements SurfaceTexture.OnFrameAvailableListener {
    private static  String vertexShaderCode ;


    private static String fragmentShaderCode ;


    private static float squareSize = 1.0f;
    //surface顶点 ，顶点数组
    private float squareCoords[] = {-squareSize, squareSize, 0.0f,   // top left
            -squareSize, -squareSize, 0.0f,   // bottom left
            squareSize, -squareSize, 0.0f,   // bottom right
            squareSize, squareSize, 0.0f}; // top right

    private static short drawOrder[] = {0, 1, 2, 0, 2, 3};

    private Context ctx;

    // Texture to be shown in backgrund
    private FloatBuffer textureBuffer;
    //纹理数组（图片）
    private float textureCoords[] = {0.0f, 1.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 0.0f, 1.0f,
        1.0f, 0.0f, 0.0f, 1.0f,
        1.0f, 1.0f, 0.0f, 1.0f};
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
    private OnStateListner onStateListner;

    public VideoTextureRenderer(Context context, SurfaceTexture texture, int width, int height) {
        super(texture, width, height);
        this.ctx = context;
        videoTextureTransform = new float[16];
        vertexShaderCode = readTextFileFromResource(context, R.raw.simple_vertex_shader);
        fragmentShaderCode =readTextFileFromResource(context,R.raw.simple_fragment_shader);
    }

    public OnStateListner getOnStateListner() {
        return onStateListner;
    }

    public void setOnStateListner(OnStateListner onStateListner) {
        this.onStateListner = onStateListner;
    }

    /**
     * 读取着色器代码
     * @param context 上下文
     * @param resourceId 资源ID
     * @return 代码字符串
     */
    public static String readTextFileFromResource(Context context,int resourceId){
        StringBuilder sb = new StringBuilder();

        try {
            InputStream is = context.getResources().openRawResource(resourceId);
            InputStreamReader inputStreamReader = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(inputStreamReader);
            String nextLine;
            while((nextLine = br.readLine())!=null){
                sb.append(nextLine);
                sb.append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
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
        ByteBuffer bb = ByteBuffer.allocateDirect(squareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());

        //顶点缓冲区
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);
    }


    private void setupTexture(Context context) {

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
     * 裁剪框内视频适配显示，初始化执行一次
     */
    private void ajustVideo(){

        float topLeftX = 0,topLeftY = 0;
        float topRightX = 0,topRightY = 0;
        float bottomLeftX = 0,bottomLeftY = 0;
        float bottomRightX = 0,bottomRightY = 0;

        topLeftX=cropRect.left/videoWidth;
        topLeftY=(videoHeight-cropRect.top)/videoHeight;
        topRightX=cropRect.right/videoWidth;
        topRightY=(videoHeight-cropRect.top)/videoHeight;
        bottomLeftX=cropRect.left/videoWidth;
        bottomLeftY=(videoHeight-cropRect.bottom)/videoHeight;
        bottomRightX=cropRect.right/videoWidth;
        bottomRightY=(videoHeight-cropRect.bottom)/videoHeight;

        textureCoords[0]=topLeftX;
        textureCoords[1]=topLeftY;
        textureCoords[4]=bottomLeftX;
        textureCoords[5]=bottomLeftY;
        textureCoords[8]=bottomRightX;
        textureCoords[9]=bottomRightY;
        textureCoords[12]=topRightX;
        textureCoords[13]=topRightY;

        float topLeftSurfaceX = 0,topLeftSurfaceY = 0;
        float topRightSurfaceX = 0,topRightSurfaceY = 0;
        float bottomLeftSurfaceX = 0,bottomLeftSurfaceY = 0;
        float bottomRightSurfaceX = 0,bottomRightSurfaceY = 0;

        float cropW = cropRect.right - cropRect.left;
        float cropH = cropRect.bottom - cropRect.top;
        float cropAspect = cropH / cropW;
        float surfaceAspect = (float) height / width;
        float scale = 1;
        float realVideoH,realVideoW;

        if(cropAspect > surfaceAspect){//高度填满surface
            realVideoH=height;
            realVideoW=realVideoH/cropAspect;
            topLeftSurfaceY=squareCoords[1];
            topRightSurfaceY=squareCoords[10];
            bottomLeftSurfaceY=squareCoords[4];
            bottomRightSurfaceY=squareCoords[7];

            scale=realVideoW/width;
            topLeftSurfaceX=squareCoords[0]*scale;
            topRightSurfaceX=squareCoords[9]*scale;
            bottomLeftSurfaceX=squareCoords[3]*scale;
            bottomRightSurfaceX=squareCoords[6]*scale;
        }else{//宽度填满surface
            realVideoW=width;
            realVideoH=realVideoW*cropAspect;
            topLeftSurfaceX=squareCoords[0];
            topRightSurfaceX=squareCoords[9];
            bottomLeftSurfaceX=squareCoords[3];
            bottomRightSurfaceX=squareCoords[6];

            scale=realVideoH/height;
            topLeftSurfaceY=squareCoords[1]*scale;
            topRightSurfaceY=squareCoords[10]*scale;
            bottomLeftSurfaceY=squareCoords[4]*scale;
            bottomRightSurfaceY=squareCoords[7]*scale;

        }

        squareCoords[0]=topLeftSurfaceX;
        squareCoords[1]=topLeftSurfaceY;
        squareCoords[3]=bottomLeftSurfaceX;
        squareCoords[4]=bottomLeftSurfaceY;
        squareCoords[6]=bottomRightSurfaceX;
        squareCoords[7]=bottomRightSurfaceY;
        squareCoords[9]=topRightSurfaceX;
        squareCoords[10]=topRightSurfaceY;

    }

    /**
     * 子线程定时回调
     * @return
     */
    @Override
    protected boolean draw() {
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
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 4 * 3, vertexBuffer);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);//绑定0号纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);//激活0号纹理
        GLES20.glUniform1i(textureParamHandle, 0);

        GLES20.glEnableVertexAttribArray(textureCoordinateHandle);//启动纹理坐标具柄
        //传递纹理坐标进纹理具柄
        GLES20.glVertexAttribPointer(textureCoordinateHandle, 4, GLES20.GL_FLOAT, false, 0, textureBuffer);

        //传递纹理矩阵进去
        GLES20.glUniformMatrix4fv(textureTranformHandle, 1, false, videoTextureTransform, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
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


    @Override
    protected void initGLComponents() {
        ajustVideo();
        setupVertexBuffer();
        setupTexture(ctx);
        loadShaders();
    }

    @Override
    protected void deinitGLComponents() {
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

    public SurfaceTexture getVideoTexture() {
        return videoTexture;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        synchronized (this) {
            frameAvailable = true;

        }
    }

    public interface OnStateListner {
        void onTextureInitSuccess();
    }
}
