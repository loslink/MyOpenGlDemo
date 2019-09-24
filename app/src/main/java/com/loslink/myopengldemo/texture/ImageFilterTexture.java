package com.loslink.myopengldemo.texture;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import com.loslink.myopengldemo.DemoApp;
import com.loslink.myopengldemo.R;
import com.loslink.myopengldemo.utils.OpenGlUtils;
import com.loslink.myopengldemo.utils.Utils;

/**
 *
 * 绘制图片的类
 * 总结：
 * 顶点坐标控制绘制的范围
 * 纹理坐标控制纹理的哪一部分绘制
 * 两个坐标的整合就是：控制纹理的某一部分绘制到的某个位置
 */

public class ImageFilterTexture extends ImageTexture {
    protected static final String VERTEX_SHADER = "" +
            "uniform mat4 uMVPMatrix;\n" +
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            "attribute vec4 inputTextureCoordinate2;\n" +
            " \n" +
            "varying vec2 textureCoordinate;\n" +
            "varying vec2 textureCoordinate2;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position * uMVPMatrix;\n" +//顶点坐标
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +//纹理坐标
            "    textureCoordinate2 = inputTextureCoordinate2.xy;\n" +
            "}";

    public static String LOOKUP_FRAGMENT_SHADER ;

    private int mFilterTextureCoordHandle;
    private int mFilterTextureHandle;
    private int mIntensityHandle;

    protected FloatBuffer mFilterTextureBuffer;//纹理坐标
    protected int mFilterTextureId = -1;

    public ImageFilterTexture(Context context){
        super(VERTEX_SHADER, Utils.readTextFileFromResource(context,R.raw.lookup_fragment_shader));
        LOOKUP_FRAGMENT_SHADER = Utils.readTextFileFromResource(context,R.raw.lookup_fragment_shader);
        mFilterTextureBuffer = ByteBuffer.allocateDirect(4 * 2 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mFilterTextureBuffer.put(TEXTURE_COORD).position(0);
    }

    @Override
    public void onInit() {//初始化各种句柄
        super.onInit();
        mFilterTextureCoordHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate2");
        mFilterTextureHandle = GLES20.glGetUniformLocation(mProgram, "inputImageTexture2");;
        mIntensityHandle = GLES20.glGetUniformLocation(mProgram, "intensity");

        mFilterTextureId = OpenGlUtils.loadTexture(BitmapFactory.decodeResource(DemoApp.getApplication().getResources(), R.drawable.lookup_candy), OpenGlUtils.NO_TEXTURE, true);
    }

    @Override
    public void onPreDraw() {
        super.onPreDraw();

        GLES20.glEnableVertexAttribArray(mFilterTextureCoordHandle);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFilterTextureId);
        GLES20.glUniform1i(mFilterTextureHandle, 3);

        mFilterTextureBuffer.position(0);
        GLES20.glVertexAttribPointer(mFilterTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mFilterTextureBuffer);

        GLES20.glUniform1f(mIntensityHandle, 0.5f);
    }

    @Override
    public void recountVertexBuffer() {
        super.recountVertexBuffer();
        float textureCoord[] = Utils.getTextureCoord(mFilpH, mFlipV, mRotation, mMode);
        System.arraycopy(textureCoord, 0, TEXTURE_COORD, 0, TEXTURE_COORD.length);
        mFilterTextureBuffer.clear();
        mFilterTextureBuffer.put(textureCoord);
        mFilterTextureBuffer.position(0);
    }

    @Override
    public void draw(float[] mvpMatrix) {
        super.draw(mvpMatrix);
    }

    @Override
    public void drawToFrameBuffer(float[] mvpMatrix) {
        super.drawToFrameBuffer(mvpMatrix);
    }

    @Override
    public void destory() {
        super.destory();
    }
}
