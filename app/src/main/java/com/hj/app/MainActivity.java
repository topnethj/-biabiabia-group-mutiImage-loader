package com.hj.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hj on 18/12/5.
 */

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.rv_test);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Map<Integer, List<String>> datas = new HashMap<>();
        List<String> urls1 = new ArrayList<>();
        urls1.add("https://3am-image.superwie.com/110197_1532787373_2403015392");
        List<String> urls2 = new ArrayList<>();
        urls2.add("https://3am-image.superwie.com/110197_1532787373_2403015392");
        urls2.add("https://3am-image.superwie.com/h_109943_server_15277538069525539417399");
        List<String> urls3 = new ArrayList<>();
        urls3.add("https://3am-image.superwie.com/110197_1532787373_2403015392");
        urls3.add("https://3am-image.superwie.com/h_109943_server_15277538069525539417399");
        urls3.add("https://3am-image.superwie.com/h_113421_server_15302842143979577408636");
        List<String> urls4 = new ArrayList<>();
        urls4.add("https://3am-image.superwie.com/110197_1532787373_2403015392");
        urls4.add("https://3am-image.superwie.com/h_109943_server_15277538069525539417399");
        urls4.add("https://3am-image.superwie.com/h_113421_server_15302842143979577408636");
        urls4.add("https://3am-image.superwie.com/h_113739_server_15307119554349055480184");
        datas.put(0, urls1);
        datas.put(1, urls2);
        datas.put(2, urls3);
        datas.put(3, urls4);

        MyAdapter myAdapter = new MyAdapter(datas);
        recyclerView.setAdapter(myAdapter);
    }
}
