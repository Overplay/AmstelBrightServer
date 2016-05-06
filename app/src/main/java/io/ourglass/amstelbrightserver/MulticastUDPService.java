package io.ourglass.amstelbrightserver;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Created by atorres on 5/6/16.
 */
public class MulticastUDPService extends Service {

    static final String TAG = "MulticastUDPService";

    static final String DEFAULT_INET_ADDR = "224.0.0.3";
    static final int DEFAULT_PORT = 8888;

    String mAddr;
    int mPort;
    InetAddress mInetAddr = null;
    DatagramSocket mSocket = null;
    Boolean mMulticasting = false;

    private void startMulticasting() {
        Log.d(TAG, "startMulticasting");
        mMulticasting = true;

        Thread MulticastThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (mMulticasting) {
                    sendAudioPacket();
                }
                if (mSocket != null) {
                    mSocket.close();
                }
            }
        });

        MulticastThread.start();

    }

    private void stopMulticasting() {
        Log.d(TAG, "stopMulticasting");
        mMulticasting = false;
    }

    private void sendAudioPacket() {
        if (mInetAddr == null) {
            try {
                mInetAddr = InetAddress.getByName(mAddr);
            } catch (UnknownHostException e) {
                Log.e(TAG, e.getLocalizedMessage());
                return;
            }
        }

        if (mSocket == null || mSocket.isClosed()) {
            try {
                mSocket = new DatagramSocket(mPort);
            } catch (SocketException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Starting Multicast UDP server", Toast.LENGTH_SHORT).show();

        if (intent != null) {
            if (intent.getStringExtra("addr") != null) {
                mAddr = intent.getStringExtra("iaddr");
            } else {
                mAddr = DEFAULT_INET_ADDR;
            }
            mPort = intent.getIntExtra("port", DEFAULT_PORT);

        } else {
            mAddr = DEFAULT_INET_ADDR;
            mPort = DEFAULT_PORT;
        }

        startMulticasting();

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        stopMulticasting();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
