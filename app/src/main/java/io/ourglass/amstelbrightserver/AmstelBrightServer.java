package io.ourglass.amstelbrightserver;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.widget.Toast;

/**
 * This is the parent server class that kicks off everybody else: UDP, Bluetooth, HTTP
 *
 * (c) Ourglass
 * Mitch Kahn, May 2016
 */

public class AmstelBrightServer extends Service {


    Context mContext = getApplicationContext();
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    public AmstelBrightServer() {
    }

    /** indicates how to behave if the service is killed */
    int mStartMode = START_STICKY;

    // For debug toasts
    public static final Boolean DEBUG = true;


    /** indicates whether onRebind should be used */
    boolean mAllowRebind = true;

    /** Called when the service is being created. */
    @Override
    public void onCreate() {

    }

    /** The service is starting, due to a call to startService() */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        dbToastr("ABS: onStartCommand");

        // Let's start the UDP service
        startChildServices();

        return mStartMode;
    }

    /** A client is binding to the service with bindService() */
    @Override
    public IBinder onBind(Intent intent) {
        dbToastr("ABS: binding");
        return mMessenger.getBinder();
    }

    /** Called when all clients have unbound with unbindService() */
    @Override
    public boolean onUnbind(Intent intent) {
        super.onUnbind(intent);
        dbToastr("ABS: unbinding");
        return mAllowRebind;
    }


    /** Called when The service is no longer used and is being destroyed */
    @Override
    public void onDestroy() {
        super.onDestroy();
        dbToastr("ABS: onDestroy");

    }


    // TODO: Seriously with the leaks? This code is right from the Google site. FCOL
    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Toast.makeText(getApplicationContext(), "hello!", Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    Toast.makeText(getApplicationContext(), "beer thirty!", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }


    private void startChildServices(){


        Intent intent = new Intent(this, UDPBeaconService.class)
                .putExtra("data", "some data to broadcast")
                .putExtra("port", 1234)
                .putExtra("beaconFreq", 2000);

        startService(intent);

    }


    private void dbToastr(String msg){

        if (DEBUG){
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }

    }
}
