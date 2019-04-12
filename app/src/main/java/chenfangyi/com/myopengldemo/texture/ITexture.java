package chenfangyi.com.myopengldemo.texture;

/**
 * Created by chenfangyi on 17-5-12.
 */

public interface ITexture {
    void onInit();

    void setTexture(int textureId, int width, int height, boolean hasAddPaddingX, boolean hasAddPaddingY);

    void onDisplaySizeChanged(int width, int height);

    void onSizeChanged();

    void draw(float[] mvpMatrix);

    void drawToFrameBuffer(float[] mvpMatrix);

    void destory();
}
