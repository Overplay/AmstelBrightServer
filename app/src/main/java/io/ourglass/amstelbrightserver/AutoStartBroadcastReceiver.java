package io.ourglass.amstelbrightserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by mkahn on 5/3/16.
 */
public class AutoStartBroadcastReceiver extends BroadcastReceiver {

    public static final String TAG = "AutoStartBroadcastRX";

    public void onReceive(Context context, Intent i) {
        Intent intent = new Intent(context, UDPBeaconService.class)
                .putExtra("data", "some data to broadcast")
                .putExtra("port", 1234)
                .putExtra("beaconFreq", 2000);

        context.startService(intent);
        Log.d(TAG, "onReceive");
    }

}
