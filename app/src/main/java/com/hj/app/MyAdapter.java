package com.hj.app;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import java.util.List;
import java.util.Map;

/**
 * Created by hj on 18/12/5.
 */

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

    Map<Integer, List<String>> mDatas;

    public MyAdapter(Map<Integer, List<String>> datas) {
        mDatas = datas;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group_image, parent, false);
        ViewHolder holder = new ViewHolder(itemView);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        BaseApp.mInstance.loadImages(mDatas.get(position), holder.iv_group);
    }

    @Override
    public int getItemCount() {
        return mDatas == null ? 0 : mDatas.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iv_group;

        public ViewHolder(View view) {
            super(view);
            iv_group = view.findViewById(R.id.iv_group);
        }
    }
}
