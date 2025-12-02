package com.example.videoplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    String url = "https://filesamples.com/samples/video/f4v/sample_640x360.f4v";
    String url2 = "https://filesamples.com/samples/video/f4v/sample_960x400_ocean_with_audio.f4v";
    TextView tvLoadUrl;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLoadUrl = findViewById(R.id.tv_loadurl);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_loadurl) {

        }
    }
}