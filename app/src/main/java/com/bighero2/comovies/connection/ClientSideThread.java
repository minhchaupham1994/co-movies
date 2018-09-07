package com.bighero2.comovies.connection;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

/**
 * Created by Tuan on 30/03/2016.
 */
public class ClientSideThread extends Thread{
    private Context mContext;
    protected static final int AUDIO_PORT = 1221;
    protected static final int SIGNAL_PORT = 1331;

    public ClientSideThread(Context context) {
        mContext = context;
    }
    protected String getGatewayIpAddress() {
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
        String gatewayIP = Formatter.formatIpAddress(dhcpInfo.gateway);
        return gatewayIP;
    }

    protected Context getContext() {
        return mContext;
    }
}
