package com.loslink.myopengldemo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.loslink.myopengldemo.renderer.DemoRenderer;
import com.loslink.myopengldemo.utils.IConfigChangeListener;
import com.loslink.myopengldemo.view.DemoGLSurfaceView;

public class ImageActivity extends AppCompatActivity {

    DemoGLSurfaceView mGLSurfaceView;
    DemoRenderer mRenderer;
    TextView mRotateBt;
    TextView mFlipHBt;
    TextView mFlipVBt;
    int mRotate;
    boolean mFlipV;
    boolean mFlipH;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_activity_main);
        mGLSurfaceView = (DemoGLSurfaceView) findViewById(R.id.gl_view);
        mRenderer = new DemoRenderer(this);
        mGLSurfaceView.setRenderer(mRenderer);
        mGLSurfaceView.init();
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test_image6);
        mRenderer.setImageBitmap(bitmap, true);
        mRenderer.setOnConfigChangeListener(new IConfigChangeListener(){
            @Override
            public void onRotateChanged(final int rotate) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mRotate = rotate;
                        mRotateBt.setText(getResources().getString(R.string.rotate_value, rotate));
                    }
                });
            }

            @Override
            public void onFlipChanged(final boolean flipH, final boolean flipV) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mFlipH = flipH;
                        mFlipV = flipV;

                        mFlipHBt.setText(getResources().getString(R.string.flipH_value, flipH + ""));
                        mFlipVBt.setText(getResources().getString(R.string.flipV_value, flipV + ""));
                    }
                });
            }
        });
        mRenderer.setScaleType(ImageView.ScaleType.FIT_CENTER);

        mRotateBt = (TextView) findViewById(R.id.rotate_bt);
        mFlipHBt = (TextView) findViewById(R.id.flipH_bt);
        mFlipVBt = (TextView) findViewById(R.id.flipV_bt);
        mRotateBt.setText(getResources().getString(R.string.rotate_value, 0));
        mFlipHBt.setText(getResources().getString(R.string.flipH_value, "false"));
        mFlipVBt.setText(getResources().getString(R.string.flipV_value, "false"));
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(v == mRotateBt){
                    mRotate += 90;
                    mRotate = (mRotate % 360 + 360) % 360;//保证是正的
                    mRenderer.setRotation(mRotate);
                    mGLSurfaceView.requestRender();
                } else if(v == mFlipHBt){
                    mFlipH = !mFlipH;
                    mRenderer.setFlip(mFlipH, mFlipV);
                    mGLSurfaceView.requestRender();
                } else if(v == mFlipVBt){
                    mFlipV = !mFlipV;
                    mRenderer.setFlip(mFlipH, mFlipV);
                    mGLSurfaceView.requestRender();
                }
            }
        };
        mRotateBt.setOnClickListener(listener);
        mFlipHBt.setOnClickListener(listener);
        mFlipVBt.setOnClickListener(listener);
//
//        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test_image6);
//        Bitmap result = GPUImage.getApplyBitmap(this, bitmap, true);
//        System.out.println(result);
//        mRenderer.setRotation(180);
//        mRenderer.setFlip(true, false);z
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
