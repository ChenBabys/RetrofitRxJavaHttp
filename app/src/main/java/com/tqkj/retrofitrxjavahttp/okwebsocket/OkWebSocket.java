package com.tqkj.retrofitrxjavahttp.okwebsocket;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

/**
 * author : CYS
 * e-mail : 1584935420@qq.com
 * date : 2020/10/16 22:19
 * desc : 记得在清单文件中添加服务。
 * version : 1.0
 */
public class OkWebSocket extends Service {
    private OkInitSocketThread okInitSocketThread;

    @Override
    public void onCreate() {
        super.onCreate();
        //开启一个线程
        okInitSocketThread = new OkInitSocketThread();
        okInitSocketThread.start();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        okInitSocketThread.closeService();
    }
}
