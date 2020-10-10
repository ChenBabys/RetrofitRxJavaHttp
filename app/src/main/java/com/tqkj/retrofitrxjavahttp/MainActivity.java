package com.tqkj.retrofitrxjavahttp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.util.ToastUtils;
import com.tqkj.retrofitrxjavahttp.activity.WebActivity;
import com.tqkj.retrofitrxjavahttp.adapter.MainListAdapter;
import com.tqkj.retrofitrxjavahttp.bean.WangYiNewsBean;
import com.tqkj.retrofitrxjavahttp.lifecycle.MainLifecycle;
import com.tqkj.retrofitrxjavahttp.nfc.NfcHandler;
import com.tqkj.retrofitrxjavahttp.nfc.NfcView;
import com.tqkj.retrofitrxjavahttp.viewmodel.MainViewModel;

import java.util.List;


public class MainActivity extends AppCompatActivity implements NfcView {

    private RecyclerView rcvNews;
    private TextView tvTitle;
    private MainListAdapter adapter;
    private MainViewModel mainViewModel;
    private NfcHandler nfcHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvTitle = this.findViewById(R.id.page_title);
        rcvNews = this.findViewById(R.id.rclv_news_list);
        //绑定生命周期的监听
        getLifecycle().addObserver(new MainLifecycle(this));

        //获取viwemodel实例，必须加入第二个构造参数。否则没有对应的构造方法无法传入参数给viewmodel.
        //但是有时候遇到不用传递参数给viewmodel的情况，第二个参数就不必要了，所以为了兼容这种情况，提高代码的可读性，
        // 所以如果想有只传入this的话，可以引入extensions2.2.0包，即在gradle中：加入 'androidx.lifecycle:lifecycle-extensions:2.2.0'

        //mainViewModel = new ViewModelProvider(this, new MainViewModel.Factory()).get(MainViewModel.class);

        //所以就可以写成这种方式了，有参用上面的，无参数就用下面的，所以说参数不用这里传递的也用下面这个
        // ,用这个方法的同时，MainViewModel那边的构造函数也要去掉才行，并且那边的new MainModel(),也不要放在Factory构造函数里面new了
        // ，直接在MainViewModel构造函数里面new就行，并删除MainViewModel构造函数中的参数MainModel。
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);


        rcvNews.setHasFixedSize(true);
        rcvNews.setNestedScrollingEnabled(false);
        rcvNews.setLayoutManager(new LinearLayoutManager(MainActivity.this, RecyclerView.VERTICAL, false));
        initData();

        //Nfc相关初始化
        nfcHandler = new NfcHandler(this);
        nfcHandler.init(this);
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
                            mainViewModel.page++;
                            mainViewModel.refreshNews(String.valueOf(mainViewModel.page));//刷新数据
                        }
                    });
                } else {
                    adapter.addItems(wangYiNewsBeans);
                }
                ToastUtils.showShort("现在是第" + mainViewModel.page + "页，总共已经为您推荐了" + wangYiNewsBeans.size() * mainViewModel.page + "条新闻，每次刷新提供8条，请观赏");
            }
        });

        //观察时间，系统更新就更新
        mainViewModel.getTimeData().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String time) {
                tvTitle.setText(time);
            }
        });


    }

    /**
     * 可要可不要
     *
     * @param intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }


    @Override
    protected void onResume() {
        super.onResume();
        nfcHandler.enableNfc(this);
        //直接在准备好的时候就读卡
        nfcHandler.readCardId(getIntent());
    }


    @Override
    protected void onPause() {
        super.onPause();
        nfcHandler.disableNfc(this);
    }

    /**
     * 获取到的卡号或者id
     *
     * @param response
     */
    @Override
    public void appendResponse(String response) {
        Toast.makeText(this, response, Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        nfcHandler.onDestroy();
    }
}