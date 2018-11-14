package com.pressuresense.footview;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.footview.entity.FootDefaultEntity;
import com.footview.view.FootView;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final FootView view = findViewById(R.id.foot);
        FootView.FootParams params = new FootView.Builder().setPressureStepSizes(10f, 30f, 50f, 60f, 100f)
                .setScStepSize(0.1f, 0.2f, 0.5f).setScStick(0.9f).setStepColors(Color.GREEN, Color.RED, Color.CYAN, Color.MAGENTA, Color.YELLOW)
                .build();
        view.setFootParams(params);
        view.hasAnimation(true, 1000);
        //        CountDownTimer timer = new CountDownTimer(0,3000) {
        //            @Override
        //            public void onTick(long millisUntilFinished) {
        //
        //            }
        //
        //            @Override
        //            public void onFinish() {
        //
        //            }
        //        };
        final Random random = new Random();
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        FootDefaultEntity entity = new FootDefaultEntity(random.nextInt(100), random.nextInt(100), random.nextInt(100), random.nextInt(100), random.nextInt(100), random.nextInt(100), random.nextInt(100), random.nextInt(100));
                        FootDefaultEntity entity2 = new FootDefaultEntity(random.nextInt(100), random.nextInt(100), random.nextInt(100), random.nextInt(100), random.nextInt(100), random.nextInt(100), random.nextInt(100), random.nextInt(100));
                        view.setFootEntity(entity, entity2);
                    }
                });
            }
        };

        timer.schedule(task, 0, 1000);
       /*  SeekBar bar = findViewById(R.id.seek);
       bar.setMax(1000);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int pro, boolean fromUser) {
                float progress = pro / 10f;
                FootDefaultEntity entity = new FootDefaultEntity(progress, progress, progress, progress, progress, progress, progress, progress);
                FootDefaultEntity entity2 = new FootDefaultEntity(progress, progress, progress, progress, progress, progress, progress, progress);
                view.setFootEntity(entity, entity2);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });*/
    }
}
