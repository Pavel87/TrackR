package com.pacmac.trackr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by tqm837 on 4/27/2016.
 */


public class NetworkStateChangedReceiver extends BroadcastReceiver {

    private NetworkStateListener listener = null;

    @Override
    public void onReceive(Context context, Intent intent) {

        ConnectivityManager conn = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = conn.getActiveNetworkInfo();

        if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
            update(true);
        } else {
            update(false);
        }
    }

    public void setConnectionListener(NetworkStateListener listener){
        this.listener = listener;
    }

    private void update(boolean isConnected) {
        if (listener != null) {
            listener.connectionChanged(isConnected);
        }
    }
}
