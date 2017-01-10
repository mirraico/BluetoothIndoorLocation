package com.mirraico.bluetoothindoorlocation.network;


import android.os.Handler;
import android.util.Log;
import android.util.StringBuilderPrinter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Set;

public class TCPConnection {

    private static String TAG = TCPConnection.class.getSimpleName();

    private static TCPConnection tcpConnection; //单例模式

    private boolean flag = false; //连接状态
    private String remoteIp; //服务器地址
    private int remotePort; //服务器端口

    private Handler mainHandler; //主界面Handler

    private SocketChannel channel;
    private Selector selector;

    private final static int BUFFER_SIZE = 4096;
    private ByteBuffer sendBuffer;
    private ByteBuffer recvBuffer;
    private StringBuilder pool; //处理TCP黏包的缓冲池
    private Charset charset; //字符集

    private TCPConnection() {}

    //获得单例模式实例
    public static synchronized TCPConnection instance() {
        if(tcpConnection == null) {
            tcpConnection = new TCPConnection();
            tcpConnection.sendBuffer = ByteBuffer.allocate(BUFFER_SIZE);
            tcpConnection.recvBuffer = ByteBuffer.allocate(BUFFER_SIZE);
            tcpConnection.charset = Charset.forName("utf-8");
            tcpConnection.pool = new StringBuilder();
        }
        return tcpConnection;
    }

    //返回当前是否连接
    public boolean isConnect() {
        return flag;
    }

    //设置服务器地址
    public void setServerAddr(String remoteIp, int remotePort) {
        this.remoteIp = remoteIp;
        this.remotePort = remotePort;
    }

    //设置主界面的回调
    public void setHandler(Handler handler) {
        this.mainHandler = handler;
    }

    //连接，该函数立刻返回，连接过程在线程中完成，连接成功后接收也由该线程完成
    public void connect() {
        if(remoteIp == null) return;
        ConnectionThread conn = new ConnectionThread();
        new Thread(conn).start();
    }

    //向服务器发送数据
    public int send(String str) {
        if(!flag) return -1; //未连接
        try {
            sendBuffer.clear();
            sendBuffer.put(str.getBytes("utf-8"));
            sendBuffer.flip();
            Log.e(TAG, "SEND: " + str);
            return channel.write(sendBuffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private class ConnectionThread implements Runnable {

        @Override
        public void run() {
            try {
                SocketAddress addr = new InetSocketAddress(remoteIp, remotePort);
                channel = SocketChannel.open();
                channel.configureBlocking(false);

                Log.e(TAG, "TRY TO CONNECT SERVER");
                channel.connect(addr);
                while (!channel.finishConnect()){ //如果连接失败将抛出异常
                }
                flag = true;
                Log.e(TAG, "CONNECT SERVER SUCCESSFULLY");

                selector = Selector.open();
                channel.register(selector, SelectionKey.OP_READ); //注册可读事件
            } catch (IOException e) {
                e.printStackTrace();
                flag = false;
                return;
            }

            while(true) {
                try {
                    int num = selector.select();
                    //Log.e(TAG, "SELECT NUM: " + num);

                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    for (SelectionKey key : selectionKeys) {
                        if(key.isReadable()) {
                            SocketChannel recv = (SocketChannel)key.channel();
                            recvBuffer.clear();
                            int ret = recv.read(recvBuffer);
                            if(ret == -1) { //服务器已断开
                                Log.e(TAG, "SERVER IS DOWN");
                                flag = false;
                                //TODO: 通知主界面
                                return;
                            } else if(ret > 0) {
                                recvBuffer.flip();
                                String recvString = String.valueOf(charset.decode(recvBuffer).array());
                                Log.e(TAG, "RECV: " + recvString);
                                pool.append(recvString);
                                execute(pool); //TCP黏包处理
                            }
                        }
                    }
                    selectionKeys.clear();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    flag = false;
                    return;
                }

            }
        }

        //TCP黏包处理，找出合法的JSON格式并传递
        private void execute(StringBuilder pool) {
            //TODO: TCP黏包处理
        }
    }
}
