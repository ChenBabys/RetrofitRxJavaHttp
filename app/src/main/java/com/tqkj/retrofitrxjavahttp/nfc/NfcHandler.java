package com.tqkj.retrofitrxjavahttp.nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.widget.Toast;

import com.blankj.utilcode.util.ToastUtils;

import java.io.IOException;

/**
 * author : CYS
 * e-mail : 1584935420@qq.com
 * date : 2020/10/10 23:14
 * desc : NFC读卡功能，如需要指令功能自行修改添加即可。
 * version : 1.0
 */
public class NfcHandler {
    private NfcAdapter mNfcAdapter;
    private IsoDep mIsoDep;
    private NfcView mNfcView;

    public NfcHandler(NfcView mNfcView) {
        this.mNfcView = mNfcView;
    }

    public void init(Context context) {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(context);
    }


    private boolean checkNfc(Context context) {
        if (mNfcAdapter == null) {
            Toast.makeText(context, "未找到NFC设备", Toast.LENGTH_SHORT).show();
            return false;
        } else if (!mNfcAdapter.isEnabled()) {
            Toast.makeText(context, "请在设置中打开NFC开关", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /**
     * 通过tag.getTechList()可以获取当前目标Tag支持的Tag Technology，这里默认支持IsoDep。
     * 通过IsoDep.get(tag)方式获取IsoDep的实例。然后通过函数connect()我们应用和IC卡之间建立联系，
     * 建立联系后我们可以往IC卡发送指令进行交互。
     *
     * @param intent
     */
    private void connectNfc(Intent intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())
//                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())
//                || NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())
        ) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                mIsoDep = IsoDep.get(tag);
                try {
                    mIsoDep.connect();//这里建立我们应用和IC卡
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            ToastUtils.showShort("不支持的类型");
        }
    }

    /**
     * 这个函数可以获取IC卡的序列号
     *
     * @param intent
     */
    public void readCardId(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null && mNfcView != null) {
            byte[] ids = tag.getId();
            String uid = DataUtil.bytesToHexString(ids, ids.length);
            mNfcView.appendResponse(uid);
        }
    }

    /**
     * 启用nfc(启用的时候会判断是否可用再继续)
     *
     * @param activity
     */
    public void enableNfc(Activity activity) {
        if (checkNfc(activity)) {
            PendingIntent pendingIntent = PendingIntent.getActivity(activity
                    , 0, new Intent(activity, activity.getClass()), 0);
            mNfcAdapter.enableForegroundDispatch(activity, pendingIntent, null, null);
            connectNfc(activity.getIntent());//链接nfc
        }
    }

    /**
     * 关闭nfc
     *
     * @param activity
     */
    public void disableNfc(Activity activity) {
        if (mIsoDep != null && mIsoDep.isConnected()) {
            try {
                mIsoDep.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(activity);
        }
    }

    /**
     * 销毁
     */
    public void onDestroy() {
        mNfcView = null;
    }

}
