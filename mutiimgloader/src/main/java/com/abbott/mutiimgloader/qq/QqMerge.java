package com.abbott.mutiimgloader.qq;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.TypedValue;
import android.widget.ImageView;
import com.abbott.mutiimgloader.call.MergeCallBack;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jyb jyb_96@sina.com on 2017/9/14.
 * @version V1.0
 * @Description: add comment
 * @date 16-4-21 11:21
 * @copyright www.tops001.com
 */

public class QqMerge implements MergeCallBack {

    @Override
    public Bitmap merge(List<Bitmap> bitmapArray, Context context, ImageView imageView) {
        List<Bitmap> bitmaps = new ArrayList<>();
        for (Bitmap bitmap : bitmapArray) {
            if (!bitmap.isRecycled()) {
                bitmaps.add(bitmap);
            }
        }

        if (bitmaps.size() < 1 && bitmaps.size() > 9) {
            return null;
        }
        // 先取一个获取宽和高
        Bitmap tempBitmap =  bitmaps.get(0);
        if (tempBitmap == null) {
            return null;
        }
        // 画布的宽
        int tempWidth = imageView.getWidth();
        int tempHeight = imageView.getHeight();
        if (tempWidth == 0 || tempHeight == 0) {
            tempWidth = dip2px(context, 52);
            tempHeight = dip2px(context, 52);
        }
        Bitmap canvasBitmap = Bitmap.createBitmap(tempWidth, tempHeight,
                Bitmap.Config.ARGB_8888);
        Canvas localCanvas = new Canvas(canvasBitmap);
//        localCanvas.drawColor(Color.WHITE);
        JoinBitmaps.join(localCanvas, Math.min(tempWidth, tempHeight), bitmaps);
        return canvasBitmap;
    }

    @Override
    public String getMark() {
        return "qq@";
    }

    private  int dip2px(Context context, float value) {
        return (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                value, context.getResources().getDisplayMetrics()) + 0.5f);
    }
}
