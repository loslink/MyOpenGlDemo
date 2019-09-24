package com.loslink.myopengldemo.texture;

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
 * Created by chenfangyi on 17-5-12.
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

    public static final String LOOKUP_FRAGMENT_SHADER = "    precision mediump float;\n" +
            "    varying highp vec2 textureCoordinate;\n" +
            "    varying highp vec2 textureCoordinate2; // TODO: This is not used\n" +
            "\n" +
            "    uniform sampler2D inputImageTexture;\n" +
            "    uniform sampler2D inputImageTexture2; // lookup texture\n" +
            "    uniform lowp float intensity;\n" +
            "    void main()\n" +
            "    {\n" +
            "        mediump vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "\n" +
            "        mediump float blueColor = textureColor.b * 63.0;\n" +
            "\n" +
            "        mediump vec2 quad1;\n" +
            "        quad1.y = floor(floor(blueColor) / 8.0);\n" +
            "        quad1.x = floor(blueColor) - (quad1.y * 8.0);\n" +
            "\n" +
            "        mediump vec2 quad2;\n" +
            "        quad2.y = floor(ceil(blueColor) / 8.0);\n" +
            "        quad2.x = ceil(blueColor) - (quad2.y * 8.0);\n" +
            "        quad1 = clamp(quad1, vec2(0.0), vec2(7.0));\n" +
            "        quad2 = clamp(quad2, vec2(0.0), vec2(7.0));\n" +
            "\n" +
            "        highp vec2 texPos1;\n" +
            "        texPos1.x = (quad1.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.r);\n" +
            "        texPos1.y = (quad1.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.g);\n" +
            "\n" +
            "        highp vec2 texPos2;\n" +
            "        texPos2.x = (quad2.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.r);\n" +
            "        texPos2.y = (quad2.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.g);\n" +
            "\n" +
            "        mediump vec4 newColor1 = texture2D(inputImageTexture2, texPos1);\n" +
            "        mediump vec4 newColor2 = texture2D(inputImageTexture2, texPos2);\n" +
            "\n" +
            "        mediump vec4 newColor = mix(newColor1, newColor2, fract(blueColor));\n" +
            "        gl_FragColor = mix(textureColor, vec4(newColor.rgb, textureColor.a), intensity);\n" +
            "    }";

    private int mFilterTextureCoordHandle;
    private int mFilterTextureHandle;
    private int mIntensityHandle;

    protected FloatBuffer mFilterTextureBuffer;//纹理坐标
    protected int mFilterTextureId = -1;

    public ImageFilterTexture(){
        super(VERTEX_SHADER, LOOKUP_FRAGMENT_SHADER);
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
