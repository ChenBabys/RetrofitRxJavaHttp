package com.tqkj.retrofitrxjavahttp.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.tqkj.retrofitrxjavahttp.bean.WangYiNewsBean;
import com.tqkj.retrofitrxjavahttp.http.BaseObserver;
import com.tqkj.retrofitrxjavahttp.http.BaseRequest;
import com.tqkj.retrofitrxjavahttp.http.BaseResponse;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * author : CYS
 * e-mail : 1584935420@qq.com
 * date : 2020/10/6 1:09
 * desc : 请求网络，page是页码，count是单次请求回来的新闻条数
 * version : 1.0
 */
public class MainModel {
    /**
     * 这里必须保证mutableLiveData的唯一，他的初始化不要放在方法中。直接在这里初始化或者构造函数初始化是最好的。
     * 否则会出现空指针异常
     */
    private MutableLiveData<List<WangYiNewsBean>> mutableLiveData = new MutableLiveData<>();
    private MutableLiveData<String> timeLiveData = new MutableLiveData<>();


    /**
     * 获取网易新闻
     */
    public LiveData<List<WangYiNewsBean>> getNews(String page) {

        BaseRequest.getInstance()
                .getApiService()
                .getNews(page, "8")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())//主线程
                .subscribe(new BaseObserver<List<WangYiNewsBean>>() {
                    @Override
                    public void onSuccess(BaseResponse<List<WangYiNewsBean>> baseResponse) {
                        //成功回调方法,可以直接在此更新ui,AndroidSchedulers.mainThread()表示切换到主线程
                        if (baseResponse.isSuccess()) {
                            List<WangYiNewsBean> newsList = baseResponse.getResults();
                            if (newsList != null && !newsList.isEmpty())
                                //如果不是在主线程就要用postValue，但是这里是主线程来的，所以用setValue一样可以的
                                //这里的mutableLiveData是被MainViewModel调用的，不是activity直接调用，
                                // 所以可以用子线程中用的postValue，也可以用setValue。
                                mutableLiveData.postValue(newsList);
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

        return mutableLiveData;

    }

    /**
     * 获取系统时间
     *
     * @return
     */
    public LiveData<String> getTime() {
        //格式化一个中国地区时间
        //final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.CHINA);
        //DateFormat dateFormat = DateFormat.getDateTimeInstance();//上面是详细用法，这个也可以
        //SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.CHINA);//也可以用这个

//        Calendar calendar = Calendar.getInstance();//也可以用这种方法（纯英文的显示方式）反正获取时间的方法很多。
//        Date time = calendar.getTime();
//        timeData.setValue(String.valueOf(time));

        //Calendar calendar = Calendar.getInstance();//也可以用这种方法
//        Date time = calendar.getTime();
//        timeData.setValue(String.valueOf(time));

        //timeLiveData.setValue(dateFormat.format(new Date()));







        //为接收器指定action，使之用于接收同action的广播
        IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_TICK);
        //动态注册广播接收器
        ActivityUtils.getTopActivity().registerReceiver(receiver, filter);
        //返回时间livedata
        return timeLiveData;

    }

    //利用广播实时获取系统时间变化，好像也是一分钟一次
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.CHINA);

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (Objects.requireNonNull(intent.getAction())) {
                case Intent.ACTION_TIME_TICK:
                    timeLiveData.setValue(dateFormat.format(new Date()));
                    break;
            }
        }
    };

}
