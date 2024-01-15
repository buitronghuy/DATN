package com.example.brainwave;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.media.MediaPlayer;

public class SleepActivity extends AppCompatActivity {

    public static int count_playbacks = 0;

    public static final int PLAYBACKS = 15;
    public static MediaPlayer player;

    ImageView play;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sleep_view);

        play = findViewById(R.id.play);
        play.setImageResource(R.drawable.baseline_play_circle_24);

        if(player != null) {
            if(player.isPlaying()){
                play.setImageResource(R.drawable.baseline_pause_circle_24);
            }
        }

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                count_playbacks = 0;
                if(player == null) {
                    player = MediaPlayer.create(SleepActivity.this, R.raw.test_delta_waves_4hz_3min_isochronic);
                    player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            if(count_playbacks >= PLAYBACKS) {
                                player.release();
                                player = null;
                                count_playbacks = 0;
                                play.setImageResource(R.drawable.baseline_play_circle_24);
                            } else {
                                count_playbacks++;
                                player.start();
                            }

                        }
                    });
                }
                if(player.isPlaying()){
                    player.pause();
                    play.setImageResource(R.drawable.baseline_play_circle_24);
                } else {
                    player.start();
                    play.setImageResource(R.drawable.baseline_pause_circle_24);
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}