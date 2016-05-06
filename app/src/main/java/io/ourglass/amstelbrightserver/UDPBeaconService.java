package io.ourglass.amstelbrightserver;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Created by atorres on 4/19/16.
 */
public class UDPBeaconService extends Service {

    static final String TAG = "UDPBeaconService";

    static final String DEFAULT_DEVICE_NAME = "Overplayer";
    static final String DEFAULT_DEVICE_LOCATION = "Bar";
    static int DEFAULT_PORT = 9090;
    static int DEFAULT_BEACON_FREQ = 5000;  // time in ms between UDP broadcasts

    String mMessage;
    int mPort;
    int mBeaconFreq;
    DatagramSocket mSocket;
    Boolean mSending = false;

    private void sendUDPPacket() {
        if (mSocket == null || mSocket.isClosed()) {
            try {
                mSocket = new DatagramSocket(mPort);
                mSocket.setBroadcast(true);
            } catch (SocketException e) {
                Log.e(TAG, e.getLocalizedMessage());
                return;
            }
        }

        try {
            DatagramPacket packet = new DatagramPacket(mMessage.getBytes(), mMessage.length(),
                    getBroadcastAddress(), mPort);
            mSocket.send(packet);
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }

    }

    private void startBeacon() {
        Log.d(TAG, "startBeacon");
        mSending = true;

        Thread UDPBeaconThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (mSending) {
                    Log.d(TAG, "sending UDP packet");
                    sendUDPPacket();
                    try {
                        Thread.sleep(mBeaconFreq);
                    } catch (InterruptedException e) {
                        Log.e(TAG, e.getLocalizedMessage());
                    }
                }
                mSocket.close();
            }
        });

        UDPBeaconThread.start();
    }

    private void stopBeacon() {
        Log.d(TAG, "stopBeacon");
        mSending = false;
    }

    private InetAddress getBroadcastAddress() throws IOException {
        WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = manager.getDhcpInfo();

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++) {
            quads[k] = (byte) ((broadcast >> k * 8) & 0xff);
        }

        return InetAddress.getByAddress(quads);
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Starting UDP Beacon", Toast.LENGTH_SHORT).show();

        if (intent != null) {
            if (intent.getStringExtra("data") != null) {
                mMessage = intent.getStringExtra("data");
            } else {
                WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                String mac = manager.getConnectionInfo().getMacAddress();
                mMessage = String.format("{\"name\": \"%s\", \"location\": \"%s\", \"mac\": \"%s\"}",
                        DEFAULT_DEVICE_NAME, DEFAULT_DEVICE_LOCATION, mac);
            }

            mPort = intent.getIntExtra("port", DEFAULT_PORT);
            mBeaconFreq = intent.getIntExtra("beaconFreq", DEFAULT_BEACON_FREQ);

        } else {
            WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            String mac = manager.getConnectionInfo().getMacAddress();
            mMessage = String.format("{\"name\": \"%s\", \"location\": \"%s\", \"mac\": \"%s\"}",
                    DEFAULT_DEVICE_NAME, DEFAULT_DEVICE_LOCATION, mac);
            mPort = DEFAULT_PORT;
            mBeaconFreq = DEFAULT_BEACON_FREQ;
        }

        Log.d(TAG, mMessage + " " + mPort + " " + mBeaconFreq);

        startBeacon();

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        stopBeacon();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
