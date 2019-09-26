package com.loslink.myopengldemo;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.view.Surface;
import android.view.TextureView;

import com.loslink.myopengldemo.renderer.VideoTextureRenderer;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.ButterKnife;

/**
 * 视频裁剪预览
 *
 * @author loslink
 * @time 2019/1/10 16:19
 */
public class VideoActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    private TextureView surface;
    private MediaPlayer player;

    private VideoTextureRenderer renderer;
    public static final String VIDEO_PATH = "VIDEO_PATH";
    public static final String VIDEO_CROP_RECT = "VIDEO_CROP_RECT";

    private int surfaceWidth;
    private int surfaceHeight;
    private String videoPath;
    private RectF cropRect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_preview_part);
        ButterKnife.bind(this);
        initView();
        initData();
    }


    protected void initView() {
        surface = findViewById(R.id.surface);

        surface.setSurfaceTextureListener(this);
    }


    private void getIntentExtral(Intent intent){
        videoPath = intent.getStringExtra(VIDEO_PATH);
        cropRect = intent.getParcelableExtra(VIDEO_CROP_RECT);
        cropRect=new RectF(0,0,720,720);
    }


    protected void initData() {
        getIntentExtral(getIntent());
        player = new MediaPlayer();
        AssetFileDescriptor fd = null;
        try {
            fd = getAssets().openFd("video_sample.mp4");
            player.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
//            player.setDataSource(videoPath);
            player.setLooping(true);
            player.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /**
     * 播放
     */
    private void startPlay(){
        if(player!=null){
            player.start();
        }
    }

    /**
     * 暂停
     */
    private void pausePlay(){
        if(player!=null){
            player.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (surface.isAvailable()) {
            startPlaying();
        }
        try {
            //重新回来，恢复播放
            if(player!=null && !player.isPlaying()){
                startPlay();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        try {
            //离开页面暂停播放
            if(player!=null && player.isPlaying()){
                pausePlay();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null)
            player.release();
        if (renderer != null)
            renderer.onPause();
    }

    private void startPlaying() {

        //该TextureView用来显示视频帧
        renderer = new VideoTextureRenderer(this, surface.getSurfaceTexture(), surfaceWidth, surfaceHeight);
        renderer.setCropRect(cropRect);
        renderer.setVideoSize(player.getVideoWidth(), player.getVideoHeight());
        renderer.setOnStateListner(new VideoTextureRenderer.OnStateListner() {
            @Override
            public void onTextureInitSuccess() {
                if (renderer.getVideoTexture() == null) {
                    return;
                }
                //SurfaceTexture可以获得没处理过的中间视频帧
                player.setSurface(new Surface(renderer.getVideoTexture()));//该Surface用来OpenGL获取视频帧
                startPlay();
                VideoActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String allTimeStr=DateUtils.formatElapsedTime(player.getDuration() / 1000);
//                        tv_duration.setText(allTimeStr);
                    }
                });

            }
        });
        renderer.start();

    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        surfaceWidth = width;
        surfaceHeight = height;
        startPlaying();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

}
