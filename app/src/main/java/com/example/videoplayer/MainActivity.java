package com.example.videoplayer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import com.example.videoplayer.common.AudioAttributes;
import com.example.videoplayer.common.C;
import com.example.videoplayer.common.MediaItem;
import com.example.videoplayer.common.PlaybackException;
import com.example.videoplayer.common.Player;
import com.example.videoplayer.common.Tracks;
import com.example.videoplayer.exoplayer.ExoPlayer;

import android.widget.Button;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    String url = "https://filesamples.com/samples/video/f4v/sample_640x360.f4v";
    String url2 = "https://filesamples.com/samples/video/f4v/sample_960x400_ocean_with_audio.f4v";
    TextView tvLoadUrl;
    SurfaceView surfaceView;

    // 测试模块控件
    Button btnPlay;
    Button btnPause;
    Button btnStop;
    Button btnSeekForward;
    Button btnSeekBackward;
    Button btnSwitchUrl;
    TextView tvStatus;
    TextView tvVideoInfo;

    // 播放器实例
    private ExoPlayer player;
    private boolean isVideoLoaded = false;
    private boolean usingFirstUrl = true;

    // 定时更新UI
    private final Handler handler = new Handler();
    private final Runnable updateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            updateCurrentPosition();
            handler.postDelayed(this, 1000); // 每秒更新一次
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化控件
        tvLoadUrl = findViewById(R.id.tv_loadurl);
        surfaceView = findViewById(R.id.surface_view);

        btnPlay = findViewById(R.id.btn_play);
        btnPause = findViewById(R.id.btn_pause);
        btnStop = findViewById(R.id.btn_stop);
        btnSeekForward = findViewById(R.id.btn_seek_forward);
        btnSeekBackward = findViewById(R.id.btn_seek_backward);
        btnSwitchUrl = findViewById(R.id.btn_switch_url);
        tvStatus = findViewById(R.id.tv_status);
        tvVideoInfo = findViewById(R.id.tv_video_info);

        // 新增音频播放按钮
        Button btnAudioPlayer = findViewById(R.id.btn_audio_player);

        // 设置点击监听器
        tvLoadUrl.setOnClickListener(this);
        btnPlay.setOnClickListener(this);
        btnPause.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        btnSeekForward.setOnClickListener(this);
        btnSeekBackward.setOnClickListener(this);
        btnSwitchUrl.setOnClickListener(this);
        btnAudioPlayer.setOnClickListener(this);

        // 初始化测试模块控件状态
        updateTestControlsState(false);
    }

    private void loadVideo(){
        if (player != null) {
            player.release();
        }

        // 创建音频属性
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build();

        // 创建播放器并设置音频属性
        player = new ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, true) // true表示处理音频焦点
                .build();
        player.setVideoSurfaceView(surfaceView);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                Player.Listener.super.onPlayerError(error);
                Log.e(TAG, "onPlayerError: "+error.getMessage() );
                updateStatus("错误: " + error.getMessage());
            }

            @Override
            public void onPlayerErrorChanged(@Nullable PlaybackException error) {
                Player.Listener.super.onPlayerErrorChanged(error);
                Log.e(TAG, "onPlayerErrorChanged: "+error.getMessage() );
            }

            @Override
            public void onEvents(Player player, Player.Events events) {
                Player.Listener.super.onEvents(player, events);
                for (int i = 0; i < events.size(); i++) {
                    Log.e(TAG, "onEvents: "+events.get(i) );
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Player.Listener.super.onPlaybackStateChanged(playbackState);
                String stateStr = "";
                switch (playbackState) {
                    case Player.STATE_IDLE:
                        stateStr = "闲置";
                        break;
                    case Player.STATE_BUFFERING:
                        stateStr = "缓冲中";
                        break;
                    case Player.STATE_READY:
                        stateStr = "准备就绪";
                        isVideoLoaded = true;
                        updateTestControlsState(true);
                        updateVideoInfo();
                        startProgressUpdate(); // 开始更新进度
                        break;
                    case Player.STATE_ENDED:
                        stateStr = "播放结束";
                        break;
                }
                updateStatus("播放状态: " + stateStr);
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                Player.Listener.super.onIsPlayingChanged(isPlaying);
                updateStatus(isPlaying ? "正在播放" : "已暂停");
            }

            @Override
            public void onTracksChanged(Tracks tracks) {
                Player.Listener.super.onTracksChanged(tracks);
                logTracksInfo(tracks);
            }
        });

        String currentUrl = usingFirstUrl ? url : url2;
        MediaItem mediaItem = MediaItem.fromUri(currentUrl);
        player.setMediaItem(mediaItem);
        player.prepare();

        isVideoLoaded = false;
        updateTestControlsState(false);
        updateStatus("加载视频中: " + (usingFirstUrl ? "URL1" : "URL2"));
    }

    private static final String TAG = "MainActivity";

    // 辅助方法
    private void logTracksInfo(Tracks tracks) {
        Log.d(TAG, "音轨信息:");
        // 简化版本，只记录基本信息
        Log.d(TAG, "音轨状态: " + tracks.toString());
    }

    private void updateTestControlsState(boolean enabled) {
        btnPlay.setEnabled(enabled);
        btnPause.setEnabled(enabled);
        btnStop.setEnabled(enabled);
        btnSeekForward.setEnabled(enabled);
        btnSeekBackward.setEnabled(enabled);
        btnSwitchUrl.setEnabled(true); // 切换URL总是可用
    }

    private void updateStatus(String status) {
        runOnUiThread(() -> {
            tvStatus.setText("状态: " + status);
        });
    }

    private void updateVideoInfo() {
        if (player != null && player.getCurrentMediaItem() != null) {
            runOnUiThread(() -> {
                String currentUrl = usingFirstUrl ? url : url2;
                tvVideoInfo.setText("视频: " + (usingFirstUrl ? "URL1" : "URL2") + "\n时长: " +
                    formatDuration(player.getDuration()) + "ms");
            });
        }
    }

    private String formatDuration(long durationMs) {
        if (durationMs == C.TIME_UNSET) {
            return "未知";
        }
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60);
        } else {
            return String.format("%d:%02d", minutes, seconds % 60);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.tv_loadurl) {
            loadVideo();
        } else if (id == R.id.btn_play && player != null && isVideoLoaded) {
            player.play();
            updateStatus("播放中");
        } else if (id == R.id.btn_pause && player != null && isVideoLoaded) {
            player.pause();
            updateStatus("已暂停");
        } else if (id == R.id.btn_stop && player != null) {
            player.stop();
            isVideoLoaded = false;
            updateTestControlsState(false);
            stopProgressUpdate(); // 停止进度更新
            updateStatus("已停止");
        } else if (id == R.id.btn_seek_forward && player != null && isVideoLoaded) {
            long currentPosition = player.getCurrentPosition();
            long duration = player.getDuration();
            long newPosition = Math.min(currentPosition + 10000, duration);
            player.seekTo(newPosition);
            updateStatus("快进到: " + formatDuration(newPosition));
        } else if (id == R.id.btn_seek_backward && player != null && isVideoLoaded) {
            long currentPosition = player.getCurrentPosition();
            long newPosition = Math.max(currentPosition - 10000, 0);
            player.seekTo(newPosition);
            updateStatus("后退到: " + formatDuration(newPosition));
        } else if (id == R.id.btn_switch_url) {
            usingFirstUrl = !usingFirstUrl;
            if (player != null) {
                loadVideo(); // 重新加载新URL的视频
            } else {
                updateStatus("切换到: " + (usingFirstUrl ? "URL1" : "URL2"));
                tvVideoInfo.setText("视频: " + (usingFirstUrl ? "URL1" : "URL2") + "\n请点击'加载视频'");
            }
        } else if (id == R.id.btn_audio_player) {
            // 跳转到音频播放界面
            Intent intent = new Intent(MainActivity.this, AudioPlayerActivity.class);
            startActivity(intent);
        }
    }

    private void updateCurrentPosition() {
        if (player != null && isVideoLoaded) {
            long currentPosition = player.getCurrentPosition();
            long duration = player.getDuration();

            runOnUiThread(() -> {
                String positionStr = formatDuration(currentPosition);
                String durationStr = formatDuration(duration);

                // 更新视频信息，包含当前位置
                String currentUrl = usingFirstUrl ? url : url2;
                tvVideoInfo.setText("视频: " + (usingFirstUrl ? "URL1" : "URL2") +
                    "\n时长: " + durationStr +
                    "\n位置: " + positionStr);
            });
        }
    }

    private void startProgressUpdate() {
        handler.removeCallbacks(updateProgressRunnable);
        handler.post(updateProgressRunnable);
    }

    private void stopProgressUpdate() {
        handler.removeCallbacks(updateProgressRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopProgressUpdate();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}