package com.abbott.mutiimgloader.cache;

import android.graphics.Bitmap;

import com.abbott.mutiimgloader.util.LogUtil;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author jyb jyb_96@sina.com on 2017/9/13.
 * @version V1.0
 * @Description: add comment
 * @date 16-4-21 11:21
 * @copyright www.tops001.com
 */

public class LruCache<K, V> {

    private final LinkedHashMap<K, V> map;
    private final int MAX_CACHE_SIZE = 100;
    private final float DEFAULT_LOAD_FACTOR = 0.75f;

    public LruCache() {
        int capacity = (int) Math.ceil(MAX_CACHE_SIZE / DEFAULT_LOAD_FACTOR) + 1;
        map = new LinkedHashMap<K, V>(capacity, DEFAULT_LOAD_FACTOR, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                boolean isRemove = size() > MAX_CACHE_SIZE;
                if (isRemove) {
                    //((Bitmap)(eldest.getValue())).recycle();
                    LogUtil.d("JImageLoader", (String)eldest.getKey());
                }
                return isRemove;
            }
        };
    }

    public synchronized void put(K key, V value) {
        map.put(key, value);
    }

    public synchronized V get(K key) {
        return map.get(key);
    }

    public synchronized void remove(K key) {
        map.remove(key);
    }

    public synchronized Set<Map.Entry<K, V>> getAll() {
        return map.entrySet();
    }

    public synchronized int size() {
        return map.size();
    }

    public synchronized void clear() {
        map.clear();
    }

    public void recycleBitmap(Bitmap bitmap) {
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry entry : map.entrySet()) {
            sb.append(String.format("%s:%s ", entry.getKey(), entry.getValue()));
        }
        return sb.toString();
    }

}
