package io.ourglass.amstelbrightserver;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by atorres on 5/6/16.
 */
public class AudioStreamService extends Service {

    static final String TAG = "AudioStreamService";

    static final String DEFAULT_INET_ADDR = "207.62.162.232"; // sending directly to test tablet IP
    //static final String DEFAULT_INET_ADDR = "224.0.0.3";
    static final int DEFAULT_PORT = 8888;
    static final int DEFAULT_TTL = 12;

    static final int SAMPLE_INTERVAL = 20; // milliseconds
    static final int SAMPLE_SIZE = 2; // bytes per sample
    static final int BUF_SIZE = SAMPLE_INTERVAL*SAMPLE_INTERVAL*SAMPLE_SIZE*2;

    String mAddr;
    int mPort;
    int mTTL;
    InetAddress mInetAddr = null;
    Boolean mStreaming = false;
    DatagramSocket mSocket = null;

    public void stream() {
        mStreaming = true;

        Thread streamThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int bytes_count = 0;
                byte[] buf = new byte[BUF_SIZE];

                try {
                    mInetAddr = InetAddress.getByName(mAddr);
                    if (mSocket == null || mSocket.isClosed()) {
                        mSocket = new DatagramSocket();
                    }
                    InputStream audio_stream = getResources().openRawResource(
                            getResources().getIdentifier("sample", "raw", getPackageName()));

                    while (mStreaming) {
                        int bytes_read = audio_stream.read(buf, 0, BUF_SIZE);
                        DatagramPacket pack = new DatagramPacket(buf, bytes_read,
                                mInetAddr, mPort);
                        mSocket.send(pack);
                        bytes_count += bytes_read;
                        Log.d(TAG, "bytes_count : " + bytes_count);
                        Thread.sleep(SAMPLE_INTERVAL, 0);
                    }

                    if (mSocket != null && !mSocket.isClosed()) {
                        mSocket.close();
                    }
                }
                catch (Exception e) {
                    Log.e(TAG, e.getLocalizedMessage());
                }
            }
        });

        streamThread.start();
    }

    private void stopStream() {
        Log.d(TAG, "stopStream");
        mStreaming = false;
        if (mSocket != null && !mSocket.isClosed()) {
            mSocket.close();
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
                mAddr = intent.getStringExtra("addr");
            } else {
                mAddr = DEFAULT_INET_ADDR;
            }
            mPort = intent.getIntExtra("port", DEFAULT_PORT);
            mTTL = intent.getIntExtra("ttl", DEFAULT_TTL);

        } else {
            mAddr = DEFAULT_INET_ADDR;
            mPort = DEFAULT_PORT;
        }

        stream();

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        stopStream();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
