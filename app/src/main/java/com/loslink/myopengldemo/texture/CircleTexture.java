package com.loslink.myopengldemo.texture;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import com.loslink.myopengldemo.utils.OpenGlUtils;
import com.loslink.myopengldemo.utils.Utils;

/**
 * Created by chenfangyi on 17-5-18.
 */

public class CircleTexture implements ITexture {

//    private final String vertexShaderCode =
//            // This matrix member variable provides a hook to manipulate
//            // the coordinates of the objects that use this vertex shader
//            "uniform mat4 uMVPMatrix;" +
//                    "attribute vec4 vPosition;" +
//                    "void main() {" +
//                    // The matrix must be included as a modifier of gl_Position.
//                    // Note that the uMVPMatrix factor *must be first* in order
//                    // for the matrix multiplication product to be correct.
//                    "  gl_Position = vPosition * uMVPMatrix;" +
//                    "}";
//
//    private final String fragmentShaderCode =
//            "precision mediump float;" +
//                    "uniform vec4 vColor;" +
//                    "void main() {" +
//                    "  gl_FragColor = vColor;" +
//                    "}";

    private final String testVShaer = "uniform mat4 uMVPMatrix;                        //总变换矩阵\n" +
            "\n" +
            "uniform vec3 uLightLocation;                    //光源位置\n" +
            "attribute vec3 vPosition;                       //顶点位置\n" +
            "attribute vec3 aNormal;                         //顶点法向量\n" +
            "\n" +
            "varying vec4 vDiffuse;                          //用于传递给片元着色器的散射光分量\n" +
            "\n" +
            "void main()     \n" +
            "{                                   \n" +
            "    gl_Position = uMVPMatrix * vec4(vPosition,1);\n" +
            "\n" +
            "\n" +
            "    vec4 vAmbient = vec4(0.9, 0.9, 0.9, 1.0);       //设置环境光强度\n" +
            "\n" +
            "    vec3 normalTarget=vPosition+aNormal;\n" +
            "    vec3 newNormal=(vec4(normalTarget,1)).xyz-(vec4(vPosition,1)).xyz;      //求出法向量\n" +
            "    newNormal=normalize(newNormal);                     //向量规格化\n" +
            "\n" +
            "    vec3 vp= normalize(uLightLocation-(vec4(vPosition,1)).xyz);             //计算从表面点到光源位置的向量\n" +
            "    vp=normalize(vp);                                   //向量规格化\n" +
            "\n" +
            "    float nDotViewPosition=max(0.0, dot(newNormal,vp));     //求法向量与vp向量的点积与0的最大值\n" +
            "\n" +
            "    vDiffuse=vAmbient*nDotViewPosition;             //计算散射光的最终强度\n" +
            "}";

    private final String testFShaer = "precision mediump float;\n" +
            "varying vec4 vDiffuse;  //用于传递给片元着色器的散射光分量\n" +
            "uniform vec4 vColor;" +
            "void main()                         \n" +
            "{\n" +
            "    gl_FragColor = vColor * vDiffuse;  //通过散射光分量获得最终颜色\n" +
            "}";

    private int mProgram;
    private int mLightLocationHandle;
    private int mPositionHandle;
    private int mNormalHandle;
    private int mMatrixHandle;
    private int mColorHandle;

    private float mColor[] = new float[]{
        1f, 0f, 1f, 1f
    };
    private FloatBuffer mVertexBuffer;
    private int mLength;

    private FloatBuffer mNormalBuffer;

    private FloatBuffer mLightBuffer;

    private int mDisplayWidth;
    private int mDisplayHeight;

    @Override
    public void onInit() {
        mProgram = OpenGlUtils.loadProgram(testVShaer, testFShaer);//链接程序
        mLightLocationHandle = GLES20.glGetUniformLocation(mProgram, "uLightLocation");
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        mMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        mNormalHandle = GLES20.glGetAttribLocation(mProgram, "aNormal");
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        countBallVertex();
    }

