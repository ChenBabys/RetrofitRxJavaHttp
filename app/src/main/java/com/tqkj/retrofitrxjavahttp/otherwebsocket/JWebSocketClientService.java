package com.tqkj.retrofitrxjavahttp.otherwebsocket;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import com.tqkj.retrofitrxjavahttp.MainActivity;
import com.tqkj.retrofitrxjavahttp.R;

import org.java_websocket.enums.ReadyState;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * author : CYS
 * e-mail : 1584935420@qq.com
 * date : 2020/10/17 0:36
 * desc :
 * version : 1.0
 */
public class JWebSocketClientService extends Service {
    public JWebSocketClient client;
    private JWebSocketClientBinder mBinder = new JWebSocketClientBinder();
    private static final int GRAY_SERVICE_ID = 1001;

    //灰色保活
    public static class GrayInnerService extends Service {

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            startForeground(GRAY_SERVICE_ID, new Notification());
            stopForeground(true);
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }


    PowerManager.WakeLock wakeLock;

    @SuppressLint("InvalidWakeLockTag")
    private void acquireWakeLock() {
        if (null == wakeLock) {
            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
                    | PowerManager.ON_AFTER_RELEASE, "PostLocationService");
            if (null != wakeLock) {
                wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);//自己加的时间限制
            }
        }
    }

    //用于Activity和service通讯
    public class JWebSocketClientBinder extends Binder {
        public JWebSocketClientService getService() {
            return JWebSocketClientService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return mBinder;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //初始化websocket
        initSocketClient();
        mHandler.postDelayed(mRunnable, HEART_BEAT_RATE);//开启心跳检测
        //设置service为前台服务，提高优先级
        if (Build.VERSION.SDK_INT < 18) {
            //Android4.3以下 ，隐藏Notification上的图标
            startForeground(GRAY_SERVICE_ID, new Notification());
        } else if (Build.VERSION.SDK_INT > 18 && Build.VERSION.SDK_INT < 25) {
            //Android4.3 - Android7.0，隐藏Notification上的图标
            Intent innerIntent = new Intent(this, GrayInnerService.class);
            startService(innerIntent);
            startForeground(GRAY_SERVICE_ID, new Notification());
        } else {
            //Android7.0以上app启动后通知栏会出现一条"正在运行"的通知
            startForeground(GRAY_SERVICE_ID, sendNotification("第一次发送的监测通知"));//这里不能直接new Notification();会因为缺少信道报错
        }
        acquireWakeLock();
        return START_STICKY;
    }


    private void initSocketClient() {
        Log.e("JWebSocketClientService", "正在初始化");
        //URI uri = URI.create("ws://192.168.1.147:8080/examples/websocket/chat");//这个是测试的地址，按需求修改 ws:// ip地址 : 端口号
        URI uri = URI.create("ws://echo.websocket.org");//这个是测试的地址，按需求修改 ws:// ip地址 : 端口号
        client = new JWebSocketClient(uri) {
            @Override
            public void onMessage(String message) {
                Log.e("JWebSocketClientService", "收到的消息：" + message);
                Intent intent = new Intent();
                intent.setAction("com.tqkj.retrofitrxjavahttp.otherwebsocket.JWebSocketClientService");//待修改
                intent.putExtra("message", message);
                sendBroadcast(intent);//发送广播
                checkLockAndShowNotification(message);//发出通知
            }

            @Override
            public void onOpen(ServerHandshake handshakedata) {
                super.onOpen(handshakedata);
                Log.e("JWebSocketClientService", "websocketOnOpen连接成功");
            }

            @Override
            public void onError(Exception ex) {
                super.onError(ex);
                Log.e("JWebSocketClientService", ex.getMessage());
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                super.onClose(code, reason, remote);
                Log.e("JWebSocketClientService", reason);
            }

        };
        connect();

    }

    /**
     * 连接websocket
     */
    private void connect() {
        new Thread() {
            @Override
            public void run() {
                try {
                    //connectBlocking多出一个等待操作，会先连接再发送，否则未连接发送会报错
                    Log.e("JWebSocketClientService", "正在连接到服务器");
                    client.connectBlocking();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("JWebSocketClientService", e.getMessage());
                }
            }
        }.start();
    }

    /**
     * * 发送消息
     * *
     */
    public void sendMessage(String msg) {
        if (null != client) {
            client.send(msg);//发送消息的服务器
            //测试了好久都不起作用。（测试设备的是小米10和vivoNex）
//            if (client.getReadyState().equals(ReadyState.OPEN)) {
//                Log.e("JWebSocketClientService", "发送的消息：" + msg);
//                client.send(msg);//发送消息的服务器
//            } else {
//                Log.e("JWebSocketClientService", "不处于开启状态");
//            }
        }
    }

    /**
     * 断开连接
     */
    private void closeConnect() {
        try {
            if (null != client) {
                client.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("JWebSocketClientService", e.getMessage());
        } finally {
            client = null;
        }
    }
//    -----------------------------------消息通知--------------------------------------------------------

    /**
     * 检查锁屏状态，如果锁屏，先点亮屏幕
     *
     * @param message
     */
    @SuppressLint("InvalidWakeLockTag")
    private void checkLockAndShowNotification(String message) {
        //管理锁屏的一个服务
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (km.inKeyguardRestrictedInputMode()) {//锁屏
            //获取电源管理器对象
            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            if (!pm.isInteractive()) {
                PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
                wl.acquire(10 * 60 * 1000L /*10 minutes*/);  //点亮屏幕
                wl.release(); //任务结束后释放
            }
            sendNotification(message);
        } else {
            sendNotification(message);
        }

    }

    /**
     * 发送通知
     * 8.1以上系统开启通知闪退问题,是因为通知要添加信道才行了。
     *
     * @param message
     */
    private Notification sendNotification(String message) {
        String ChannelId = "webTestId";
        String ChannelName = "webServiceTest";
        NotificationChannel notificationChannel;
        Notification notification;

        Intent intent = new Intent();
        intent.setClass(this, MainActivity.class);
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
            notificationChannel.setDescription("websocket");
            notificationManager.createNotificationChannel(notificationChannel);
            //创建通知
            notification = new NotificationCompat.Builder(this, ChannelId)//必须添加信道id
                    .setAutoCancel(true)
                    // 设置该通知优先级
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setSmallIcon(R.drawable.ic_launcher_background)//在android8.0之后的通知没有SmallIcon也会报错。必须加上
                    .setContentTitle("服务器")
                    .setContentText(message)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setWhen(System.currentTimeMillis())
                    // 向通知添加声音、闪灯和振动效果
                    .setDefaults(Notification.DEFAULT_VIBRATE |
                            Notification.DEFAULT_ALL | Notification.DEFAULT_SOUND)
                    .setContentIntent(pendingIntent)
                    .build();
        } else {
            notification = new Notification.Builder(this)
                    .setAutoCancel(true)
                    // 设置该通知优先级
                    .setPriority(Notification.PRIORITY_MAX)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("服务器")
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
        notificationManager.notify(100001, notification);//id要保证唯一

        return notification;
    }

    //-------------------------------------websocket心跳检测------------------------------------------------
    private static final long HEART_BEAT_RATE = 10 * 1000;//每隔10秒进行一次对长连接的心跳检测
    private final Handler mHandler = new Handler();
    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            Log.e("JWebSocketClientService", "心跳包检测websocket连接状态");
            if (client != null) {
                if (client.isClosed()) {
                    reconnectWs();
                    Log.e("JWebSocketClientService", "client是关闭的");
                } else if (client.isOpen()) {
                    Log.e("JWebSocketClientService", "client是开启的");
                } else if (client.isClosing()) {
                    Log.e("JWebSocketClientService", "client是isClosing");
                } else if (client.isFlushAndClose()) {
                    Log.e("JWebSocketClientService", "client是isFlushAndClose");
                } else if (client.isReuseAddr()) {
                    Log.e("JWebSocketClientService", "client是isReuseAddr");
                } else if (client.isTcpNoDelay()) {
                    Log.e("JWebSocketClientService", "client是isTcpNoDelay");
                } else {
                    Log.e("JWebSocketClientService", "client不知道在干吗");
                }
            } else {
                //如果client已为空，重新初始化连接
                client = null;
                initSocketClient();
                Log.e("JWebSocketClientService", "client是空的");
            }
            //每隔一定的时间，对长连接进行一次心跳检测
            mHandler.postDelayed(mRunnable, HEART_BEAT_RATE);
        }
    };

    /**
     * 开启重连
     */
    private void reconnectWs() {
        mHandler.removeCallbacks(mRunnable);
        //子线程异步
        new Thread() {
            @Override
            public void run() {
                try {
                    Log.e("JWebSocketClientService", "开启重连");
                    client.reconnectBlocking();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.e("JWebSocketClientService", e.getMessage());
                }
            }
        }.start();

    }

    @Override
    public void onDestroy() {
        Log.e("JWebSocketClientService", "onDestroy");
        closeConnect();
        super.onDestroy();
    }

    public JWebSocketClientService() {
    }
}
