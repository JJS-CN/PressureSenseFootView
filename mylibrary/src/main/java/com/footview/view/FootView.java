package com.footview.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
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

import com.footview.entity.FootValues;
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

    //脚掌压力数据,初始都为0
    private FootValues mLeftFootEntity = new FootValues() {
        @Override
        public float getValue(int position) {
            return 0;
        }
    };
    private FootValues mRightFootEntity = new FootValues() {
        @Override
        public float getValue(int position) {
            return 0;
        }
    };
    //绘制统一取值的entity
    private FootValues mDrawFootEntity;
    //保存动画执行期间，新进入数据的临时存储
    private FootValues mSnapLeftFootEntity;
    private FootValues mSnapRightFootEntity;
    //此次动画执行的目标数据
    private FootValues mAnimLeftFootEntity;
    private FootValues mAnimRightFootEntity;

    private float mAnimScale;//动画执行进度

    private SvgPathParser mSvgParser;//svg路径转换
    private float mSc;//基础缩放系数，缩放以适配view
    private float mFootWidth = 150;//半个脚掌的宽度 px
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

        private List<PathStepEntity> mFixedSvgPaths;//不需要缩放的Path集合
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
        StringsToPaths();
    }

    private void getDefaultPath() {

        if (mParams.mFixedSvgPaths != null && mParams.mFixedSvgPaths.size() == 0) {
            //不缩放Paths
            mParams.mFixedSvgPaths.add(new PathStepEntity(getResources().getString(R.string.foot_right_all)).isDefault());
            mParams.mFixedSvgPaths.add(new PathStepEntity(getResources().getString(R.string.foot_right_toeAll)).isDefault());
            mParams.mFixedSvgPaths.add(new PathStepEntity(getResources().getString(R.string.foot_right_foreAll)).isDefault());
            mParams.mFixedSvgPaths.add(new PathStepEntity(getResources().getString(R.string.foot_right_hindAll)).isDefault());
        }
        if (mParams.mScaleSvgPaths != null && mParams.mScaleSvgPaths.size() == 0) {
            //缩放Paths
            mParams.mScaleSvgPaths.add(new PathStepEntity(getResources().getString(R.string.foot_right_toe1), 44, 32).isDefault());
            mParams.mScaleSvgPaths.add(new PathStepEntity(getResources().getString(R.string.foot_right_toe2), 81.5f, 32.5f).isDefault());
            mParams.mScaleSvgPaths.add(new PathStepEntity(getResources().getString(R.string.foot_right_toe3), 104f, 39f).isDefault());
            mParams.mScaleSvgPaths.add(new PathStepEntity(getResources().getString(R.string.foot_right_toe4), 122f, 55f).isDefault());
            mParams.mScaleSvgPaths.add(new PathStepEntity(getResources().getString(R.string.foot_right_toe5), 133f, 73f).isDefault());
            mParams.mScaleSvgPaths.add(new PathStepEntity(getResources().getString(R.string.foot_right_foreBig), 108f, 121f).isDefault());
            mParams.mScaleSvgPaths.add(new PathStepEntity(getResources().getString(R.string.foot_right_foreSmall), 57f, 95f).isDefault());
            mParams.mScaleSvgPaths.add(new PathStepEntity(getResources().getString(R.string.foot_right_hind), 65f, 230f).isDefault());
        }

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
         * @param mFixedSvgEntitys svgString路径集合，不缩放集合
         */
        public Builder setFixedSvgEntitys(List<PathStepEntity> mFixedSvgEntitys) {
            mP.mFixedSvgPaths = mFixedSvgEntitys;
            return this;
        }

        public Builder addFixedSvgEntity(PathStepEntity fixedSvgEntity) {
            mP.mFixedSvgPaths.add(fixedSvgEntity);
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

        public Builder addScaleSvgEntity(PathStepEntity scaleSvgEntity) {
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
        StringsToPaths();
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

    /**
     * 尺寸测量
     *
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        //设置最小高度
        if (heightMode == MeasureSpec.AT_MOST) {
            float minH = mFootWidth / mFootHeight * width;
            setMeasuredDimension(width, (int) minH);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //需要再计算纵向缩放，取最小值。并将图像放置于中央
        //固定区域绘制
        if (mParams.mFixedSvgPaths != null) {
            if (mParams.mFixedSvgPaths.get(0).isDefault) {

                //由于镜像会移至左边，需要进行移动。（）
                canvas.translate(mFootWidth * mSc + (getWidth() - mFootWidth * mSc * 2) / 2, (getHeight() - mFootHeight * mSc) / 2);
                int layerId = canvas.save();
                //对画布进行镜像翻转
                canvas.scale(-1, 1, 0, 0);
                drawDefaultFixedFoot(canvas);
                canvas.restoreToCount(layerId);
                //重置回原有
                drawDefaultFixedFoot(canvas);
            } else {
                for (int i = 0; i < mParams.mFixedSvgPaths.size(); i++) {
                    drawPathForAll(canvas, mParams.mFixedSvgPaths.get(i).getPath());
                }
            }
        }
        //缩放区域绘制
        if (mParams.mScaleSvgPaths != null) {
            if (mParams.mScaleSvgPaths.get(0).isDefault) {
                //   canvas.translate(mFootWidth * mSc + (getWidth() - mFootWidth * mSc * 2) / 2, (getHeight() - mFootHeight * mSc) / 2);
                int layerId = canvas.save();
                //对画布进行镜像翻转
                canvas.scale(-1, 1, 0, 0);

                if (hasAnimation) {
                    mDrawFootEntity = new FootValues() {
                        @Override
                        public float getValue(int position) {
                            //在原压力数据基础下，计算动画差值
                            return mLeftFootEntity.getValue(position) + (mAnimLeftFootEntity.getValue(position) - mLeftFootEntity.getValue(position)) * mAnimScale;
                        }
                    };
                } else {
                    mDrawFootEntity = mLeftFootEntity;
                }
                drawDefaultScaleFoot(canvas, Gravity.LEFT);
                canvas.restoreToCount(layerId);
                //重置回原有
                if (hasAnimation) {
                    mDrawFootEntity = new FootValues() {
                        @Override
                        public float getValue(int position) {
                            //在原压力数据基础下，计算动画差值
                            return mRightFootEntity.getValue(position) + (mAnimRightFootEntity.getValue(position) - mRightFootEntity.getValue(position)) * mAnimScale;
                        }
                    };
                } else {
                    mDrawFootEntity = mRightFootEntity;
                }

                drawDefaultScaleFoot(canvas, Gravity.RIGHT);
            } else {
                for (int i = 0; i < mParams.mScaleSvgPaths.size(); i++) {
                    drawPathForAll(canvas, mParams.mScaleSvgPaths.get(i).getPath());
                }
            }
        }
    }

    /**************************** 绘制固定Path路径 Start *********************************/
    private void drawDefaultFixedFoot(Canvas canvas) {
        for (int i = 0; i < mParams.mFixedSvgPaths.size(); i++) {
            if (i == 0) {
                //绘制全脚掌
                Paint paint = getPaint();
                paint.setStrokeWidth(5);
                paint.setPathEffect(new DashPathEffect(mDashFootAll, 0));
                paint.setColor(Color.parseColor("#A4D2FC"));
                canvas.drawPath(mParams.mFixedSvgPaths.get(0).getPath(), paint);
            } else if (i == 1) {
                //脚趾区域
                drawPathForAll(canvas, mParams.mFixedSvgPaths.get(1).getPath());
            } else if (i == 2) {
                //前脚掌区域
                int layoutId = canvas.save();
                canvas.rotate(18, 75.921695f * mSc, 122.155177f * mSc);
                drawPathForAll(canvas, mParams.mFixedSvgPaths.get(2).getPath());
                canvas.restoreToCount(layoutId);
            } else if (i == 3) {
                //后脚跟区域
                drawPathForAll(canvas, mParams.mFixedSvgPaths.get(i).getPath());
            } else {
                drawPathForAll(canvas, mParams.mFixedSvgPaths.get(i).getPath());
            }
        }
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
    private void drawDefaultScaleFoot(Canvas canvas, int gravity) {
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
            canvas.scale(mScSize, mScSize, entity.getScX(), entity.getScY());
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

    /**************************** 绘制需要缩放Path路径 End *********************************/
    /**
     * 传入具体压力数值，返回压力等级
     */
    private float getStepSize(float size) {
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
    public void setFootEntity(FootValues leftFootEntity, FootValues rightFootEntity) {
        mSnapLeftFootEntity = leftFootEntity;
        mSnapRightFootEntity = rightFootEntity;
        requestInvalidate();
    }

    public void setLeftFootEntity(FootValues leftFootEntity) {
        mSnapLeftFootEntity = leftFootEntity;
        requestInvalidate();
    }

    public void setRightFootEntity(FootValues rightFootEntity) {
        mSnapRightFootEntity = rightFootEntity;
        requestInvalidate();
    }

    /**
     * 设置svg图形参数的总宽高，在初次启动时用于mSc的计算，务必在onResume之前设置
     *
     * @param svgWidth  svg总图形的宽
     * @param svgHeight svg总图形的高
     */
    public void setSvgPathSize(float svgWidth, float svgHeight) {
        mFootWidth = svgWidth;
        mFootHeight = svgHeight;
    }


    private void requestInvalidate() {
        //申请进行绘制，从中进行动画状态判断并拦截
        if (hasAnimation) {
            if (mAnimator == null) {
                mAnimLeftFootEntity = mSnapLeftFootEntity != null ? mSnapLeftFootEntity : mLeftFootEntity;
                mAnimRightFootEntity = mSnapRightFootEntity != null ? mSnapRightFootEntity : mRightFootEntity;
                mSnapLeftFootEntity = null;
                mSnapRightFootEntity = null;

                mAnimator = ValueAnimator.ofFloat(0f, 1f);
                mAnimator.setDuration(mAnimMilliSecond);
                mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        //动画过程中一直更新UI
                        mAnimScale = (float) animation.getAnimatedValue();
                        invalidate();
                    }
                });
                //mAnimator.setInterpolator();
                mAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        //结束时将初始数据替换为动画数据
                        mAnimator.cancel();
                        mAnimator = null;
                        mLeftFootEntity = mAnimLeftFootEntity;
                        mRightFootEntity = mAnimRightFootEntity;
                        //如果有snap数据，说明动画执行期间有新数据进入，再次执行动画
                        if (mSnapLeftFootEntity != null || mSnapRightFootEntity != null) {
                            requestInvalidate();
                        }
                    }
                });
                mAnimator.start();
            }
        } else {
            mLeftFootEntity = mSnapLeftFootEntity;
            mRightFootEntity = mSnapRightFootEntity;
            invalidate();
        }
    }

    private ValueAnimator mAnimator;//动画
    private boolean hasAnimation;//是否需要动画过渡
    private int mAnimMilliSecond;

    /**
     * 2次更新数据时是否需要动画过渡，在动画播放期间，设置的数据将不再执行
     *
     * @param hasAnimation    是否需要动画过渡
     * @param animMilliSecond 动画执行时间间隔
     */
    public void hasAnimation(boolean hasAnimation, int animMilliSecond) {
        this.hasAnimation = hasAnimation;
        this.mAnimMilliSecond = animMilliSecond;
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
        public boolean isDefault;

        public PathStepEntity(String svgString, float lastScX, float lastScY) {
            mSvgString = svgString;
            mLastScX = lastScX;
            mLastScY = lastScY;
        }

        public PathStepEntity(String svgString) {
            mSvgString = svgString;
        }

        public PathStepEntity isDefault() {
            this.isDefault = true;
            return this;
        }

        public void toPath(float mSc) {
            mScX = mLastScX * mSc;
            mScY = mLastScY * mSc;
            try {
                mPath = mSvgParser.parsePath(mSc, mSvgString);
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

    /**
     * 将svgString转换为path
     */
    private void StringsToPaths() {
        if (mSc <= 0)
            return;
        getDefaultPath();
        if (mParams.mFixedSvgPaths != null) {
            for (int i = 0; i < mParams.mFixedSvgPaths.size(); i++) {
                mParams.mFixedSvgPaths.get(i).toPath(mSc);
            }
        }
        if (mParams.mScaleSvgPaths != null) {
            for (int i = 0; i < mParams.mScaleSvgPaths.size(); i++) {
                mParams.mScaleSvgPaths.get(i).toPath(mSc);
            }
        }
    }


    /**
     * 进行path的提取并保存
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            float widthSc = getWidth() / (float) (mFootWidth * 2);
            float heightSc = getHeight() / mFootHeight;
            //获取msc
            mSc = Math.min(widthSc, heightSc);
            //执行转换
            StringsToPaths();
        }
    }


}
