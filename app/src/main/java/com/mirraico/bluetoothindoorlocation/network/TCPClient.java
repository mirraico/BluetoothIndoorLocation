package com.mirraico.bluetoothindoorlocation.network;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class TCPClient {
    private static TCPClient tcpClient;

    private SocketChannel channel;
    private Selector selector;
    private String remoteIp;
    private int remotePort;

    public static synchronized TCPClient instance() {
        if ( tcpClient == null ) {
            tcpClient = new TCPClient();
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

    private class ConnectThread implements Runnable {

        @Override
        public void run() {
            try {
                SocketAddress ad = new InetSocketAddress(remoteIp, remotePort);
                channel = SocketChannel.open(ad);
                if(channel != null) {
                    channel.socket().setTcpNoDelay(true);
                    channel.socket().setKeepAlive(true);
                    channel.socket().setSoTimeout(10000);
                    channel.configureBlocking(false);


                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
