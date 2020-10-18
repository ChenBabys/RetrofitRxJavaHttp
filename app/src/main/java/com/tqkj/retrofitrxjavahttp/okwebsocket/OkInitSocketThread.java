package com.tqkj.retrofitrxjavahttp.okwebsocket;

import android.os.Handler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * author : CYS
 * e-mail : 1584935420@qq.com
 * date : 2020/10/16 22:21
 * desc : 一个处理长连接的线程
 * version : 1.0
 */
public class OkInitSocketThread extends Thread {

    @Override
    public void run() {
        super.run();
        try {
            initSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static final long HEART_BEAT_RATE = 15 * 1000;//每隔15秒进行一次对长连接的心跳检测
    private static final String WEBSOCKET_HOST_AND_PORT = "https://kyfw.12306.cn/otn/";//自己的主机名和端口号
    private WebSocket mWebSocket;


    private void initSocket() throws UnknownHostException, IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS).build();
        Request request = new Request.Builder().url(WEBSOCKET_HOST_AND_PORT).build();
        client.newWebSocket(request, new WebSocketListener() {

            @Override
            public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                super.onOpen(webSocket, response);
                //开启长连接成功的回调
                mWebSocket = webSocket;
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                super.onMessage(webSocket, text);
                //收到服务器端传过来的消息text
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
                super.onMessage(webSocket, bytes);

            }

            @Override
            public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                super.onClosed(webSocket, code, reason);
            }

            @Override
            public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                super.onClosing(webSocket, code, reason);
            }

            @Override
            public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
                super.onFailure(webSocket, t, response);
                //长连接连接失败的回调
            }

        });
        client.dispatcher().executorService().shutdown();
    }

    private long sendTime = 0L;
    //发送心跳包
    private final Handler handler = new Handler();
    private final Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (System.currentTimeMillis() - sendTime >= HEART_BEAT_RATE) {
                //发送一个空消息给服务器，通过服务器发送消息的成功或失败来判断长连接的链接状态
                boolean isSuccess = mWebSocket.send("");
                //如果长连接已经断开
                if (!isSuccess) {
                    handler.removeCallbacks(heartBeatRunnable);
                    mWebSocket.cancel();//取消掉以前的长连接
                    new OkInitSocketThread().start();//创建一个新的连接
                } else {
                    //todo //长连接处于连接状态
                }

                sendTime = System.currentTimeMillis();

            }
            //每隔一定的时间，对长连接进行一次心跳检测
            handler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE);
        }
    };

    /**
     * 关闭webSocket
     */
    public void closeService() {
        if (mWebSocket != null) {
            mWebSocket.close(1000, null);
        }

    }

}
