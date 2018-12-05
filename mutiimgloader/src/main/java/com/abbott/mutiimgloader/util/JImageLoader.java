package com.abbott.mutiimgloader.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;
import com.abbott.mutiimgloader.R;
import com.abbott.mutiimgloader.cache.DiskLruCache;
import com.abbott.mutiimgloader.cache.LruCache;
import com.abbott.mutiimgloader.call.MergeCallBack;
import com.abbott.mutiimgloader.entity.Result;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author hj topnethj@163.com on 2018/12/5.
 * @version V2.0
 * @Description: optimize
 */

public class JImageLoader {
    public static final String Tag = "JImageLoader";
    public static final int IMG_URL = R.drawable.ic_launcher;
    private static final int MESSAGE_SEND_RESULT = 100;
    private static final int CPU_COUNT = 2;
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1; //corePoolSize为CPU数加1
    private static final int MAX_POOL_SIZE = 2 * CPU_COUNT + 1; //maxPoolSize为2倍的CPU数加1
    private static final long KEEP_ALIVE = 5L; //存活时间为5s
    private static final long DISK_CACHE_SIZE = 1024 * 1024 * 30;
    private static final int BUF_SIZE = 1024 * 8;
    private static final Executor threadPoolExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    public static ReentrantLock mLock = new ReentrantLock();

    private Context mContext;
    private int defaultId = R.drawable.ic_launcher_round;
    private int mTargetWidth = 200;
    private int mTargetHight = 200;
    private LruCache<String, Bitmap> mMemoryCache;
    private DiskLruCache mDiskLruCache;

    public JImageLoader(Context context) {
        mContext = context.getApplicationContext();
        //int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        //int cacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<String, Bitmap>();
        File diskCacheDir = getAppCacheDir(mContext, "images");
        if (!diskCacheDir.exists()) {
            diskCacheDir.mkdirs();
        }
        if (diskCacheDir.getUsableSpace() > DISK_CACHE_SIZE) {
            //剩余空间大于我们指定的磁盘缓存大小
            try {
                mDiskLruCache = DiskLruCache.open(diskCacheDir, 1, 1, DISK_CACHE_SIZE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void configDefaultPic(int defaultPic) {
        this.defaultId = defaultPic;
    }

    public static File getAppCacheDir(Context context, String dirName) {
        String cacheDirString;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                !Environment.isExternalStorageRemovable()) {
            if (context.getExternalCacheDir() != null) {
                cacheDirString = context.getExternalCacheDir().getPath();
            } else {
                cacheDirString = Environment.getExternalStorageDirectory().getPath() +
                        "/Android/data/" + context.getPackageName() + "/cache";
            }
        } else {
            cacheDirString = context.getCacheDir().getPath();
        }
        return new File(cacheDirString + File.separator + dirName);
    }

    //主线程加载图片
    private Handler mMainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            Result result = (Result) msg.obj;
            ImageView imageView = result.imageView;
            if (imageView != null) {
                String url = (String) imageView.getTag(IMG_URL);
                if (url.equals(result.url)) {
                    Bitmap bitmap = loadFromNative(result.url);
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    }
                } else {
                    LogUtil.d(Tag, "The url associated with imageView has changed");
                }
            }
        }
    };

    public void displayImage(final String url, final ImageView imageView) {
        imageView.setTag(IMG_URL, url);
        Bitmap bitmap = loadFromMemory(url);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            return;
        }

