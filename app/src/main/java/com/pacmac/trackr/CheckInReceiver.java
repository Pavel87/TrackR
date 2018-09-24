package com.pacmac.trackr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class CheckInReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        LocationUpdate locationUpdate = LocationUpdate.getLocationUpdateInstance(context, null);
        locationUpdate.getLocation();
        Toast.makeText(context.getApplicationContext(), context.getResources().getString(R.string.check_in_msg), Toast.LENGTH_LONG).show();
    }
}
