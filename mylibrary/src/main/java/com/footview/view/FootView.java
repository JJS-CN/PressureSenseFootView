package com.footview.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.footview.utils.SvgPathParser;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import code.account.com.mylibrary.R;

/**
 * 说明：脚部压力显示，
 * 脚部将根据view自动进行缩放，也可在onLayout之后手动设置mSc参数
 * Created by jjs on 2018/8/31.
 */

public class FootView extends View {
    private float mPressureStepSize = 10f;//压力阶层，最小值为0，最大值为size*colors.size()
    private float mScStepSize = 0.2f;//缩放系数层级，每多一层最大轮廓会减小0.2f
    private float mScStick = 0.6f;//缩放粘性系数，总数值增大时，最大轮廓会相应放大，最大值为mScStepSize数值,有效区间为0f-1f
    private float mDefaultStep = 0;//默认展示的层级数
    private boolean hasDashOther = true;//是否需要边缘虚线


    //颜色数组
    private int[] mStepColors = new int[]{
            Color.parseColor("#D8D7FE"),
            Color.parseColor("#BCB9FD"),
            Color.parseColor("#7F86FF"),
            Color.parseColor("#3F51FF")
    };
    private float[] mDashFootAll = new float[]{15, 15};//全脚掌的虚线间隔
    private float[] mDashFootOther = new float[]{8, 4};//其他位置的虚线间隔


    private FootEntity mLeftFootEntity = new FootEntity();//脚掌压力数据,初始都为0
    private FootEntity mRightFootEntity = new FootEntity();
    private FootEntity mDrawFootEntity;//绘制统一取值的entity
    private List<PathStepEntity> mPathStepList;//需要缩放的path集合
    private List<Path> mPaths;//不需要缩放的Path集合

    private SvgPathParser mSvgParser;//svg路径转换
    private float mSc;//基础缩放系数，缩放以适配view
    private int mFootWidth = 150;//半个脚掌的宽度 px
    private float mFootHeight = 285f;//脚掌的高度 px

    public FootView(Context context) {
        super(context);
        init();
    }

