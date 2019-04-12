package chenfangyi.com.myopengldemo.utils;

import android.content.Context;
import android.graphics.Bitmap;

import chenfangyi.com.myopengldemo.renderer.DemoRenderer;

/**
 * Created by chenfangyi on 17-5-16.
 */

public class GPUImage {

    public static Bitmap getApplyBitmap(Context context, Bitmap bitmap, boolean recycle){
        DemoRenderer renderer = new DemoRenderer(context);
        renderer.setImageBitmap(bitmap, recycle);
        renderer.setRotation(180);
        renderer.setFlip(true, true);
        PixelBuffer mBuffer = new PixelBuffer(bitmap.getWidth(), bitmap.getHeight());
        mBuffer.setRenderer(renderer);
        Bitmap dstBitmap = mBuffer.getBitmap();
        mBuffer.destroy();
        return dstBitmap;
    }

}
