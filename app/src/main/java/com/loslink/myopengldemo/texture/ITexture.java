package com.loslink.myopengldemo.texture;

/**
 * 纹理基类，约束纹理的操作
 */

public interface ITexture {
    /**
     * 初始化各种具柄
     */
    void onInit();

    /**
     * 设置外部图片资源信息
     * @param textureId 纹理ID
     * @param width
     * @param height
     * @param hasAddPaddingX
     * @param hasAddPaddingY
     */
    void setTexture(int textureId, int width, int height, boolean hasAddPaddingX, boolean hasAddPaddingY);

    /**
     * 窗口大小变化
     * @param width
     * @param height
     */
    void onDisplaySizeChanged(int width, int height);

    /**
     * 重新计算图片显示
     */
    void onSizeChanged();

    /**
     * 绘制纹理
     * @param mvpMatrix
     */
    void draw(float[] mvpMatrix);

    /**
     * 把当前纹理写到FrameBuffer中，再转成bitmap
     * @param mvpMatrix
     */
    void drawToFrameBuffer(float[] mvpMatrix);

    /**
     * 善后工作
     */
    void destory();
}
