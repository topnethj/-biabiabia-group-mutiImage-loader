package com.hj.app;

import android.app.Application;
import android.support.annotation.NonNull;
import android.widget.ImageView;
import com.abbott.mutiimgloader.call.MergeCallBack;
import com.abbott.mutiimgloader.qq.QqMerge;
import com.abbott.mutiimgloader.util.JImageLoader;
import java.util.List;

/**
 * Created by hj on 18/12/5.
 */

public class BaseApp extends Application {

    public static BaseApp mInstance;
    private JImageLoader imageLoader;
    private MergeCallBack mergeCallBack;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        imageLoader = new JImageLoader(getApplicationContext());
        imageLoader.configDefaultPic(R.drawable.uhead);
        mergeCallBack = new QqMerge();
        imageLoader.setTargetWH(200, 200);
    }

    public void loadImages(List<String> urls, @NonNull ImageView iv) {
        if (urls == null || urls.size() < 1) {
            return;
        }
        String newUrl = imageLoader.getNewUrlByList(urls, mergeCallBack.getMark());
        if (newUrl.equals(iv.getTag(JImageLoader.IMG_URL))) {
            return;
        } else {
            imageLoader.displayImages(urls, iv, mergeCallBack);
        }
    }
}
