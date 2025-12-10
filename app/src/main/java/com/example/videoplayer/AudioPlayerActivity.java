package com.example.videoplayer;

import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.videoplayer.common.AudioAttributes;
import com.example.videoplayer.common.C;
import com.example.videoplayer.common.MediaItem;
import com.example.videoplayer.common.PlaybackException;
import com.example.videoplayer.common.Player;
import com.example.videoplayer.common.Tracks;
import com.example.videoplayer.exoplayer.ExoPlayer;

import java.io.IOException;
import java.util.Locale;

public class AudioPlayerActivity extends AppCompatActivity implements View.OnClickListener {

    // 控件
    private Button btnPlay;
    private Button btnPause;
    private Button btnStop;
    private Button btnPrevious;
    private Button btnNext;
    private Button btnLoadAssetsAudio;
    private SeekBar seekBar;
    private TextView tvStatus;
    private TextView tvAudioInfo;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;

    // 播放器实例
    private ExoPlayer player;
    private boolean isPlaying = false;
    private boolean isAudioLoaded = false;

    // 定时更新进度
    private final Handler handler = new Handler();
    private final Runnable updateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            handler.postDelayed(this, 500); // 每500毫秒更新一次
        }
    };

    // Assets音频文件路径
    private static final String ASSETS_AUDIO_PATH = "224a.mp3";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);

        initViews();
        setClickListeners();
        initPlayer();
    }

    private void initViews() {
        btnPlay = findViewById(R.id.btn_play);
        btnPause = findViewById(R.id.btn_pause);
        btnStop = findViewById(R.id.btn_stop);
        btnPrevious = findViewById(R.id.btn_previous);
        btnNext = findViewById(R.id.btn_next);
        btnLoadAssetsAudio = findViewById(R.id.btn_load_assets_audio);
        seekBar = findViewById(R.id.seek_bar);
        tvStatus = findViewById(R.id.tv_status);
        tvAudioInfo = findViewById(R.id.tv_audio_info);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);

        // 返回按钮
        Button btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());
    }

    private void setClickListeners() {
        btnPlay.setOnClickListener(this);
        btnPause.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        btnPrevious.setOnClickListener(this);
        btnNext.setOnClickListener(this);
        btnLoadAssetsAudio.setOnClickListener(this);

        // SeekBar进度监听
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null && isAudioLoaded) {
                    long duration = player.getDuration();
                    if (duration != C.TIME_UNSET && duration > 0) {
                        long newPosition = progress * duration / 100;
                        player.seekTo(newPosition);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 开始拖动时暂停自动更新
                handler.removeCallbacks(updateProgressRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 停止拖动后恢复自动更新
                handler.post(updateProgressRunnable);
            }
        });
    }

    private void initPlayer() {
        // 创建音频属性
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build();

        // 创建播放器
        player = new ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, true)
                .build();

        // 设置播放器监听器
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                String stateText = "";
                switch (playbackState) {
                    case Player.STATE_IDLE:
                        stateText = "闲置";
                        break;
                    case Player.STATE_BUFFERING:
                        stateText = "缓冲中";
                        break;
                    case Player.STATE_READY:
                        stateText = "准备就绪";
                        isAudioLoaded = true;
                        updateUIForAudioLoaded();
                        break;
                    case Player.STATE_ENDED:
                        stateText = "播放结束";
                        isPlaying = false;
                        updatePlayButtonState();
                        break;
                }
                updateStatus("播放状态: " + stateText);
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                AudioPlayerActivity.this.isPlaying = isPlaying;
                updatePlayButtonState();
                updateStatus(isPlaying ? "正在播放" : "已暂停");
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Player.Listener.super.onPlayerError(error);
                Log.e("AudioPlayer", "播放错误: " + error.getMessage());
                updateStatus("错误: " + error.getMessage());
                Toast.makeText(AudioPlayerActivity.this,
                    "播放错误: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onTracksChanged(Tracks tracks) {
                Player.Listener.super.onTracksChanged(tracks);
                Log.d("AudioPlayer", "音频轨道信息: " + tracks.toString());
            }
        });
    }

    private void loadAssetsAudio() {
        try {
            // 从assets加载音频文件
            AssetFileDescriptor afd = getAssets().openFd(ASSETS_AUDIO_PATH);
            Uri assetUri = Uri.parse("asset:///" + ASSETS_AUDIO_PATH);

            // 创建媒体项目并设置给播放器
            MediaItem mediaItem = MediaItem.fromUri(assetUri);
            player.setMediaItem(mediaItem);
            player.prepare();

            updateStatus("正在加载音频文件: " + ASSETS_AUDIO_PATH);
            isAudioLoaded = false;
            updateUIForAudioLoading();

        } catch (IOException e) {
            Log.e("AudioPlayer", "加载assets音频失败: " + e.getMessage());
            updateStatus("加载失败: " + e.getMessage());
            Toast.makeText(this, "加载音频文件失败: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
        }
    }

    private void updateUIForAudioLoading() {
        runOnUiThread(() -> {
            btnPlay.setEnabled(false);
            btnPause.setEnabled(false);
            btnStop.setEnabled(false);
            seekBar.setEnabled(false);
            tvAudioInfo.setText("加载中...");
        });
    }

    private void updateUIForAudioLoaded() {
        runOnUiThread(() -> {
            btnPlay.setEnabled(true);
            btnPause.setEnabled(true);
            btnStop.setEnabled(true);
            seekBar.setEnabled(true);

            // 获取音频信息

            long duration = player.getDuration();
            String durationStr = formatDuration(duration);
            tvAudioInfo.setText("音频文件: " + ASSETS_AUDIO_PATH + "\n时长: " + durationStr);
            tvTotalTime.setText(durationStr);

            // 启动进度更新

            handler.post(updateProgressRunnable);
        });
    }

    private void updateProgress() {
        if (player != null && isAudioLoaded) {
            long currentPosition = player.getCurrentPosition();
            long duration = player.getDuration();

            runOnUiThread(() -> {
                if (duration > 0 && duration != C.TIME_UNSET) {
                    int progress = (int) (currentPosition * 100 / duration);
                    seekBar.setProgress(progress);
                    tvCurrentTime.setText(formatDuration(currentPosition));
                }
                tvCurrentTime.setText(formatDuration(currentPosition));
            });
        }
    }

    private void updateStatus(String status) {
        runOnUiThread(() -> tvStatus.setText("状态: " + status));
    }

    private void updatePlayButtonState() {
        runOnUiThread(() -> {
            btnPlay.setEnabled(!isPlaying);
            btnPause.setEnabled(isPlaying);
        });
    }

    private String formatDuration(long durationMs) {
        if (durationMs == C.TIME_UNSET) {
            return "00:00";
        }

        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d",
                hours, minutes % 60, seconds % 60);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d",
                minutes, seconds % 60);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btn_load_assets_audio) {
            loadAssetsAudio();
        } else if (id == R.id.btn_play && player != null && isAudioLoaded) {
            player.play();
            isPlaying = true;
            updatePlayButtonState();
        } else if (id == R.id.btn_pause && player != null && isAudioLoaded) {
            player.pause();
            isPlaying = false;
            updatePlayButtonState();
        } else if (id == R.id.btn_stop && player != null) {
            player.stop();
            isPlaying = false;
            isAudioLoaded = false;
            updatePlayButtonState();
            seekBar.setProgress(0);
            tvCurrentTime.setText("00:00");
        } else if (id == R.id.btn_previous) {
            if (player != null && isAudioLoaded) {
                long currentPosition = player.getCurrentPosition();
                long newPosition = Math.max(currentPosition - 10000, 0);
                player.seekTo(newPosition);
                updateStatus("后退10秒");
            }
        } else if (id == R.id.btn_next) {
            if (player != null && isAudioLoaded) {
                long currentPosition = player.getCurrentPosition();
                long duration = player.getDuration();
                long newPosition = Math.min(currentPosition + 10000, duration);
                player.seekTo(newPosition);
                updateStatus("快进10秒");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateProgressRunnable);
        if (player != null) {
            player.release();
            player = null;
        }
    }
}