    public FootView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FootView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        this.setBackgroundColor(Color.WHITE);
        mSvgParser = new SvgPathParser();
    }


    private Paint mPaint;

    private Paint getPaint() {
        if (mPaint == null) {
            mPaint = new Paint();
        } else {
            mPaint.reset();
        }
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        //mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeWidth(2);
        mPaint.setStyle(Paint.Style.STROKE);
        return mPaint;
    }


    @Override
    protected void onDraw(Canvas canvas) {


        //需要再计算纵向缩放，取最小值。并将图像放置于中央

        //由于镜像会移至左边，需要进行移动。（）
        canvas.translate(mFootWidth * mSc + (getWidth() - mFootWidth * mSc * 2) / 2, (getHeight() - mFootHeight * mSc) / 2);
        canvas.save();
        //对画布进行镜像翻转
        canvas.scale(-1, 1, 0, 0);
        mDrawFootEntity = mLeftFootEntity;
        drawFoot(canvas);
        canvas.restore();
        //重置回原有
        mDrawFootEntity = mRightFootEntity;
        drawFoot(canvas);

    }

    private void drawFoot(Canvas canvas) {
        //全脚掌区域
        drawFootAll(canvas, mPaths.get(0));
        //脚趾区域
        drawToeAll(canvas, mPaths.get(1));
        //前脚掌区域
        drawForeAll(canvas, mPaths.get(2));
        //后脚跟区域
        drawHindAll(canvas, mPaths.get(3));

        for (int i = 0; i < mPathStepList.size(); i++) {
            if (i == 5 || i == 6) {
                int layoutId = canvas.save();
                canvas.rotate(18, 75.921695f * mSc, 122.155177f * mSc);
                drawStep(canvas, mDrawFootEntity.getValue(i), mPathStepList.get(i));
                canvas.restoreToCount(layoutId);
            } else {
                drawStep(canvas, mDrawFootEntity.getValue(i), mPathStepList.get(i));
            }
        }
    }

    /**************************** 绘制固定Path路径 Start *********************************/
    private void drawFootAll(Canvas canvas, Path Path) {
        //绘制全脚掌
        Paint paint = getPaint();
        paint.setStrokeWidth(5);
        paint.setPathEffect(new DashPathEffect(mDashFootAll, 0));
        paint.setColor(Color.parseColor("#A4D2FC"));

        canvas.drawPath(Path, paint);
    }

    private void drawToeAll(Canvas canvas, Path path) {
        //绘制脚趾区域
        drawPathForAll(canvas, path);
    }

    private void drawForeAll(Canvas canvas, Path path) {
        int layoutId = canvas.save();
        canvas.rotate(18, 75.921695f * mSc, 122.155177f * mSc);
        drawPathForAll(canvas, path);
        canvas.restoreToCount(layoutId);
    }

    private void drawHindAll(Canvas canvas, Path path) {
        drawPathForAll(canvas, path);
    }

    private void drawPathForAll(Canvas canvas, Path path) {
        Paint paint = getPaint();
        paint.setColor(Color.parseColor("#E4E4FF"));

        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#F0F0FF"));
        canvas.drawPath(path, paint);
    }

    /**************************** 绘制固定Path路径 End *********************************/


    /**************************** 绘制需要缩放Path路径 Start *********************************/
    private void drawStep(Canvas canvas, float value, PathStepEntity entity) {
        //压力等级(精确值)
        float step = getStepSize(value);
        //压力等级(层级值/模糊)
        int size = (int) Math.ceil(step);
        for (int i = 0; i < size; i++) {
            float scSize = Math.min(1 - mScStepSize * i + mScStepSize * i * step * Math.max(Math.min(mScStick, 1f), 0f) / mStepColors.length, step - i == 0 ? 1 : step - i);

            int layoutId = canvas.save();
            canvas.scale(scSize, scSize, entity.mScX, entity.mScY);
            Paint paint = getPaint();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(mStepColors[i]);
            canvas.drawPath(entity.mPath, paint);
            if (hasDashOther) {
                paint = getPaint();
                paint.setStrokeWidth(1);
                paint.setPathEffect(new DashPathEffect(mDashFootOther, 0));
                paint.setColor(Color.parseColor("#C9C9D1"));
                canvas.drawPath(entity.mPath, paint);
            }
            canvas.restoreToCount(layoutId);
        }
    }

    private float getStepSize(float size) {
        //传入压力数值，返回压力等级
        float s = size / mPressureStepSize;
        return s < mDefaultStep ? mDefaultStep : s > mStepColors.length ? mStepColors.length : s;
    }

    /**
     * 设置左右脚压力数据
     */
    public void setFootEntity(FootEntity leftFootEntity, FootEntity rightFootEntity) {
        mLeftFootEntity = leftFootEntity != null ? leftFootEntity : new FootEntity();
        mRightFootEntity = rightFootEntity != null ? rightFootEntity : new FootEntity();
        invalidate();
    }

    /**
     * 脚掌压力数据
     */
    public static class FootEntity {
        public float mToe1, mToe2, mToe3, mToe4, mToe5;//脚趾1-5
        public float mForeBig, mForeSmall, mHind;//前脚掌大区域，前脚掌小区域，后脚跟区域

        public FootEntity() {

        }

        public FootEntity(float toe1, float toe2, float toe3, float toe4, float toe5, float foreBig, float foreSmall, float hind) {
            mToe1 = toe1;
            mToe2 = toe2;
            mToe3 = toe3;
            mToe4 = toe4;
            mToe5 = toe5;
            mForeBig = foreBig;
            mForeSmall = foreSmall;
            mHind = hind;
        }

        public float getValue(int position) {
            float value = 0;
            switch (position) {
                case 0:
                    value = mToe1;
                    break;
                case 1:
                    value = mToe2;
                    break;
                case 2:
                    value = mToe3;
                    break;
                case 3:
                    value = mToe4;
                    break;
                case 4:
                    value = mToe5;
                    break;
                case 5:
                    value = mForeBig;
                    break;
                case 6:
                    value = mForeSmall;
                    break;
                case 7:
                    value = mHind;
                    break;
            }
            return value;
        }
    }

    /**
     * 脚掌绘制路径
     */
    private class PathStepEntity {
        public Path mPath;//绘制路径
        public float mScX;//缩放中心点X
        public float mScY;//缩放中心点Y

        public PathStepEntity(Path path, float scX, float scY) {
            mPath = path;
            mScX = scX;
            mScY = scY;
        }
    }

    /**
     * 进行path的提取并保存
     */
    float widthSc;
    float heightSc;

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed && mPaths == null) {
            widthSc = getWidth() / (float) (mFootWidth * 2);//横向缩放
            heightSc = getHeight() / mFootHeight;
            mSc = Math.min(widthSc, heightSc);
            try {
                mPaths = new ArrayList<>();
                Path mRightAll = mSvgParser.parsePath(mSc, getResources().getString(R.string.foot_right_all));
                mPaths.add(mRightAll);
                Path mRightToeAll = mSvgParser.parsePath(mSc, getResources().getString(R.string.foot_right_toeAll));
                mPaths.add(mRightToeAll);
                Path mRightForeAll = mSvgParser.parsePath(mSc, getResources().getString(R.string.foot_right_foreAll));
                mPaths.add(mRightForeAll);
                Path mRightHindAll = mSvgParser.parsePath(mSc, getResources().getString(R.string.foot_right_hindAll));
                mPaths.add(mRightHindAll);

                mPathStepList = new ArrayList<>();
                Path mRightToe1 = mSvgParser.parsePath(mSc, getResources().getString(R.string.foot_right_toe1));
                mPathStepList.add(new PathStepEntity(mRightToe1, 44 * mSc, 32 * mSc));
                Path mRightToe2 = mSvgParser.parsePath(mSc, getResources().getString(R.string.foot_right_toe2));
                mPathStepList.add(new PathStepEntity(mRightToe2, 81.5f * mSc, 32.5f * mSc));
                Path mRightToe3 = mSvgParser.parsePath(mSc, getResources().getString(R.string.foot_right_toe3));
                mPathStepList.add(new PathStepEntity(mRightToe3, 104f * mSc, 39f * mSc));
                Path mRightToe4 = mSvgParser.parsePath(mSc, getResources().getString(R.string.foot_right_toe4));
                mPathStepList.add(new PathStepEntity(mRightToe4, 122f * mSc, 55f * mSc));
                Path mRightToe5 = mSvgParser.parsePath(mSc, getResources().getString(R.string.foot_right_toe5));
                mPathStepList.add(new PathStepEntity(mRightToe5, 133f * mSc, 73f * mSc));
                Path mRightForeBig = mSvgParser.parsePath(mSc, getResources().getString(R.string.foot_right_foreBig));
                mPathStepList.add(new PathStepEntity(mRightForeBig, 108f * mSc, 121f * mSc));
                Path mRightForeSmall = mSvgParser.parsePath(mSc, getResources().getString(R.string.foot_right_foreSmall));
                mPathStepList.add(new PathStepEntity(mRightForeSmall, 57f * mSc, 95f * mSc));
                Path mRightHind = mSvgParser.parsePath(mSc, getResources().getString(R.string.foot_right_hind));
                mPathStepList.add(new PathStepEntity(mRightHind, 65f * mSc, 230f * mSc));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }
}
