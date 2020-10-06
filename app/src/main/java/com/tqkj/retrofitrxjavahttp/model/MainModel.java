package com.tqkj.retrofitrxjavahttp.model;

import android.annotation.SuppressLint;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.tqkj.retrofitrxjavahttp.bean.WangYiNewsBean;
import com.tqkj.retrofitrxjavahttp.http.BaseObserver;
import com.tqkj.retrofitrxjavahttp.http.BaseRequest;
import com.tqkj.retrofitrxjavahttp.http.BaseResponse;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * author : CYS
 * e-mail : 1584935420@qq.com
 * date : 2020/10/6 1:09
 * desc :
 * version : 1.0
 */
public class MainModel {
    /**
     * 这里必须保证mutableLiveData的唯一，他的初始化不要放在方法中。直接在这里初始化或者构造函数初始化是最好的。
     * 否则会出现空指针异常
     */
    private MutableLiveData<List<WangYiNewsBean>> mutableLiveData = new MutableLiveData<>();


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
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onSuccess(BaseResponse<List<WangYiNewsBean>> baseResponse) {
                        //成功回调方法,可以直接在此更新ui,AndroidSchedulers.mainThread()表示切换到主线程
                        if (baseResponse.isSuccess()) {
                            List<WangYiNewsBean> newsList = baseResponse.getResults();
                            if (newsList != null && !newsList.isEmpty())
                                //如果不是在主线程就要用postValue，但是这里是主线程来的，所以用setValue一样可以的
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

}
