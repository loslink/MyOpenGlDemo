package com.loslink.myopengldemo.renderer;

import android.opengl.GLSurfaceView;

import com.loslink.myopengldemo.utils.ICreateBitmap;

/**
 * Created by chenfangyi on 17-5-17.
 */

public abstract class AbstractBitmapRenderer implements GLSurfaceView.Renderer, ICreateBitmap {

    public abstract void destory();
}
