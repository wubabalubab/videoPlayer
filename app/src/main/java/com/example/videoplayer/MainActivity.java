package com.example.videoplayer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import com.example.videoplayer.common.MediaItem;
import com.example.videoplayer.common.PlaybackException;
import com.example.videoplayer.common.Player;
import com.example.videoplayer.exoplayer.ExoPlayer;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    String url = "https://filesamples.com/samples/video/f4v/sample_640x360.f4v";
    String url2 = "https://filesamples.com/samples/video/f4v/sample_960x400_ocean_with_audio.f4v";
    TextView tvLoadUrl;
    SurfaceView surfaceView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLoadUrl = findViewById(R.id.tv_loadurl);
        surfaceView = findViewById(R.id.surface_view);

        tvLoadUrl.setOnClickListener(this);
    }

    private void loadVideo(){

        ExoPlayer player = new ExoPlayer.Builder(this).build();
        player.setVideoSurfaceView(surfaceView);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                Player.Listener.super.onPlayerError(error);
                Log.e(TAG, "onPlayerError: "+error.getMessage() );
            }

            @Override
            public void onPlayerErrorChanged(@Nullable PlaybackException error) {
                Player.Listener.super.onPlayerErrorChanged(error);
                Log.e(TAG, "onPlayerErrorChanged: "+error.getMessage() );
            }

            @Override
            public void onEvents(Player player, Player.Events events) {
                Player.Listener.super.onEvents(player, events);
                Log.e(TAG, "onEvents: "+events.toString() );
            }
        });
        MediaItem mediaItem = MediaItem.fromUri(url);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }

    private static final String TAG = "MainActivity";
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_loadurl) {
            loadVideo();
        }
    }
}