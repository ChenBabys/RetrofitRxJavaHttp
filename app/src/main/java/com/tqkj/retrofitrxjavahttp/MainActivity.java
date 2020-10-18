package com.tqkj.retrofitrxjavahttp;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.util.ToastUtils;
import com.tqkj.retrofitrxjavahttp.activity.TestWebSocketActivity;
import com.tqkj.retrofitrxjavahttp.activity.WebActivity;
import com.tqkj.retrofitrxjavahttp.adapter.MainListAdapter;
import com.tqkj.retrofitrxjavahttp.bean.WangYiNewsBean;
import com.tqkj.retrofitrxjavahttp.lifecycle.MainLifecycle;
import com.tqkj.retrofitrxjavahttp.nfc.NfcHandler;
import com.tqkj.retrofitrxjavahttp.nfc.NfcView;
import com.tqkj.retrofitrxjavahttp.otherwebsocket.ChatMessage;
import com.tqkj.retrofitrxjavahttp.otherwebsocket.JWebSocketClient;
import com.tqkj.retrofitrxjavahttp.otherwebsocket.JWebSocketClientService;
import com.tqkj.retrofitrxjavahttp.viewmodel.MainViewModel;

import org.java_websocket.enums.ReadyState;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements NfcView {

    private RecyclerView rcvNews;
    private TextView tvTitle, title_tem;
    private MainListAdapter adapter;
    private MainViewModel mainViewModel;
    private NfcHandler nfcHandler;
    //下面是websocket相关
    private JWebSocketClient client;
    private JWebSocketClientService jWebSocketClientService;
    private JWebSocketClientService.JWebSocketClientBinder binder;
    private ChatMessageReveice chatMessageReveice;
    private List<ChatMessage> chatMessageList = new ArrayList<>();//消息列表
    /**
     * 服务连接
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e("MainActivity", "服务与活动成功绑定");
            binder = (JWebSocketClientService.JWebSocketClientBinder) service;
            jWebSocketClientService = binder.getService();
            client = jWebSocketClientService.client;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e("MainActivity", "服务与活动成功断开");
        }
    };


    private class ChatMessageReveice extends BroadcastReceiver {

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.e("MainActivity", "接收到了广播:" + message);
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setContent(message);
            chatMessage.setIsMeSend(0);
            chatMessage.setIsRead(1);
            chatMessage.setTime(System.currentTimeMillis() + "");
            chatMessageList.add(chatMessage);
            // initChatMsgListView();
        }
    }

    /**
     * 初始化聊天列表，不实现这个俩天对话的功能了
     */
    private void initChatMsgListView() {
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvTitle = this.findViewById(R.id.page_title);
        rcvNews = this.findViewById(R.id.rclv_news_list);
        title_tem = this.findViewById(R.id.title_tem);
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

        //webservice相关
        startJWebSClientService();
        bindService();
        doRegisterReceiver();
        checkNotification(this);


        title_tem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, TestWebSocketActivity.class));
            }
        });


    }

    /**
     * 检查是否开启了消息通知
     */
    private void checkNotification(final Context context) {
        if (!isNotificationEnabled(context)) {
            new AlertDialog.Builder(context).setTitle("温馨提示")
                    .setMessage("你还未开启系统通知，将影响消息的接收，要去开启吗？")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setNotificationSetting(context);
                        }
                    });
        }
    }

    /**
     * 如果没有开启通知，跳转到设置界面
     *
     * @param context
     */
    private void setNotificationSetting(Context context) {
        Intent localIntent = new Intent();
        //直接跳转到应用通知设置的代码：
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            localIntent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            localIntent.putExtra("app_package", context.getPackageName());
            localIntent.putExtra("app_uid", context.getApplicationInfo().uid);
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            localIntent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            localIntent.addCategory(Intent.CATEGORY_DEFAULT);
            localIntent.setData(Uri.parse("package:" + context.getPackageName()));
        } else {
            //4.4以下没有从app跳转到应用通知设置页面的Action，可考虑跳转到应用详情页面,
            localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= 9) {
                localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                localIntent.setData(Uri.fromParts("package", context.getPackageName(), null));
            } else if (Build.VERSION.SDK_INT <= 8) {
                localIntent.setAction(Intent.ACTION_VIEW);
                localIntent.setClassName("com.android.settings", "com.android.setting.InstalledAppDetails");
                localIntent.putExtra("com.android.settings.ApplicationPkgName", context.getPackageName());
            }
        }
        context.startActivity(localIntent);
    }

    /**
     * 获取通知权限，检测是否开启了系统通知
     *
     * @param context
     * @return
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private boolean isNotificationEnabled(Context context) {
        String CHECK_OP_NO_THROW = "checkOpNoThrow";
        String OP_POST_NOTIFICATION = "OP_POST_NOTIFICATION";

        AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        String pkg = context.getApplicationContext().getPackageName();
        int uid = applicationInfo.uid;

        Class appOpsClass = null;
        try {
            appOpsClass = Class.forName(AppOpsManager.class.getName());
            Method checkOpNoThrowMethod = appOpsClass.getMethod(CHECK_OP_NO_THROW, Integer.TYPE, Integer.TYPE, String.class);
            Field opPostNotificationValue = appOpsClass.getDeclaredField(OP_POST_NOTIFICATION);
            int value = (Integer) opPostNotificationValue.get(Integer.class);
            return ((Integer) checkOpNoThrowMethod.invoke(appOpsManager, value, uid, pkg) == AppOpsManager.MODE_ALLOWED);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * 注册服务
     */
    private void doRegisterReceiver() {
        chatMessageReveice = new ChatMessageReveice();
        registerReceiver(chatMessageReveice, new IntentFilter("com.tqkj.retrofitrxjavahttp.otherwebsocket.JWebSocketClientService"));
    }

    /**
     * 绑定服务
     */
    private void bindService() {
        bindService(new Intent(this, JWebSocketClientService.class)
                , serviceConnection, BIND_AUTO_CREATE);
    }

    /**
     * 开启服务
     * 需要注意的是//android8.0以上通过startForegroundService启动service
     * 也就是说8.0之后系统不再允许后台启动服务，只能启动前台服务
     */
    private void startJWebSClientService() {
        Intent intent = new Intent(this, JWebSocketClientService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //android8.0以上通过startForegroundService启动service
            startForegroundService(intent);
        } else {
            startService(intent);
        }
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
                            //点击标题发出通知给服务器
                            sendMegToService("今天的不开心就到此为止吧，明天依旧光芒万丈哟宝贝！！！");
                        }
                    });
                } else {
                    adapter.addItems(wangYiNewsBeans);
                }
                //ToastUtils.showShort("现在是第" + mainViewModel.page + "页，总共已经为您推荐了" + wangYiNewsBeans.size() * mainViewModel.page + "条新闻，每次刷新提供8条，请观赏");
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
     * 发送消息到服务器
     */
    private void sendMegToService(String msg) {
        if (client != null && client.isOpen()) {
            jWebSocketClientService.sendMessage(msg);
            //暂时将发送的消息加入消息列表，实际以发送成功为准（也就是服务器返回你发的消息时）
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setContent(msg);
            chatMessage.setIsMeSend(1);
            chatMessage.setIsRead(1);
            chatMessage.setTime(System.currentTimeMillis() + "");
            chatMessageList.add(chatMessage);
        } else {
            Toast.makeText(this, "链接已断开，请稍后或者重启App", Toast.LENGTH_SHORT).show();
        }
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