        //开启一个新的线程
        Runnable loadBitmapTask = new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = loadBitMap(url, mTargetWidth, mTargetHight);
                if (bitmap != null) {
                    Result result = new Result(url, imageView);
                    Message msg = mMainHandler.obtainMessage(MESSAGE_SEND_RESULT, result);
                    msg.sendToTarget();

                }
            }
        };

        threadPoolExecutor.execute(loadBitmapTask);
    }

    /**
     * 默认按100 100的方法来存储
     *
     * @param urls
     * @param imageView
     * @param mergeCallBack
     */
    public void displayImages(final List<String> urls, final ImageView imageView, final MergeCallBack mergeCallBack) {
        displayImages(urls, imageView, mergeCallBack, mTargetWidth, mTargetHight);
    }

    /**
     * @param urls
     * @param imageView
     * @param mergeCallBack
     * @param dstWidth      用于单个图像的压缩存储
     * @param dstHeight
     */
    public void displayImages(final List<String> urls, final ImageView imageView, final MergeCallBack mergeCallBack, final int dstWidth, final int dstHeight) {
        if (urls == null || urls.size() <= 0) {
            throw new IllegalArgumentException("url不能为空");
        }

        if (mergeCallBack == null) {
            throw new IllegalArgumentException("mergeCallBack 不能为空");
        }
        final String url = getNewUrlByList(urls, mergeCallBack.getMark());

        imageView.setTag(IMG_URL, url);

        Bitmap bitmap = loadFromNative(url);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            return;
        }

        imageView.setImageResource(defaultId);
        LogUtil.e(Tag, "displayImages this is from default");

        //开启一个新的线程，同步加载所有的图片。如果加载成功，则返回。
        Runnable loadBitmapTask = new Runnable() {
            @Override
            public void run() {
                ArrayList<Bitmap> bitmaps = loadBitMaps(urls, dstWidth, dstHeight);
                if (bitmaps != null && bitmaps.size() > 0) {
                    Result result;
                    if (mergeCallBack != null) {
                        Bitmap mergeBitmap = mergeCallBack.merge(bitmaps, mContext, imageView);
                        if (urls.size() == bitmaps.size()) {
                            try {
                                saveDru(url, mergeBitmap);
                                addToMemoryCache(getKeyFromUrl(url), mergeBitmap);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            LogUtil.e(Tag, "size change. so can not save");
                        }
                        LogUtil.e(Tag, "displayImages this is from Merge");
                        result = new Result(url, imageView);
                    } else {
                        result = new Result(url, imageView);
                    }

                    Message msg = mMainHandler.obtainMessage(MESSAGE_SEND_RESULT, result);
                    msg.sendToTarget();
                }
            }
        };

        threadPoolExecutor.execute(loadBitmapTask);
    }

    private Bitmap loadFromNative(String url) {
        Bitmap bitmap = loadFromMemory(url);
        if (bitmap != null) {
            LogUtil.e(Tag, "displayImages this is from Memory");
            return bitmap;
        }
        try {
            bitmap = loadFromDiskCache(url);
            if (bitmap != null) {
                LogUtil.e(Tag, "displayImages this is from Disk");
                return bitmap;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据数组构造新的url.这里需要区别为1的时候，不进行合并操作，导致直接出现原图。所以需要调整下规则。
     *
     * @param urls
     * @return
     */
    public String getNewUrlByList(List<String> urls, String mark) {
        StringBuilder sb = new StringBuilder();
        for (String url : urls) {
            sb.append(url + mark);
        }

        return sb.toString();
    }

    private String getKeyFromUrl(String url) {
        String key;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(url.getBytes());
            byte[] m = messageDigest.digest();
            return getString(m);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            key = String.valueOf(url.hashCode());
        }

        return key;
    }

    private static String getString(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            sb.append(b[i]);
        }
        return sb.toString();
    }

    /**
     * 同步加载
     *
     * @param url
     * @param dstWidth
     * @param dstHeight
     * @return
     */
    private Bitmap loadBitMap(String url, int dstWidth, int dstHeight) {
        Bitmap bitmap = loadFromMemory(url);
        if (bitmap != null) {
            LogUtil.e(Tag, "this is from Memory");
            return bitmap;
        }

        try {
            bitmap = loadFromDiskCache(url);
            if (bitmap != null) {
                LogUtil.e(Tag, "this is from Disk");
                return bitmap;
            }

            bitmap = loadFromNet(url, dstWidth, dstHeight);
            LogUtil.e(Tag, "this is from Net");
            if (bitmap == null) {
                LogUtil.e(Tag, "bitmap null network error");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bitmap;
    }


    private ArrayList<Bitmap> loadBitMaps(List<String> urls, int dstWidth, int dstHeight) {
        ArrayList<Bitmap> bitmaps = new ArrayList<>();
        for (String url : urls) {
            Bitmap bitmap = loadBitMap(url, dstWidth, dstHeight);
            if (bitmap != null) {
                bitmaps.add(bitmap);
            }
        }
        return bitmaps;
    }

    /**
     * 1、先加载到diskDruCache中
     * 2、再从diskDru中取出
     *
     * @param url
     * @param dstWidth
     * @param dstHeight
     * @return
     * @throws IOException
     */
    private Bitmap loadFromNet(String url, int dstWidth, int dstHeight) throws IOException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException("Do not load Bitmap in main thread.");
        }

        if (mDiskLruCache == null) {
            return null;
        }

        String key = getKeyFromUrl(url);

        Bitmap bitmap = loadFormNet(url, dstWidth, dstHeight);
        if (bitmap == null) {
            return null;
        }

        DiskLruCache.Editor editor = mDiskLruCache.edit(key);
        //如果这个key对应的缓存正在被编辑，则会返回null，因为DiskLruCache不允许同时编辑一个缓存对象。
        if (editor != null) {
            OutputStream outputStream = editor.newOutputStream(0);
            if (bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)) {
                editor.commit();
            } else {
                editor.abort();
            }
            mDiskLruCache.flush();
        }

        addToMemoryCache(key, bitmap);

        return loadFromNative(url);
    }

    /**
     * 从网络拿到bitmap
     *
     * @param
     * @return
     */
    private Bitmap loadFormNet(String urlString, int dstWidth, int dstHeight) {
        HttpURLConnection urlConnection = null;
        BufferedInputStream bis = null;
        Bitmap bitmap = null;
        InputStream is = null;
        FileOutputStream tmepFos = null;
        File tempCacheFile = null;
        try {
            File tempCacheDir = getAppCacheDir(mContext, "temp");
            if (!tempCacheDir.exists()) {
                tempCacheDir.mkdir();
            }
            tempCacheFile = new File(tempCacheDir, "jil_" + System.currentTimeMillis());
            tmepFos = new FileOutputStream(tempCacheFile);
            final URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            is = urlConnection.getInputStream();
            byte[] buffer = new byte[BUF_SIZE];
            int len;
            while ((len = is.read(buffer)) != -1) {
                tmepFos.write(buffer, 0, len);
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(tempCacheFile.getAbsolutePath(), options);
            options.inSampleSize = calSampleSize(options, dstWidth, dstHeight);
            options.inJustDecodeBounds = false;
            bitmap = BitmapFactory.decodeFile(tempCacheFile.getAbsolutePath(), options);
            return bitmap;
        } catch (Exception e) {
            //e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (is != null) {
                HttpUtils.close(is);
            }
            if (bis != null) {
                HttpUtils.close(bis);
            }
            if (tmepFos != null) {
                HttpUtils.close(tmepFos);
            }
            if (tempCacheFile != null && tempCacheFile.exists()) {
                tempCacheFile.delete();
            }
        }

        return bitmap;
    }

    /**
     * 将合并的图像更新到指定的索引中
     *
     * @param url
     * @param bitmap
     * @throws IOException
     */
    public void saveDru(String url, Bitmap bitmap) throws IOException {
        if (mDiskLruCache == null) {
            return;
        }
        String key = getKeyFromUrl(url);
        DiskLruCache.Editor editor = mDiskLruCache.edit(key);
        //如果这个key对应的缓存正在被编辑，则会返回null，因为DiskLruCache不允许同时编辑一个缓存对象。

        if (editor != null) {
            OutputStream outputStream = editor.newOutputStream(0);
            if (bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)) {
                editor.commit();
            } else {
                editor.abort();
            }
            mDiskLruCache.flush();
        }
    }

    /**
     * 从磁盘加载
     *
     * @param url
     * @return
     * @throws IOException
     */
    private Bitmap loadFromDiskCache(String url) throws IOException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.w("warn", "should not load bitmap in main thread");
        }
        if (mDiskLruCache == null) {
            return null;
        }
        Bitmap bitmap = null;
        String key = getKeyFromUrl(url);
        DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
        if (snapshot != null) {
            FileInputStream fileInputStream = (FileInputStream) snapshot.getInputStream(0);
            FileDescriptor fileDescriptor = fileInputStream.getFD();
            bitmap = decodeSampledBitmapFromFD(fileDescriptor);
            if (bitmap != null) {
                //加入缓存队列中
                addToMemoryCache(key, bitmap);
            }
        }
        return bitmap;
    }

    /**
     * 添加到内存缓存
     *
     * @param key
     * @param bitmap
     */
    private void addToMemoryCache(String key, Bitmap bitmap) {
        if (getFromMemoryCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        } else {
//            bitmap.recycle();
        }
    }

    private Bitmap getFromMemoryCache(String key) {
        return mMemoryCache.get(key);
    }

    private Bitmap decodeSampledBitmapFromFD(FileDescriptor fileDescriptor) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
        //calInSampleSize方法的实现请见“Android开发之高效加载Bitmap”这篇博文
        options.inSampleSize = calSampleSize(options, mTargetWidth/2, mTargetHight/2);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
    }

    public static int calSampleSize(BitmapFactory.Options options, int dstWidth, int dstHeight) {
        int rawWidth = options.outWidth;
        int rawHeight = options.outHeight;
        int inSampleSize = 1;
        if (rawWidth > dstWidth || rawHeight > dstHeight) {
            float ratioHeight = (float) rawHeight / dstHeight;
            float ratioWidth = (float) rawWidth / dstHeight;
            inSampleSize = (int) Math.min(ratioWidth, ratioHeight);
        }
        return inSampleSize;
    }

    private boolean getStreamFromUrl(String urlString, OutputStream outputStream) {
        HttpURLConnection urlConnection = null;
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;

        try {
            final URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            bis = new BufferedInputStream(urlConnection.getInputStream(), BUF_SIZE);
            bos = new BufferedOutputStream(outputStream);

            int byteRead;
            while ((byteRead = bis.read()) != -1) {
                bos.write(byteRead);
            }

            return true;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }

            HttpUtils.close(bis);
            HttpUtils.close(bos);
        }

        return false;
    }

    public void setTargetWH(int targetWidth, int targetHight) {
        mTargetWidth = targetWidth;
        mTargetHight = targetHight;
    }

    private Bitmap loadFromMemory(String url) {
        LogUtil.e(Tag, "this is from memory:key=" + getKeyFromUrl(url));
        return mMemoryCache.get(getKeyFromUrl(url));
    }

    public void clear() {
        if (mDiskLruCache != null) {
            try {
                mDiskLruCache.delete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (mMemoryCache != null) {
            mMemoryCache.clear();
        }
    }
}
