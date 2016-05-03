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

    @Override
    public void onReceive(Context context, Intent i) {
        Intent intent = new Intent(context, AmstelBrightServer.class);
        context.startService(intent);
        Log.d(TAG, "onReceive");
    }

}
