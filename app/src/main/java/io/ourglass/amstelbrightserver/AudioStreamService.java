package io.ourglass.amstelbrightserver;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;

/**
 * Created by atorres on 5/6/16.
 */
public class AudioStreamService extends Service {

    static final String TAG = "AudioStreamService";

    static final String DEFAULT_INET_ADDR = "224.0.0.3";
    static final int DEFAULT_PORT = 8888;
    static final int DEFAULT_TTL = 12;

    static final int SAMPLE_RATE = 44100;
    static final int SAMPLE_INTERVAL = 20; // milliseconds
    static final int SAMPLE_SIZE = 2; // bytes per sample
    static final int BUF_SIZE = SAMPLE_INTERVAL*SAMPLE_INTERVAL*SAMPLE_SIZE*2;

    private String mAddr;
    private int mPort;
    private int mTTL;
    private InetAddress mInetAddr = null;
    private Boolean mStreaming = false;
    private MulticastSocket mSocket = null;
    //private DatagramSocket mSocket = null;
    private MediaCodec mEncoder;
    private WifiManager.MulticastLock mMulticastLock;

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

        WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        mMulticastLock = wifi.createMulticastLock(TAG);
        mMulticastLock.acquire();

        stream();

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        stopStream();
        mMulticastLock.release();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void stream() {
        mStreaming = true;

        Thread streamThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int bytes_count = 0;
                byte[] dataBuffer = new byte[BUF_SIZE];

                try {
                    mInetAddr = InetAddress.getByName(mAddr);
                    if (mSocket == null || mSocket.isClosed()) {
                        mSocket = new MulticastSocket(mPort);
                        mSocket.joinGroup(mInetAddr);
                    }

                    InputStream audio_stream = getResources().openRawResource(
                            getResources().getIdentifier("sample", "raw", getPackageName()));

                    setEncoder(SAMPLE_RATE);
                    mEncoder.start();

                    while (mStreaming) {
                        int bytes_read = audio_stream.read(dataBuffer, 0, BUF_SIZE);

                        ByteBuffer[] inputBuffers = null, outputBuffers = null;
                        ByteBuffer inputBuffer = null, outputBuffer = null;

                        if (Build.VERSION.SDK_INT < 21) {
                            inputBuffers = mEncoder.getInputBuffers();
                            outputBuffers = mEncoder.getOutputBuffers();
                        }

                        int inputBufferIdx = mEncoder.dequeueInputBuffer(-1);

                        if (inputBufferIdx >= 0) {

                            if (Build.VERSION.SDK_INT >= 21) {
                                inputBuffer = mEncoder.getInputBuffer(inputBufferIdx);
                            } else if (inputBuffers != null) {
                                inputBuffer = inputBuffers[inputBufferIdx];
                            }

                            if (inputBuffer != null) {
                                inputBuffer.clear();
                                inputBuffer.put(dataBuffer);
                                mEncoder.queueInputBuffer(inputBufferIdx, 0, bytes_read, 0, 0);
                            }
                        }

                        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                        int outputBufferIdx = mEncoder.dequeueOutputBuffer(bufferInfo, 0);

                        while (outputBufferIdx >= 0) {

                            if (Build.VERSION.SDK_INT >= 21) {
                                outputBuffer = mEncoder.getOutputBuffer(outputBufferIdx);
                            } else if (outputBuffers != null) {
                                outputBuffer = outputBuffers[outputBufferIdx];
                            }

                            if (outputBuffer != null) {
                                outputBuffer.position(bufferInfo.offset);
                                outputBuffer.limit(bufferInfo.offset + bufferInfo.size);

                                byte[] outData = new byte[bufferInfo.size];
                                outputBuffer.get(outData);

                                DatagramPacket packet = new DatagramPacket(outData, outData.length,
                                        mInetAddr, mPort);
                                mSocket.setTimeToLive(1);

                                mSocket.send(packet);
                            }

                            mEncoder.releaseOutputBuffer(outputBufferIdx, false);
                            outputBufferIdx = mEncoder.dequeueOutputBuffer(bufferInfo, 0);
                        }

                        bytes_count += bytes_read;
                        Log.d(TAG, "bytes_count : " + bytes_count);
                        Thread.sleep(SAMPLE_INTERVAL, 0);
                    }
                }

                catch (Exception e) {
                    Log.e(TAG, "Exception: " + e);
                    e.printStackTrace();

                } finally {
                    if (mEncoder != null) {
                        mEncoder.stop();
                    }
                    if (mSocket != null && !mSocket.isClosed()) {
                        mSocket.close();
                    }
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

    private void setEncoder(int sampleRate) throws IOException {
        mEncoder = MediaCodec.createEncoderByType("audio/mp4a-latm");
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
        format.setInteger(MediaFormat.KEY_BIT_RATE, 64 * 1024);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectHE);
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    }
}
