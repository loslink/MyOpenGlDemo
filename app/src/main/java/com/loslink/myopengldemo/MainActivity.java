package com.loslink.myopengldemo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.loslink.myopengldemo.renderer.NewRenderer;
import com.loslink.myopengldemo.view.CircleGLSurfaceView;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.bt_filter_image)
    public void bt_filter_image(){
        Intent intent=new Intent(this,ImageFilterActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.bt_image)
    public void bt_image(){
        Intent intent=new Intent(this,ImageActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.bt_3d_ball)
    public void bt_3d_ball(){
        Intent intent=new Intent(this,CircleActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.bt_video)
    public void bt_video(){
        Intent intent=new Intent(this,VideoPreviewCropActivity.class);
        startActivity(intent);
    }

}
