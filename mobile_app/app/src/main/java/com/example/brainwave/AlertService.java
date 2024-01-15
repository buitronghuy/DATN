package com.example.brainwave;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class AlertService extends IntentService {

    public static MediaPlayer player;
    public static MediaPlayer musicPlayer;
    private static int musicCount = 0;
    private static final int MAX_MUSIC_COUNT = 5;

    public AlertService() {
        super("AlertService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Bundle b = intent.getBundleExtra("Alert");
        boolean status = b.getBoolean("Status");
        musicCount = 0;
        if (status) {
            playHorn();
            Log.d("TAG", "Turn on alert! ");
        } else {
            stopPlayer();
        }
    }

    public void playHorn(){
        stopPlayer();
//        if(player == null) {
        player = MediaPlayer.create( this, R.raw.test_warninghorn_beta_isochronic_13hz_6min);
//        player = MediaPlayer.create( this, R.raw.warning_horn);
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
//                playMusic();
                player.start();
            }
        });
        player.start();
    }

    public void playMusic(){
        stopPlayer();
        musicPlayer = MediaPlayer.create(this, R.raw.betawaves14hz);
//        musicPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//            @Override
//            public void onCompletion(MediaPlayer mp) {
//                if (musicCount < MAX_MUSIC_COUNT) {
//                    musicCount++;
//                    musicPlayer.start();
//                } else {
//                    musicCount = 0;
//                    stopPlayer();
//                }
//            }
//        });
        musicPlayer.start();
    }

    private void stopPlayer() {
        if (player != null) {
            player.release();
            player = null;
//            Toast.makeText(this, "MediaPlayer released", Toast.LENGTH_SHORT).show();
        }
    }


}