package com.tqkj.retrofitrxjavahttp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.tqkj.retrofitrxjavahttp.activity.WebActivity;
import com.tqkj.retrofitrxjavahttp.adapter.MainListAdapter;
import com.tqkj.retrofitrxjavahttp.bean.WangYiNewsBean;
import com.tqkj.retrofitrxjavahttp.http.BaseObserver;
import com.tqkj.retrofitrxjavahttp.http.BaseRequest;
import com.tqkj.retrofitrxjavahttp.http.BaseResponse;

import java.util.List;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rcvNews;
    private TextView tvTitle;
    private MainListAdapter adapter;

    //生物识别（指纹识别）相关
    private Executor executor;//并发
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvTitle = this.findViewById(R.id.page_title);
        rcvNews = this.findViewById(R.id.rclv_news_list);
        rcvNews.setHasFixedSize(true);
        rcvNews.setNestedScrollingEnabled(false);
        rcvNews.setLayoutManager(new LinearLayoutManager(MainActivity.this, RecyclerView.VERTICAL, false));
        getNews();
        initBiometricManager();
    }

    /**
     * 验证设备是否可用生物识别等类似指纹识别的功能
     */
    private void initBiometricManager() {
        //这必须导入的是androidx的包，别和api是29那个包混淆了
        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate()) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                createBiometric();
                Log.d("MY_APP_TAG", "App can authenticate using biometrics.");
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Log.e("MY_APP_TAG", "No biometric features available on this device.");
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Log.e("MY_APP_TAG", "Biometric features are currently unavailable.");
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Log.e("MY_APP_TAG", "The user hasn't associated " +
                        "any biometric credentials with their account.");
                break;
        }


    }

    /**
     * 当系统可用生物指纹识别功能后执行
     * 改指纹识别功能暂时没有用到加密功能
     */
    private void createBiometric() {
        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(MainActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                ToastUtils.showShort("Authentication error: " + errString);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                ToastUtils.showShort("Authentication succeeded: " + result);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                ToastUtils.showShort("Authentication failed: ");
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("触摸解锁验证登录")
                .setSubtitle("使用你的系统指纹进行身份验证")
                .setNegativeButtonText("替代你的密码验证")
                .build();
    }

    /**
     * 获取网易新闻
     */
    private void getNews() {

        BaseRequest.getInstance()
                .getApiService()
                .getNews("1", "50")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new BaseObserver<List<WangYiNewsBean>>(this) {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onSuccess(BaseResponse<List<WangYiNewsBean>> baseResponse) {
                        //成功回调方法,可以直接在此更新ui,AndroidSchedulers.mainThread()表示切换到主线程
                        if (baseResponse.isSuccess()) {
                            LogUtils.d(baseResponse.getMessage());
                            LogUtils.json(baseResponse.getResults());
                            List<WangYiNewsBean> newsList = baseResponse.getResults();
                            if (newsList != null && !newsList.isEmpty()) {
                                if (adapter == null) {
                                    adapter = new MainListAdapter(newsList);
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
                                            adapter.clear();
                                            getNews();
                                            /**
                                             * 点击跳出指纹识别界面
                                             */
                                            biometricPrompt.authenticate(promptInfo);

                                        }
                                    });
                                } else {
                                    adapter.updateAll(newsList);
                                    LogUtils.d("直接添加了");
                                }
                                ToastUtils.showShort("已经为您推荐" + newsList.size() + "条新闻，请观赏");
                            }
                        } else {
                            ToastUtils.showShort("失败了");
                        }
                    }

                    @Override
                    public void onCodeError(BaseResponse baseResponse) {
                        LogUtils.d(baseResponse.getMessage());
                        //失败回调方法,可以直接在此更新ui,AndroidSchedulers.mainThread()表示切换到主线程

                    }

                    @Override
                    public void onFailure(Throwable e, boolean network) throws Exception {
                        LogUtils.d(e.getMessage());
                    }
                });
    }


}