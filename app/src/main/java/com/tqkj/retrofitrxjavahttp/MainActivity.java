package com.tqkj.retrofitrxjavahttp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.tqkj.retrofitrxjavahttp.activity.WebActivity;
import com.tqkj.retrofitrxjavahttp.adapter.MainListAdapter;
import com.tqkj.retrofitrxjavahttp.bean.WangYiNewsBean;
import com.tqkj.retrofitrxjavahttp.lifecycle.MainLifecycle;
import com.tqkj.retrofitrxjavahttp.viewmodel.MainViewModel;

import java.util.List;


public class MainActivity extends AppCompatActivity {

    private RecyclerView rcvNews;
    private TextView tvTitle;
    private MainListAdapter adapter;
    private MainViewModel mainViewModel;
    private int page = 1;//首次为1

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvTitle = this.findViewById(R.id.page_title);
        rcvNews = this.findViewById(R.id.rclv_news_list);
        //绑定生命周期的监听
        getLifecycle().addObserver(new MainLifecycle(this));
        //获取viwemodel实例
        mainViewModel = new ViewModelProvider(this, new MainViewModel.Factory()).get(MainViewModel.class);

        rcvNews.setHasFixedSize(true);
        rcvNews.setNestedScrollingEnabled(false);
        rcvNews.setLayoutManager(new LinearLayoutManager(MainActivity.this, RecyclerView.VERTICAL, false));
        initData();
    }

    private void initData() {
        //观察数据的变化
        mainViewModel.getNews().observe(this, new Observer<List<WangYiNewsBean>>() {
            @Override
            public void onChanged(List<WangYiNewsBean> wangYiNewsBeans) {

                if (adapter == null) {
                    adapter = new MainListAdapter(wangYiNewsBeans);
                    adapter.setOnItemListener(new MainListAdapter.OnItemListener() {
                        @Override
                        public void onClick(RecyclerView.ViewHolder holder, WangYiNewsBean wangYiNew) {
                            //ToastUtils.showShort("点击了第" + holder.getAdapterPosition() + "行的" + wangYiNew.getTitle());
                            startActivity(new Intent(MainActivity.this, WebActivity.class)
                                    .putExtra("webUrl", wangYiNew.getPath()));
                        }
                    });
                    rcvNews.setAdapter(adapter);
                    tvTitle.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
//                            adapter.clear();//做页数的就清理了
                            page++;
                            mainViewModel.refreshNews(String.valueOf(page));//刷新数据
                        }
                    });
                } else {
                    adapter.addItems(wangYiNewsBeans);
                }
                ToastUtils.showShort("现在是第" + page + "页，总共已经为您推荐了" + wangYiNewsBeans.size()*page + "条新闻，每次刷新提供8条，请观赏");
            }
        });

    }

}