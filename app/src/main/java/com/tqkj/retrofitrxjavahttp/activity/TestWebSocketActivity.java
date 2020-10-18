package com.tqkj.retrofitrxjavahttp.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.tqkj.retrofitrxjavahttp.MainActivity;
import com.tqkj.retrofitrxjavahttp.R;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TestWebSocketActivity extends AppCompatActivity {
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_web_socket);
        textView = findViewById(R.id.text_clic);


        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url("ws://192.168.1.147:8080/examples/websocket/chat").build();

                client.newWebSocket(request, new WebSocketListener() {
                    @Override
                    public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                        super.onClosed(webSocket, code, reason);
                        Log.d("client", "onClosed" + reason);
                    }

                    @Override
                    public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                        super.onClosing(webSocket, code, reason);
                        Log.d("client", "onClosing" + reason);
                    }

                    @Override
                    public void onFailure(@NotNull WebSocket webSocket, final @NotNull Throwable t, final @Nullable Response response) {
                        super.onFailure(webSocket, t, response);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("client", "onFailure" + t.getMessage());
                            }
                        });
                    }

                    @Override
                    public void onMessage(@NotNull WebSocket webSocket, final @NotNull String text) {
                        super.onMessage(webSocket, text);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("client", "onMessage" + text);
                                textView.append("\n" + text);
                                sendNotification(text);
                            }
                        });
                    }

                    @Override
                    public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
                        super.onMessage(webSocket, bytes);
                    }

                    @Override
                    public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                        super.onOpen(webSocket, response);
                        Log.d("client", "onOpen" + response.message());
                    }
                });


            }
        });

    }


    /**
     * 发送通知
     * 8.1以上系统开启通知闪退问题,是因为通知要添加信道才行了。
     *
     * @param message
     */
    private Notification sendNotification(String message) {
        String ChannelId = "webId";
        String ChannelName = "web";
        NotificationChannel notificationChannel;
        Notification notification;

        Intent intent = new Intent();
        intent.setClass(this, TestWebSocketActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //添加信道以只会安卓8.1以上消息通知,创建信道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(
                    ChannelId, ChannelName, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(false);//如果使用中的设备支持通知灯，则说明此通知通道是否应显示灯
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(true);//是否显示角标
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            //描述
            notificationChannel.setDescription("websocda");
            notificationManager.createNotificationChannel(notificationChannel);
            //创建通知
            notification = new NotificationCompat.Builder(this, ChannelId)//必须添加信道id
                    .setAutoCancel(true)
                    // 设置该通知优先级
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setSmallIcon(R.drawable.ic_launcher_background)//在android8.0之后的通知没有SmallIcon也会报错。必须加上
                    .setContentTitle("服务器子弹")
                    .setContentText(message)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setWhen(System.currentTimeMillis())
                    // 向通知添加声音、闪灯和振动效果
                    .setDefaults(Notification.DEFAULT_VIBRATE |
                            Notification.DEFAULT_ALL | Notification.DEFAULT_SOUND)
                    .setContentIntent(pendingIntent)
                    .build();
        } else {
            notification = new Notification.Builder(this)//必须添加信道id
                    .setAutoCancel(true)
                    // 设置该通知优先级
                    .setPriority(Notification.PRIORITY_MAX)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("服务器拿到的字段")
                    .setContentText(message)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setWhen(System.currentTimeMillis())
                    // 向通知添加声音、闪灯和振动效果
                    .setDefaults(Notification.DEFAULT_VIBRATE |
                            Notification.DEFAULT_ALL | Notification.DEFAULT_SOUND)
                    .setContentIntent(pendingIntent)
                    .build();
        }
        //notification.flags |= Notification.FLAG_NO_CLEAR;
        //通知开始唤醒
        //startForeground(100001, notification);//前台通知,和下面的二取一
        notificationManager.notify(1001, notification);//id要保证唯一

        return notification;
    }

}