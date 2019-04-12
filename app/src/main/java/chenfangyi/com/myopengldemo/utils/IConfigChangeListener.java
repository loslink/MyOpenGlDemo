package chenfangyi.com.myopengldemo.utils;

/**
 * Created by chenfangyi on 17-5-16.
 */

public interface IConfigChangeListener {
    void onRotateChanged(int rotate);
    void onFlipChanged(boolean flipH, boolean flipV);
}
