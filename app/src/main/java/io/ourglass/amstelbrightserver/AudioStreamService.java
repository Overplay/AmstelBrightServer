package io.ourglass.amstelbrightserver;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

/**
 * Created by atorres on 5/6/16.
 */
public class AudioStreamService extends Service {

    static final String TAG = "AudioStreamService";

    static final String DEFAULT_INET_ADDR = "224.0.0.3";
    static final int DEFAULT_PORT = 8888;
    static final int DEFAULT_TTL = 12;

    static final int PACKET_SIZE = 1200;
    static final int BUFFER_SIZE = 4096;

    static final int SAMPLE_RATE = 32000;
    static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    static final int SAMPLE_SIZE_BITS = 16;
    static final int CHANNELS = 2;
    static final boolean SIGNED = true;
    static final boolean BIG_ENDIAN = false;

    String mAddr;
    int mPort;
    int mTTL;
    InetAddress mInetAddr = null;
    MulticastSocket mSocket = null;
    Boolean mStreaming = false;
    AudioFormat mAudioFormat = null;

    int minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    AudioRecord mRecorder = null;

    private void startStreaming() {
        Log.d(TAG, "startStreaming");
        mStreaming = true;

        Thread streamThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (mStreaming) {
                    sendAudioPacket();
                }
                if (mSocket != null) {
                    mSocket.close();
                }
            }
        });

        streamThread.start();

    }

    private void stopStreaming() {
        Log.d(TAG, "stopStreaming");
        mStreaming = false;
        mRecorder.release();
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
                mSocket.joinGroup(mInetAddr);
                mSocket.setTimeToLive(mTTL);
            } catch (IOException e) {
                Log.e(TAG, e.getLocalizedMessage());
                return;
            }
        }

        if (mRecorder == null) {
            mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG,
                    AUDIO_FORMAT, minBufSize * 10);
            mRecorder.startRecording();
        }

        byte[] buffer = new byte[minBufSize];

        minBufSize = mRecorder.read(buffer, 0, buffer.length);

        try {
            mSocket.send(new DatagramPacket(buffer, buffer.length, mInetAddr, mPort));
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }

        Log.d(TAG, "sent audio packet");
    }

    /*private AudioFormat getAudioFormat() {
        if (mAudioFormat == null) {
            AudioFormat.Builder ab = new AudioFormat.Builder();
            ab.setSampleRate(SAMPLE_RATE);
            ab.setEncoding(AUDIO_FORMAT);
            ab.setChannelMask(CHANNEL_CONFIG);
            mAudioFormat = ab.build();
        }

        return mAudioFormat;
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

        startStreaming();

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        stopStreaming();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
