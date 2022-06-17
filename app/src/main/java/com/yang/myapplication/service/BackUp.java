package com.yang.myapplication.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.yang.myapplication.Activity.BluetoothChat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


public class BackUp {
    private Context context;
    private final Handler handler;


    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";
    private final UUID APP_UUID = UUID.fromString("3eed6b33-2a70-4b11-b05d-6f5e072e2fc7");


    private static UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");


    private final String APP_NAME = "BluetoothChatApp";
    private BluetoothAdapter bluetoothAdapter;
    private ConnectThread connectThread;
    private AcceptThread acceptThread;

    private ConnectedThread mConnectedThread;

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;


    public int state;
    private String TAG = "----->" ;

    public BackUp(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;
        this.state = STATE_NONE;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public int getState() {
        return state;
    }

    public synchronized void setState(int state) {
        this.state = state;
        handler.obtainMessage(BluetoothChat.MESSAGE_STATE_CHANGE,state,-1).sendToTarget();
    }
    public synchronized void start(){
        if(connectThread != null){
            connectThread.cancel();
            connectThread = null;
        }
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if(acceptThread == null){
            acceptThread = new AcceptThread();
            acceptThread.start();
        }
        setState(STATE_LISTEN);

    }

    public synchronized void stop(){
        if(connectThread != null){
            connectThread.cancel();
            connectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if(acceptThread != null){
            acceptThread.cancel();
        }
        setState(STATE_NONE);
    }

    public synchronized void connect(BluetoothDevice device,UUID uuid){
        Log.d(TAG, "connect to: " + device);
        MY_UUID_SECURE = uuid;
        if(state == STATE_CONNECTING){
            if (connectThread != null) {
                connectThread.cancel();
                connectThread = null;
            }
        }
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        connectThread = new ConnectThread(device);
        connectThread.start();
        setState(STATE_CONNECTING);
    }
    public void write(byte[] out) {
        ConnectedThread connectedThread;
        synchronized (this) {
            if (state != STATE_CONNECTED) return;
            connectedThread = mConnectedThread;
        }
        connectedThread.write(out);
    }

    private class AcceptThread extends Thread{
        private BluetoothServerSocket serverSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,MY_UUID_SECURE);
            } catch (IOException e) {
                Log.e("Accept-> Constructor",e.toString());
            }
            serverSocket = tmp;
            setState(STATE_LISTEN);
        }
        public void run(){
            BluetoothSocket socket;
            while (state != STATE_CONNECTED) {
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    Log.e("Accept-> Run",e.toString());
                    try {
                        serverSocket.close();
                        break;
                    } catch (IOException e2) {
                        e2.printStackTrace();
                        Log.e("Accept-> Close",e2.toString());
                        break;
                    }
                }
                if (socket != null){
                    synchronized (BackUp.this) {
                        switch (state){
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                connected(socket,socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    Log.e("Accept-> CloseSocket",e.toString());
                                }
                                break;
                        }
                    }

                }
            }

        }
        public void cancel(){
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("Accept-> CloseServer",e.toString());
            }
        }
    }
    private class ConnectThread extends Thread{
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ConnectThread(BluetoothDevice device){
            this.device = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("Connect-> Constructor",e.toString());
            }
            socket = tmp;
            setState(STATE_CONNECTING);
        }
        public void run(){
            bluetoothAdapter.cancelDiscovery();
            try {
                socket.connect();
            } catch (IOException e) {
//                Log.e("Connect-> Run",e.toString());
                try {
                    socket.close();
                } catch (IOException e2) {
                    Log.e("Connect-> CloseSocket",e2.toString());
                }
                connectionFailed();
                return;
            }
            synchronized (BackUp.this){
                connectThread = null;
            }
            connected(socket,device);
        }
        public void cancel(){
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("Connect-> Cancel",e.toString());
            }
        }

    }



    private synchronized void connectionFailed( ) {
        Message message = handler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothChat.TOAST,"Unable to connect device");
        message.setData(bundle);
        handler.sendMessage(message);
        setState(STATE_NONE);
        BackUp.this.start();
    }

    private void connectionLost() {
        Message msg = handler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothChat.TOAST, "Device connection was lost");
        msg.setData(bundle);
        handler.sendMessage(msg);
        setState(STATE_NONE);
        BackUp.this.start();
    }

    private synchronized void connected(BluetoothSocket socket,BluetoothDevice device) {
        if(connectThread != null){
            connectThread.cancel();
            connectThread = null;
        }
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();


        Message message = handler.obtainMessage(BluetoothChat.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothChat.DEVICE_NAME,device.getName());
        message.setData(bundle);
        handler.sendMessage(message);
        setState(STATE_CONNECTED);
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            state = STATE_CONNECTED;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            while (state == STATE_CONNECTED) {
                try {
                    bytes = mmInStream.read(buffer);

                    handler.obtainMessage(BluetoothChat.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                handler.obtainMessage(BluetoothChat.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
