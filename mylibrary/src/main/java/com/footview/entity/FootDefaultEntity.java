package com.footview.entity;

/**
 * 说明：自定义数据保存类，脚掌压力数据
 * Created by jjs on 2018/11/14.
 */

public class FootDefaultEntity implements FootValues {
    public float mToe1, mToe2, mToe3, mToe4, mToe5;//脚趾1-5
    public float mForeBig, mForeSmall, mHind;//前脚掌大区域，前脚掌小区域，后脚跟区域

    public FootDefaultEntity(float toe1, float toe2, float toe3, float toe4, float toe5, float foreBig, float foreSmall, float hind) {
        mToe1 = toe1;
        mToe2 = toe2;
        mToe3 = toe3;
        mToe4 = toe4;
        mToe5 = toe5;
        mForeBig = foreBig;
        mForeSmall = foreSmall;
        mHind = hind;
    }

    @Override
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
