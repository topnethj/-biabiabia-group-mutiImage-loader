package com.abbott.mutiimgloader.entity;

import android.view.View;
import android.widget.ImageView;
/**
 * @author jyb jyb_96@sina.com on 2017/9/8.
 * @version V1.0
 * @Description: add comment
 * @date 16-4-21 11:21
 * @copyright www.tops001.com
 */

public class Result {
    public String url;
    public ImageView imageView;
    public View joinView;

    public Result(String url, ImageView imageView) {
        this.url = url;
        this.imageView = imageView;
    }

    public Result(String url, View joinView) {
        this.url = url;
        this.joinView = joinView;
    }
}
