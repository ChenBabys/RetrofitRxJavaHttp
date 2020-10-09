package com.tqkj.retrofitrxjavahttp.lifecycle;


import android.content.Context;
import android.util.Log;

import com.blankj.utilcode.util.LogUtils;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

/**
 * author : CYS
 * e-mail : 1584935420@qq.com
 * date : 2020/10/6 0:09
 * desc : 监听生命周期 在活动中绑定他MainLifeCycle即可
 * version : 1.0
 */
public class MainLifecycle implements LifecycleObserver {

    private Context context;
    private static final String TAG = "ActivityObserver";
    //构造函数，获得实例，如果不需要获取实例也可以删除
    public MainLifecycle(Context context) {
        this.context = context;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void onCreate() {
        Log.d(TAG, "onCreate");
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        Log.d(TAG, "onStart");
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        Log.d(TAG, "onResume");
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        Log.d(TAG, "onPause");
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        Log.d(TAG, "onStop");
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
    }

    //任意回调都会调用它，比如调用完onCreate()后会回调这里的onCreate(),然后会回调onAny();
    @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
    public void onAny() {
        Log.d(TAG, "onAny");
    }

}
