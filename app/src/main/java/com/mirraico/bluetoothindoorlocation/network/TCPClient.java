package com.mirraico.bluetoothindoorlocation.network;


import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Set;

public class TCPClient {
    private static TCPClient tcpClient;
    private static String TAG = "connect_tag";

    private boolean isConnect = false;
    private SocketChannel channel;
    private Selector selector;
    private String remoteIp;
    private int remotePort;
    private ByteBuffer sendBuffer;
    private ByteBuffer recvBuffer;
    private Charset cs;

    public static synchronized TCPClient instance() {
        if ( tcpClient == null ) {
            tcpClient = new TCPClient();
            tcpClient.sendBuffer = ByteBuffer.allocate(1024);
            tcpClient.cs = Charset.forName("utf-8");
        }
        return tcpClient;
    }

    public void connect( String remoteIp, int remotePort) {
        this.remoteIp = remoteIp;
        this.remotePort = remotePort;
        connect();
    }

    private void connect() {
        ConnectThread connect = new ConnectThread();
        new Thread(connect).start();
    }

    public int send(String str) {
        if(!isConnect) return 0;
        try {
            sendBuffer.clear();
            sendBuffer.put(str.getBytes("utf-8"));
            sendBuffer.flip();
            Log.e(TAG, str);
            return channel.write(sendBuffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private class ConnectThread implements Runnable {

        @Override
        public void run() {
            try {
                SocketAddress ad = new InetSocketAddress(remoteIp, remotePort);
                channel = SocketChannel.open();
                channel.configureBlocking(false);
                Log.e(TAG, "try to connect to tcp service");
                channel.connect(ad);
                while (!channel.finishConnect()){
                }
                isConnect = true;
                Log.e(TAG, "connect to tcp service successfully");
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (true) {
                try {
                    selector.select();
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    for (SelectionKey key : selectionKeys) {
                        if(key.isReadable()) {
                            SocketChannel recv = (SocketChannel)key.channel();
                            recvBuffer.clear();
                            int ret = recv.read(recvBuffer);
                            if (ret > 0) {
                                recvBuffer.flip();
                                String str = String.valueOf(cs.decode(recvBuffer).array());
                                Log.e(TAG, "recv: str");
                            }
                        }
                    }
                    selectionKeys.clear();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    break;
                }

            }
        }
    }
}
