package com.loslink.myopengldemo;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.AssetFileDescriptor;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

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
public class VideoPreviewCropActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    private TextureView surface;
    private MediaPlayer player;

    private VideoTextureRenderer renderer;
    public static final String VIDEO_PATH = "VIDEO_PATH";
    public static final String VIDEO_CROP_RECT = "VIDEO_CROP_RECT";
    public static final String VIDEO_OUT_PATH = "VIDEO_OUT_PATH";
    public static final String VIDEO_CROP_PARAM = "VIDEO_CROP_PARAM";

    private int surfaceWidth;
    private int surfaceHeight;
    private String videoPath;
    private RectF cropRect;
    private Timer mTimer = new Timer();
    private TimerTask mTimerTask;
    private float progress;
    private String outputPath;
    private boolean isCropping = false;

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
        outputPath = intent.getStringExtra(VIDEO_OUT_PATH);
        cropRect=new RectF(0,0,720,1280);
    }


    protected void initData() {
        getIntentExtral(getIntent());
//        if (cropParam.getOutputWidth() > cropParam.getOutputHeight()) {
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
//        }

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
        if(mTimer!=null){
            mTimer.cancel();
        }
        if(mTimerTask!=null){
            mTimerTask.cancel();
        }
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
                VideoPreviewCropActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String allTimeStr=DateUtils.formatElapsedTime(player.getDuration() / 1000);
//                        tv_duration.setText(allTimeStr);
                    }
                });

                mTimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        VideoPreviewCropActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if(player!=null && player.isPlaying()){
                                        long currentDuration = player.getCurrentPosition();
//                                        seekbar.setProgress((int) (((float)currentDuration / player.getDuration()) * 100));
//                                        String timeStr=DateUtils.formatElapsedTime(currentDuration / 1000);
//                                        tv_position.setText(timeStr);
                                    }
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                };
                mTimer.schedule(mTimerTask, 0, 10);
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
