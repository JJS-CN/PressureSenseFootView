package com.pressuresense.footview;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.SeekBar;

import com.footview.view.FootView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final FootView view = findViewById(R.id.foot);
        FootView.FootParams params = new FootView.Builder().setPressureStepSizes(10f,30f,50f,60f,100f)
                .setScStepSize(0.1f,0.2f,0.5f).setScStick(0.9f).setStepColors(Color.GREEN,Color.RED,Color.CYAN,Color.MAGENTA,Color.YELLOW)
                .build();
        view.setFootParams(params);
        SeekBar bar = findViewById(R.id.seek);
        bar.setMax(1000);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int pro, boolean fromUser) {
                float progress = pro / 10f;
                FootView.FootEntity entity = new FootView.FootEntity(progress, progress, progress, progress, progress, progress, progress, progress);
                FootView.FootEntity entity2 = new FootView.FootEntity(progress, progress, progress, progress, progress, progress, progress, progress);
                view.setFootEntity(entity, entity2);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
}
