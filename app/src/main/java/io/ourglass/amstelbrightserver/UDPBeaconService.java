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

    // TODO: get info from a config file
    private static final String DEVICE_NAME = "Overplayer 1";
    private static final String DEVICE_LOCATION = "Bar";

    private static final String TAG = "UDPBeaconService";
    private static int PORT = 9090;
    private static int BEACON_FREQUENCY = 5000;  // time in ms between UDP broadcasts

    private String message;
    private DatagramSocket socket;
    private Boolean sending = false;

    private void sendUDPPacket() {
        if (socket == null || socket.isClosed()) {
            try {
                socket = new DatagramSocket(PORT);
                socket.setBroadcast(true);
            } catch (SocketException e) {
                Log.e(TAG, e.getLocalizedMessage());
                return;
            }
        }

        try {
            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(),
                    getBroadcastAddress(), PORT);
            socket.send(packet);
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }

    }

    private void startBeacon() {
        Log.d(TAG, "startBeacon");
        sending = true;

        WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        String mac = manager.getConnectionInfo().getMacAddress();
        message = String.format("{\"name\": \"%s\", \"location\": \"%s\", \"mac\": \"%s\"}",
                DEVICE_NAME, DEVICE_LOCATION, mac);

        Thread UDPBeaconThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (sending) {
                    Log.d(TAG, "sending UDP packet");
                    sendUDPPacket();
                    try {
                        Thread.sleep(BEACON_FREQUENCY);
                    } catch (InterruptedException e) {
                        Log.e(TAG, e.getLocalizedMessage());
                    }
                }
                socket.close();
            }
        });

        UDPBeaconThread.start();
    }

    private void stopBeacon() {
        Log.d(TAG, "stopBeacon");
        sending = false;
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
        startBeacon();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // This restarts the service if it is killed by Android
        Toast.makeText(this, "Starting UDP Beacon", Toast.LENGTH_SHORT).show();
        return Service.START_NOT_STICKY;
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