    private void countCircleVertex(){
        float radius = 1.0f;
        float singleDegree = 0.75f;
        int number = (int)(360f / singleDegree);
        number += 2;//原点和第二个点
        float[] vertexCoord = new float[number * 2];
        vertexCoord[0] = 0;
        vertexCoord[1] = 0;
        int count = number - 1;//原点已经加入
        for(int key = 1 ; key <= count ; key++){//以Y轴为起始点顺时针
            int x = key * 2;
            int y = x + 1;
            float yDegree = (key - 1) * singleDegree;
            yDegree = yDegree % 360;
            double radian = Math.toRadians(yDegree);
            vertexCoord[x] = (float)Math.sin(radian) * radius;
            vertexCoord[y] = (float)Math.cos(radian) * radius;
        }
        mVertexBuffer = ByteBuffer.allocateDirect(vertexCoord.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertexBuffer.put(vertexCoord).position(0);
        mLength = vertexCoord.length / 2;
    }

    private void countBallVertex(){
        float radius = 1.0f;
        float ySingleDegree = 1f;
        float xzSingleDegree = 1f;

        int numberY = (int)(180f / ySingleDegree);
        int numberXZ = (int)(360f / xzSingleDegree) + 1;//360完全满了  所以不需要加1

        long time = System.currentTimeMillis();
        float[] vertexs = new float[numberXZ * numberY * 3 * 6];
        int index = 0;
        for(int i = 0; i < numberY; i++){//和Y轴正方向的夹角
            float degreeY = i * ySingleDegree - 90;
//            degreeY = degreeY % 360;
            double radianY = Math.toRadians(degreeY);
            float z = radius * (float)Math.sin(radianY);//i
            float xy = radius * (float)Math.cos(radianY);//i

            float zJPlus = z;

            degreeY = (i + 1) * ySingleDegree - 90;
//            degreeY = degreeY % 360;
            radianY = Math.toRadians(degreeY);
            float zIPlus = radius * (float)Math.sin(radianY);//i + 1
            float xyIPlus = radius * (float)Math.cos(radianY);//i + 1

            float zIJPlus = zIPlus;

            for(int j = 0 ; j < numberXZ ; j++){//和Z轴正方向的夹角
                float degreeZ = j * xzSingleDegree;
//                degreeZ = degreeZ % 360;
                double radianZ = Math.toRadians(degreeZ);
                float cosRadianZ = (float)Math.cos(radianZ);
                float sinRadianZ = (float)Math.sin(radianZ);

                float x = xy * cosRadianZ;//j
                float y = xy * sinRadianZ;//j

                float xIPlus = xyIPlus * cosRadianZ;//i + 1
                float yIPlus = xyIPlus * sinRadianZ;//i + 1

                //下面是j + 1
                degreeZ = (j + 1) * ySingleDegree;
                radianZ = Math.toRadians(degreeZ);
                cosRadianZ = (float)Math.cos(radianZ);
                sinRadianZ = (float)Math.sin(radianZ);

                float xJPlus = xy * cosRadianZ;//j + 1
                float yJPlus = xy * sinRadianZ;//j + 1

                float xIJPlus = xyIPlus * cosRadianZ;//i + 1 j + 1
                float yIJPlus = xyIPlus * sinRadianZ;//i + 1 j + 1

                vertexs[index++] = xJPlus;
                vertexs[index++] = yJPlus;
                vertexs[index++] = zJPlus;

                vertexs[index++] = xIPlus;
                vertexs[index++] = yIPlus;
                vertexs[index++] = zIPlus;


                vertexs[index++] = x;
                vertexs[index++] = y;
                vertexs[index++] = z;

                vertexs[index++] = xJPlus;
                vertexs[index++] = yJPlus;
                vertexs[index++] = zJPlus;

                vertexs[index++] = xIJPlus;
                vertexs[index++] = yIJPlus;
                vertexs[index++] = zIJPlus;

                vertexs[index++] = xIPlus;
                vertexs[index++] = yIPlus;
                vertexs[index++] = zIPlus;
            }
        }
        System.out.println("time = " + (System.currentTimeMillis() - time));

        mLength = vertexs.length / 3;
        mVertexBuffer = ByteBuffer.allocateDirect(vertexs.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertexBuffer.put(vertexs).position(0);

        mNormalBuffer = ByteBuffer.allocateDirect(vertexs.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mNormalBuffer.put(vertexs).position(0);

        //ByteOrder.nativeOrder()返回本地jvm运行的硬件的字节顺序.使用和硬件一致的字节顺序可能使buffer更加有效.
        mLightBuffer = ByteBuffer.allocateDirect(vertexs.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        float light[] = new float[]{
                1f, 0.5f, 1.5f
        };
        mLightBuffer.put(light).position(0);

//        float r = 1.0f;
//        ArrayList<Float> alVertix = new ArrayList<Float>();
//        final int angleSpan = 180;// 将球进行单位切分的角度
//        for (int vAngle = -90; vAngle < 90; vAngle = vAngle + angleSpan)// 垂直方向angleSpan度一份
//        {
//            for (int hAngle = 0; hAngle <= 360; hAngle = hAngle + angleSpan)// 水平方向angleSpan度一份
//            {
//                float x0 = (float) (r * Math.cos(Math.toRadians(vAngle)) * Math.cos(Math.toRadians(hAngle)));
//                float y0 = (float) (r * Math.cos(Math.toRadians(vAngle)) * Math.sin(Math.toRadians(hAngle)));
//                float z0 = (float) (r * Math.sin(Math .toRadians(vAngle)));
//
//                float x1 = (float) (r * Math.cos(Math.toRadians(vAngle)) * Math.cos(Math.toRadians(hAngle + angleSpan)));
//                float y1 = (float) (r * Math.cos(Math.toRadians(vAngle)) * Math.sin(Math.toRadians(hAngle + angleSpan)));
//                float z1 = (float) (r  * Math.sin(Math.toRadians(vAngle)));
//
//                float x2 = (float) (r * Math.cos(Math.toRadians(vAngle + angleSpan)) * Math.cos(Math.toRadians(hAngle + angleSpan)));
//                float y2 = (float) (r * Math.cos(Math.toRadians(vAngle + angleSpan)) * Math.sin(Math.toRadians(hAngle + angleSpan)));
//                float z2 = (float) (r  * Math.sin(Math.toRadians(vAngle + angleSpan)));
//
//                float x3 = (float) (r * Math.cos(Math.toRadians(vAngle + angleSpan)) * Math.cos(Math.toRadians(hAngle)));
//                float y3 = (float) (r * Math.cos(Math.toRadians(vAngle + angleSpan)) * Math.sin(Math.toRadians(hAngle)));
//                float z3 = (float) (r * Math.sin(Math.toRadians(vAngle + angleSpan)));
//
//                alVertix.add(x1);
//                alVertix.add(y1);
//                alVertix.add(z1);
//                alVertix.add(x3);
//                alVertix.add(y3);
//                alVertix.add(z3);
//                alVertix.add(x0);
//                alVertix.add(y0);
//                alVertix.add(z0);
//
//                alVertix.add(x1);
//                alVertix.add(y1);
//                alVertix.add(z1);
//                alVertix.add(x2);
//                alVertix.add(y2);
//                alVertix.add(z2);
//                alVertix.add(x3);
//                alVertix.add(y3);
//                alVertix.add(z3);
//
//            }
//        }
//
//        mLength = alVertix.size() / 3;
//        float vertices[]=new float[alVertix.size()];
//        for (int i=0;i<vertices.length;i++) {
//            vertices[i] = alVertix.get(i);
//        }
//
//        mVertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
//        mVertexBuffer.put(vertices).position(0);
//
//        mNormalBuffer = ByteBuffer.allocateDirect(vertices.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
//        mNormalBuffer.put(vertices).position(0);
//
//        mLightBuffer = ByteBuffer.allocateDirect(vertices.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
//        float light[] = new float[]{
//                0f, 0f, 1.5f
//        };
//        mLightBuffer.put(light).position(0);
    }

    @Override
    public void setTexture(int textureId, int width, int height, boolean hasAddPaddingX, boolean hasAddPaddingY) {

    }

    @Override
    public void onDisplaySizeChanged(int width, int height) {
        mDisplayWidth = width;
        mDisplayHeight = height;
    }

    @Override
    public void onSizeChanged() {

    }

    @Override
    public void draw(float[] mvpMatrix) {
        GLES20.glUseProgram(mProgram);

        GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mvpMatrix, 0);

        mLightBuffer.position(0);
        GLES20.glUniform3fv(mLightLocationHandle, 1, mLightBuffer);

        mVertexBuffer.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 3 * 4, mVertexBuffer);
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        mNormalBuffer.position(0);
        GLES20.glVertexAttribPointer(mNormalHandle, 3, GLES20.GL_FLOAT, false, 3 * 4, mNormalBuffer);
        GLES20.glEnableVertexAttribArray(mNormalHandle);

        GLES20.glUniform4fv(mColorHandle, 1, mColor, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mLength);
    }

    public void setLightPosition(float curX, float curY){
        float []result = new float[]{
                curX, curY
        };
        Utils.countStandardVertex(result, mDisplayWidth, mDisplayHeight);//标准化
        float light[] = new float[]{
                result[0], result[1], 1.5f
        };
        mLightBuffer.clear();
        mLightBuffer.put(light).position(0);
    }

    @Override
    public void drawToFrameBuffer(float[] mvpMatrix) {

    }

    @Override
    public void destory() {

    }

    public class SetLightPositionRunnable implements Runnable{
        float mCurX;
        float mCurY;
        public SetLightPositionRunnable(float curX, float curY){
            mCurX = curX;
            mCurY = curY;
        }

        @Override
        public void run() {
            setLightPosition(mCurX, mCurY);
        }
    }

    public SetLightPositionRunnable getSetLightPositionRunnable(float curX, float curY){
        return new SetLightPositionRunnable(curX, curY);
    }
}
