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
        FootView.FootParams params = new FootView.Builder()
                .setPressureStepSizes(10f, 30f, 50f, 60f, 100f)//设置压力各层级所需压力
                .setScStepSize(0.1f, 0.2f, 0.5f)//设置压力各层级内缩距离，相对于最大值
                .setScStick(0.9f)//设置每层的粘性缩放值
                .setStepColors(Color.GREEN, Color.RED, Color.CYAN, Color.MAGENTA, Color.YELLOW)//设置每层的颜色值（左右）
                //.setStepColors(new int[]{},new int[]{}) //分别设置左右脚的颜色值
                //.setFixedSvgEntitys() //设置不变的svg底图
                //.setScaleSvgEntitys() //设置缩放的svg路径，需要设置缩放中心点
                .build();
        //应用参数
        view.setFootParams(params);
        //设置需要数值变化动画，并设置变化时间
        view.hasAnimation(true, 1000);

        //设置svg总图形的大小，以svg数值相同
        //view.setSvgPathSize(0f,0f);

        //设置左右脚的压力数据
        //view.setFootEntity();
        //view.setLeftFootEntity();
        //view.setRightFootEntity();

        /** 以下为动画测试代码 */
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
