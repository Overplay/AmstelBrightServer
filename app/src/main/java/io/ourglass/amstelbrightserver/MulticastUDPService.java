package io.ourglass.amstelbrightserver;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Created by atorres on 5/6/16.
 */
public class MulticastUDPService extends Service {

    static final String TAG = "MulticastUDPService";

    static final String DEFAULT_INET_ADDR = "224.0.0.3";
    static final int DEFAULT_PORT = 8888;
    static final int PACKET_SIZE = 1200;
    static final int BUFFER_SIZE = 4096;

    static final int SAMPLE_RATE = 32000;
    static final int SAMPLE_SIZE_BITS = 16;
    static final int CHANNELS = 2;
    static final boolean SIGNED = true;
    static final boolean BIG_ENDIAN = false;

    String mAddr;
    int mPort;
    InetAddress mInetAddr = null;
    MulticastSocket mSocket = null;
    Boolean mMulticasting = false;
    AudioFormat mAudioFormat = null;

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
                mSocket = new MulticastSocket(mPort);
            } catch (IOException e) {
                Log.e(TAG, e.getLocalizedMessage());
                return;
            }
        }

        byte[] data = new byte[PACKET_SIZE];
        //mSocket.send(new DatagramPacket())
    }

    /*private AudioFormat getAudioFormat() {
        if (mAudioFormat != null) {
            return mAudioFormat;
        }

        AudioFormat.Builder ab = new AudioFormat.Builder();
        ab.setSampleRate(SAMPLE_RATE);

        return ab.build();
    }*/

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
