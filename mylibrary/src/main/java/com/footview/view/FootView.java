package com.footview.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
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


    private float[] mDashFootAll = new float[]{15, 15};//全脚掌的虚线间隔
    private float[] mDashFootOther = new float[]{8, 4};//其他位置的虚线间隔


    private FootEntity mLeftFootEntity = new FootEntity();//脚掌压力数据,初始都为0
    private FootEntity mRightFootEntity = new FootEntity();
    private FootEntity mDrawFootEntity;//绘制统一取值的entity


    private SvgPathParser mSvgParser;//svg路径转换
    private float mSc;//基础缩放系数，缩放以适配view
    private int mFootWidth = 150;//半个脚掌的宽度 px
    private float mFootHeight = 285f;//脚掌的高度 px

    private FootParams mParams;

    //参数集合，用于存放参数
    public static class FootParams {
        private FootParams() {
            mStepLeftColors = new int[]{
                    Color.parseColor("#D8D7FE"),
                    Color.parseColor("#BCB9FD"),
                    Color.parseColor("#7F86FF"),
                    Color.parseColor("#3F51FF")
            };
            mStepRightColors = new int[]{
                    Color.parseColor("#D8D7FE"),
                    Color.parseColor("#BCB9FD"),
                    Color.parseColor("#7F86FF"),
                    Color.parseColor("#3F51FF")
            };
            mPressureStepSizes = new float[]{20f, 40f, 60f, 80f, 100f};
            mScStepSizes = new float[]{0.2f};
            mScStick = new float[]{0.6f};
            mDefaultStep = 0;
            hasDashOther = true;

            mScaleSvgPaths = new ArrayList<>();
            mFixedSvgPaths = new ArrayList<>();
            mFixedSvgStrings = new ArrayList<>();
        }

        //左脚递进颜色值（由低到高）
        private int[] mStepLeftColors;
        //右脚递进颜色值（由低到高）
        private int[] mStepRightColors;
        //压力阶层，压力圈完全展开所需的压力值，最小值为0，
        //length请与color的length一致，最大值请小于设备最大值
        private float[] mPressureStepSizes;

        private float[] mScStepSizes;//缩放系数层级，每多一层最大轮廓会减小0.2f
        private float[] mScStick; //缩放粘性系数，总数值增大时，最大轮廓会相应放大，最大值为mScStepSize数值,有效区间为0f-1f
        private float mDefaultStep;//默认展示的层级数
        private boolean hasDashOther;//是否需要边缘虚线

        private List<PathStepEntity> mScaleSvgPaths;//需要缩放的path集合(处理与未处理共用一个集合，通过toPath方法进行处理)

        private List<Path> mFixedSvgPaths;//不需要缩放的Path集合
        private List<String> mFixedSvgStrings;//不需要缩放的Path集合(未处理临时存储的集合)
    }

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
        mSvgParser = new SvgPathParser();
        mParams = new FootParams();
        getDefaultPath();
    }

    private void getDefaultPath() {
        if (mParams.mFixedSvgStrings != null && mParams.mFixedSvgStrings.size() == 0) {
            //不缩放Paths
            mParams.mFixedSvgStrings = new ArrayList<>();
            mParams.mFixedSvgStrings.add(getResources().getString(R.string.foot_right_all));
            mParams.mFixedSvgStrings.add(getResources().getString(R.string.foot_right_toeAll));
            mParams.mFixedSvgStrings.add(getResources().getString(R.string.foot_right_foreAll));
            mParams.mFixedSvgStrings.add(getResources().getString(R.string.foot_right_hindAll));
        }
        if (mParams.mScaleSvgPaths != null && mParams.mScaleSvgPaths.size() == 0) {
            //缩放Paths
            mParams.mScaleSvgPaths = new ArrayList<>();
            mParams.mScaleSvgPaths.add(new PathStepEntity(getResources().getString(R.string.foot_right_toe1), 44 * mSc, 32 * mSc));
            mParams.mScaleSvgPaths.add(new PathStepEntity(getResources().getString(R.string.foot_right_toe2), 81.5f * mSc, 32.5f * mSc));
            mParams.mScaleSvgPaths.add(new PathStepEntity(getResources().getString(R.string.foot_right_toe3), 104f * mSc, 39f * mSc));
            mParams.mScaleSvgPaths.add(new PathStepEntity(getResources().getString(R.string.foot_right_toe4), 122f * mSc, 55f * mSc));
            mParams.mScaleSvgPaths.add(new PathStepEntity(getResources().getString(R.string.foot_right_toe5), 133f * mSc, 73f * mSc));
            mParams.mScaleSvgPaths.add(new PathStepEntity(getResources().getString(R.string.foot_right_foreBig), 108f * mSc, 121f * mSc));
            mParams.mScaleSvgPaths.add(new PathStepEntity(getResources().getString(R.string.foot_right_foreSmall), 57f * mSc, 95f * mSc));
            mParams.mScaleSvgPaths.add(new PathStepEntity(getResources().getString(R.string.foot_right_hind), 65f * mSc, 230f * mSc));
        }
        StringsToPaths();
    }


    /**
     * 用于构造固定的绘制参数
     */
    public static final class Builder {
        FootParams mP;

        public Builder() {
            mP = new FootParams();
        }

        public Builder(FootParams footParams) {
            mP = footParams != null ? footParams : new FootParams();
        }

        /**
         * 设置每一级压力圈变化完全所需要的压力值
         *
         * @param pressureStepSizes 压力阈值数组，size请与color的size一致，最大值请小于设备最大值
         */
        public Builder setPressureStepSizes(@FloatRange(from = 0) float... pressureStepSizes) {
            if (pressureStepSizes != null && pressureStepSizes.length > 0) {
                mP.mPressureStepSizes = pressureStepSizes;
            }
            return this;
        }

        /**
         * 设置每一级压力圈的颜色,不够时循环使用
         *
         * @param stepColors 同时设置左右两只脚
         */
        public Builder setStepColors(@ColorInt int... stepColors) {
            if (stepColors != null && stepColors.length > 0) {
                mP.mStepLeftColors = stepColors;
                mP.mStepRightColors = stepColors;
            }
            return this;
        }

        /**
         * 设置每一级压力圈的颜色,不够时循环使用
         *
         * @param stepLeftColors  设置左脚
         * @param stepRightColors 设置右脚
         */
        public Builder setStepColors(@ColorRes int[] stepLeftColors, @ColorRes int[] stepRightColors) {
            if (stepLeftColors != null && stepLeftColors.length > 0) {
                mP.mStepLeftColors = stepLeftColors;
            }
            if (stepRightColors != null && stepRightColors.length > 0) {
                mP.mStepRightColors = stepRightColors;
            }
            return this;
        }

        /**
         * 设置每一个压力圈的内缩比例，不够时重复使用最后一个
         *
         * @param scStepSize 每一个压力圈的内缩边界，相对于未缩放尺寸(总数请控制在1f之内，并递进增加)
         */
        public Builder setScStepSize(@FloatRange(from = 0f, to = 1f) float... scStepSize) {
            if (scStepSize != null && scStepSize.length > 0) {
                mP.mScStepSizes = scStepSize;
            }
            return this;
        }

        /**
         * 设置每一个压力圈，随着总压力变化而变化的粘性放大系数
         *
         * @param scStick
         */
        public Builder setScStick(@FloatRange(from = 0f, to = 1f) float... scStick) {
            if (scStick != null && scStick.length > 0) {
                mP.mScStick = scStick;
            }
            return this;
        }

        /**
         * 设置不缩放的path路径集合
         *
         * @param mFixedSvgStrings svgString路径集合，不缩放集合
         */
        public Builder setFixedSvgStrings(List<String> mFixedSvgStrings) {
            mP.mFixedSvgStrings = mFixedSvgStrings;
            return this;
        }

        public Builder addFixedSvgString(String fixedSvgString) {
            mP.mFixedSvgStrings.add(fixedSvgString);
            return this;
        }

        /**
         * 设置需缩放的path路径集合
         *
         * @param mScaleSvgEntitys svgString路径集合，需缩放集合
         */
        public Builder setScaleSvgEntitys(List<PathStepEntity> mScaleSvgEntitys) {
            mP.mScaleSvgPaths = mScaleSvgEntitys;
            return this;
        }

        public Builder addScaleSvgString(PathStepEntity scaleSvgEntity) {
            mP.mScaleSvgPaths.add(scaleSvgEntity);
            return this;
        }

        /**
         * 创建Params,再赋值给footView
         */
        public FootParams build() {
            float sc = 0f;
            //缩放参数容错--数量不齐时使用最后一个+0.1f缩放值
            float[] stepSizes = new float[mP.mPressureStepSizes.length];
            for (int i = 0; i < mP.mPressureStepSizes.length; i++) {
                if (i < mP.mScStepSizes.length) {
                    stepSizes[i] = mP.mScStepSizes[i];
                } else {
                    stepSizes[i] = stepSizes[i - 1] + 0.1f;
                }
            }
            //赋值新数据
            mP.mScStepSizes = stepSizes;
            //计算总缩放比例
            for (int i = 0; i < mP.mPressureStepSizes.length; i++) {
                sc += mP.mScStepSizes[i < mP.mScStepSizes.length ? i : mP.mScStepSizes.length - 1];
            }
            if (sc > 1f) {
                //总缩放比例超出最大值1f,整体比例需要缩小
                float s = 1f / sc;//每一份按比例进行减小值
                for (int i = 0; i < mP.mScStepSizes.length; i++) {
                    //重赋值
                    mP.mScStepSizes[i] = mP.mScStepSizes[i] * s;
                }
            }
            return mP;
        }
    }

    /**
     * 设置除数据外的其他绘制参数，通过Builder方式构造
     */
    public void setFootParams(FootParams params) {
        mParams = params;
        getDefaultPath();
        invalidate();
    }

    /**
     * 获取绘制参数集合
     */
    public FootParams getFootParams() {
        return mParams;
    }

    private Paint mPaint;

    /**
     * 获取画笔
     */
    private Paint getPaint() {
        if (mPaint == null) {
            mPaint = new Paint();
        } else {
            //重置
            mPaint.reset();
        }
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeWidth(2);
        mPaint.setStyle(Paint.Style.STROKE);
        return mPaint;
    }


    @Override
    protected void onDraw(Canvas canvas) {

        //需要再计算纵向缩放，取最小值。并将图像放置于中央
        //
        //由于镜像会移至左边，需要进行移动。（）
        canvas.translate(mFootWidth * mSc + (getWidth() - mFootWidth * mSc * 2) / 2, (getHeight() - mFootHeight * mSc) / 2);
        canvas.save();
        //对画布进行镜像翻转
        canvas.scale(-1, 1, 0, 0);
        mDrawFootEntity = mLeftFootEntity;
        drawFoot(canvas, Gravity.LEFT);
        canvas.restore();
        //重置回原有
        mDrawFootEntity = mRightFootEntity;
        drawFoot(canvas, Gravity.RIGHT);

    }

    private void drawFoot(Canvas canvas, int gravity) {
        //全脚掌区域
        drawFootAll(canvas, mParams.mFixedSvgPaths.get(0));
        //脚趾区域
        drawToeAll(canvas, mParams.mFixedSvgPaths.get(1));
        //前脚掌区域
        drawForeAll(canvas, mParams.mFixedSvgPaths.get(2));
        //后脚跟区域
        drawHindAll(canvas, mParams.mFixedSvgPaths.get(3));

        for (int i = 0; i < mParams.mScaleSvgPaths.size(); i++) {
            if (i == 5 || i == 6) {
                //由于我拿到的path路径有点问题，进行旋转适配
                //所以和UI MM打好关系很重要
                int layoutId = canvas.save();
                canvas.rotate(18, 75.921695f * mSc, 122.155177f * mSc);
                drawStep(canvas, mDrawFootEntity.getValue(i), mParams.mScaleSvgPaths.get(i), gravity);
                canvas.restoreToCount(layoutId);
            } else {
                drawStep(canvas, mDrawFootEntity.getValue(i), mParams.mScaleSvgPaths.get(i), gravity);
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
    private void drawStep(Canvas canvas, float value, PathStepEntity entity, int gravity) {
        //压力等级(精确值)
        float step = getStepSize(value);
        //压力等级(层级值/模糊)
        int size = (int) Math.ceil(step);
        float mLastScSize = 1;
        for (int i = 0; i < size; i++) {
            //根据压力值计算缩放比例，考虑每层的最外圈缩放，和粘性缩放
            //外圈缩放参数不足是重复使用最后一个
            float lineSc = 1 - mParams.mScStepSizes[i < mParams.mScStepSizes.length ? i : mParams.mScStepSizes.length - 1];//当前的缩放值

            //计算外圈最大值
            float mScSize = lineSc * (step - i <= 0 || step - i > 1 ? 1 : step - i);
            //计算粘性缩放系数
            float stickSize = mParams.mScStick[i < mParams.mScStick.length ? i : mParams.mScStick.length - 1] * (mLastScSize - lineSc) * ((step - i) / (mParams.mScStepSizes.length - i));
            mLastScSize = lineSc;
            mScSize += stickSize;
            //保存画布
            int layoutId = canvas.save();
            //根据设置好的缩放中心点，进行比例缩放
            canvas.scale(mScSize, mScSize, entity.mScX, entity.mScY);
            Paint paint = getPaint();
            paint.setStyle(Paint.Style.FILL);
            //设置颜色并绘制
            paint.setColor(gravity == Gravity.LEFT ? mParams.mStepLeftColors[i % mParams.mStepLeftColors.length] : mParams.mStepRightColors[i % mParams.mStepRightColors.length]);
            canvas.drawPath(entity.mPath, paint);
            if (mParams.hasDashOther) {
                //绘制边线
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
        float s = 0;
        for (int i = 0; i < mParams.mPressureStepSizes.length; i++) {
            if (size < mParams.mPressureStepSizes[i]) {
                //如果压力在规定范围之内-正常判断
                if (i == 0) {
                    s = size / mParams.mPressureStepSizes[i];
                } else {
                    //由于是设定绝对值，所以要计算区间差值
                    s = i + ((size - mParams.mPressureStepSizes[i - 1]) / (mParams.mPressureStepSizes[i] - mParams.mPressureStepSizes[i - 1]));
                }
                break;
            } else {
                //否则使用最大层级
                s = mParams.mPressureStepSizes.length;
            }
        }
        return s < mParams.mDefaultStep ? mParams.mDefaultStep : s;
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
        private String mSvgString;//绘制路径

        private Path mPath;//绘制路径
        private float mScX;//缩放中心点X
        private float mScY;//缩放中心点Y

        private float mLastScX;
        private float mLastScY;
        private SvgPathParser mParser;

        public PathStepEntity(String svgString, float lastScX, float lastScY) {
            mSvgString = svgString;
            mLastScX = lastScX;
            mLastScY = lastScY;
            mParser = new SvgPathParser();
        }

        public void toPath(float mSc) {
            mScX = mLastScX * mSc;
            mScY = mLastScY * mSc;
            try {
                mPath = mParser.parsePath(mSc, mSvgString);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        public Path getPath() {
            return mPath;
        }

        public float getScX() {
            return mScX;
        }

        public float getScY() {
            return mScY;
        }
    }

    private void StringsToPaths() {
        if (mSc == 0)
            return;
        try {
            mParams.mFixedSvgPaths.clear();
            if (mParams.mFixedSvgStrings != null) {
                for (int i = 0; i < mParams.mFixedSvgStrings.size(); i++) {
                    mParams.mFixedSvgPaths.add(mSvgParser.parsePath(mSc, mParams.mFixedSvgStrings.get(i)));
                }
            }
            if (mParams.mScaleSvgPaths != null) {
                for (int i = 0; i < mParams.mScaleSvgPaths.size(); i++) {
                    mParams.mScaleSvgPaths.get(i).toPath(mSc);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }


    /**
     * 进行path的提取并保存
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed && mParams.mFixedSvgPaths == null) {
            float widthSc = getWidth() / (float) (mFootWidth * 2);
            float heightSc = getHeight() / mFootHeight;
            //获取msc
            mSc = Math.min(widthSc, heightSc);
            //执行转换
            StringsToPaths();
        }
    }


}
