package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.widget.Toast;
import android.content.res.AssetFileDescriptor;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Handle the alarm event here
        // You can play a sound or perform any other action

        // Example: Play a sound
        playAlarmSound(context);
    }

    private void playAlarmSound(Context context) {
        try {
            MediaPlayer mediaPlayer = new MediaPlayer();

            // You can replace "alarm_sound.mp3" with the name of your audio file in the "assets" folder
            AssetFileDescriptor descriptor = context.getAssets().openFd("031974_30-seconds-alarm-72117.mp3");
            mediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            descriptor.close();

            mediaPlayer.prepare();
            mediaPlayer.setLooping(true); // Set to true if you want the sound to loop
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error playing alarm sound", Toast.LENGTH_SHORT).show();
        }
    }
}
