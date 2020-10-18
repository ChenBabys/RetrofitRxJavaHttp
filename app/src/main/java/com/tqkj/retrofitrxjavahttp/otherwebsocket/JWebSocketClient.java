package com.tqkj.retrofitrxjavahttp.otherwebsocket;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

/**
 * author : CYS
 * e-mail : 1584935420@qq.com
 * date : 2020/10/16 23:20
 * desc : 需要注意的是WebSocketClient对象是不能重复使用的，所以不能重复初始化，其他地方只能调用当前这个Client。
 * websocket协议地址大致是这样的 ws:// ip地址 : 端口号
 * version : 1.0
 */
public class JWebSocketClient extends WebSocketClient {

    public JWebSocketClient(URI serverUri) {
        //new Draft_6455()代表使用的协议版本，这里可以不写或者写成这样即可。
        super(serverUri, new Draft_6455());
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.e("JWebSocketClient", "onOpen()");
    }

    @Override
    public void onMessage(String message) {
        Log.e("JWebSocketClient", "onMessage()");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.e("JWebSocketClient", "onClose()");
    }

    @Override
    public void onError(Exception ex) {
        Log.e("JWebSocketClient", "onError()");
    }
}
