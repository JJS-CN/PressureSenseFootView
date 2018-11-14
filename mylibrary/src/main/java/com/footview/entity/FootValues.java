package com.footview.entity;

/**
 * 说明：脚掌压力数据接口
 * Created by jjs on 2018/11/14.
 */

public interface FootValues {
    //缩放区域绘制时，会根据下标来获取对应的压力值
    float getValue(int position);
}
