//package com.yang.myapplication.service;
//import static com.yang.myapplication.Activity.BluetoothChat.MESSAGE_TOAST;
//import static com.yang.myapplication.entity.MessageInfo.DATA_IMAGE;
//import static com.yang.myapplication.entity.MessageInfo.DATA_TEXT;
//import static com.yang.myapplication.entity.MessageInfo.DATA_AUDIO;
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothDevice;
//import android.bluetooth.BluetoothServerSocket;
//import android.bluetooth.BluetoothSocket;
//import android.content.Context;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Message;
//import android.util.Base64;
//import android.util.Log;
//
//import com.alibaba.fastjson.JSONObject;
//import com.yang.myapplication.Activity.BluetoothChat;
//import com.yang.myapplication.Activity.ChatMainActivity;
//import com.yang.myapplication.Tools.HandlerTool;
//import com.yang.myapplication.Tools.JsonUtils;
//import com.yang.myapplication.Tools.RandomID;
//import com.yang.myapplication.entity.MessageInfo;
//import com.yang.myapplication.entity.NeighborInfo;
//import com.yang.myapplication.entity.Response;
//import com.yang.myapplication.http.APIUrl;
//
//import org.json.JSONException;
//
//import java.io.BufferedInputStream;
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.DataInputStream;
//import java.io.DataOutputStream;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.ObjectInputStream;
//import java.io.OutputStream;
//import java.nio.ByteBuffer;
//import java.nio.charset.Charset;
//import java.nio.charset.StandardCharsets;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Calendar;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.UUID;
//
//import rxhttp.RxHttp;
//
//
//public class ChatUtils {
//    private Context context;
//    private final Handler handler;
//
//    private static final String NAME_SECURE = "BluetoothChatSecure";
//    private static final String NAME_INSECURE = "BluetoothChatInsecure";
//
//
//
//    private static final UUID MY_UUID = UUID.fromString("188c5bda-d1b6-464a-8074-c5deaad3fa36");
//
//    private static UUID MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
//
//    private static UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
//
//    private final String APP_NAME = "BluetoothChatApp";
//    private BluetoothAdapter bluetoothAdapter;
//    private ChatUtils.ConnectThread connectThread;
//    private ChatUtils.AcceptThread acceptThread;
//    private ChatUtils.ConnectedThread mConnectedThread;
//
//    public static final int STATE_NONE = 0;
//    public static final int STATE_LISTEN = 1;
//    public static final int STATE_CONNECTING = 2;
//    public static final int STATE_CONNECTED = 3;
//    public static final int TRANSFER = 4;
//
//
//    public static final int MESSAGE_READ = 1;
//    public static final int MESSAGE_READ_IMAGE = 6;
//    public static final int MESSAGE_READ_AUDIO = 7;
//    public static final int MESSAGE_READ_TEXT = 8;
//
//    public static final int MESSAGE_WRITE_IMAGE = 9;
//    public static final int MESSAGE_WRITE_AUDIO = 10;
//    public static final int MESSAGE_WRITE_TEXT = 11;
//
//    public static final int MESSAGE_WRITE_MEMBER = 12;
//    public static final int MESSAGE_READ_MEMBER = 13;
//
//    public int state;
//    private String TAG = "ChatUtils" ;
//    private String MAC = null;
//    private String name = null;
//
//    public ChatUtils(Context context, Handler handler) {
//        this.context = context;
//        this.handler = handler;
//        this.state = STATE_NONE;
//        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//    }
//
//    public ChatUtils(Handler handler) {
//        this.handler = HandlerTool.handler;
//        this.state = STATE_NONE;
//        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//    }
//    public int getState() {
//        return state;
//    }
//
//    public synchronized void setState(int state) {
//        this.state = state;
//        handler.obtainMessage(BluetoothChat.MESSAGE_STATE_CHANGE,state,-1).sendToTarget();
//    }
//    public synchronized void start(){
//        if(connectThread != null){
//            connectThread.cancel();
//            connectThread = null;
//        }
//        // Cancel any thread currently running a connection
//        if (mConnectedThread != null) {
//            mConnectedThread.cancel();
//            mConnectedThread = null;
//        }
//
//        if(acceptThread == null){
//            acceptThread = new AcceptThread();
//            acceptThread.start();
//        }
//        setState(STATE_LISTEN);
//
//    }
//
//    public String getMAC() {
//        return MAC;
//    }
//
//    public void setMAC(String MAC) {
//        this.MAC = MAC;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    public synchronized void stop(){
//        if(connectThread != null){
//            connectThread.cancel();
//            connectThread = null;
//        }
//        if (mConnectedThread != null) {
//            mConnectedThread.cancel();
//            mConnectedThread = null;
//        }
//        if(acceptThread != null){
//            acceptThread.cancel();
//        }
//        setState(STATE_NONE);
//    }
//    private String label = "";
//
//    public static Map<String, ConnectThread> connectThreads = new HashMap<>();
//
//
//    public synchronized void connect(BluetoothDevice device, String tips){
//        Log.d(TAG, "connect to: " + device) ;
//        label = tips;
//        if(state == STATE_CONNECTING) {
//            if (connectThread != null) {
//                connectThread.cancel();
//                connectThread = null;
//            }
//        }
//        // Cancel any thread currently running a connection
//        if (mConnectedThread != null) {
//            mConnectedThread.cancel();
//            mConnectedThread = null;
//        }
//        connectThread = new ConnectThread(device);
//        connectThread.start();
//        setState(STATE_CONNECTING);
//    }
//    public void write(byte[] out) {
//        ChatUtils.ConnectedThread connectedThread;
//        synchronized (this) {
//            if (state != STATE_CONNECTED) return;
//            connectedThread = mConnectedThread;
//        }
//        connectedThread.write(out);
//    }
//
//    public void write(byte[] out, int datatype, String timeSent, NeighborInfo currentConnectDevice, String localName, String localMacAddress, int isUpload, String txtWriteTime) {
//        // Create temporary object
//        ChatUtils.ConnectedThread connectedThread;
//        // Synchronize a copy of the ConnectedThread
//        synchronized (this) {
//            if (state != STATE_CONNECTED) return;
//            connectedThread = mConnectedThread;
//        }
//        // Perform the write unsynchronized
//        connectedThread.write(out, datatype, timeSent,currentConnectDevice,localName,localMacAddress,isUpload,txtWriteTime);
//    }
//
//    public void write(MessageInfo out, int dataType) {
//        ChatUtils.ConnectedThread connectedThread;
//        synchronized (this) {
//            if (state != STATE_CONNECTED) return;
//            connectedThread = mConnectedThread;
//        }
//        connectedThread.write(out, dataType);
//    }
//
//    public void write(List<NeighborInfo> out) {
//        // Create temporary object
//        ChatUtils.ConnectedThread connectedThread;
//        // Synchronize a copy of the ConnectedThread
//        synchronized (this) {
//            if (state != STATE_CONNECTED) return;
//            connectedThread = mConnectedThread;
//        }
//        // Perform the write unsynchronized
//        connectedThread.write(out);
//    }
//
//    private class AcceptThread extends Thread{
//        private BluetoothServerSocket serverSocket;
//
//        public AcceptThread() {
//            BluetoothServerSocket tmp = null;
//            try {
//                tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME_SECURE, MY_UUID);
//
////                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,MY_UUID_SECURE);
//            } catch (IOException e) {
//                Log.e("Accept-> Constructor",e.toString());
//            }
//            serverSocket = tmp;
//            setState(STATE_LISTEN);
//        }
//        public void run(){
//            BluetoothSocket socket;
//            while (state != STATE_CONNECTED) {
//                try {
//                    socket = serverSocket.accept();
//                } catch (IOException e) {
//                    Log.e("Accept-> Run",e.toString());
//                    try {
//                        serverSocket.close();
//                        break;
//                    } catch (IOException e2) {
//                        e2.printStackTrace();
//                        Log.e("Accept-> Close",e2.toString());
//                        break;
//                    }
//                }
//                if (socket != null){
//                    synchronized (ChatUtils.this) {
//                        switch (state){
//                            case STATE_LISTEN:
//                            case STATE_CONNECTING:
//                                connected(socket,socket.getRemoteDevice());
//                                break;
//                            case STATE_NONE:
//                            case STATE_CONNECTED:
//                                try {
//                                    socket.close();
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                    Log.e("Accept-> CloseSocket",e.toString());
//                                }
//                                break;
//                        }
//                    }
//                }
//            }
//
//        }
//        public void cancel(){
//            try {
//                serverSocket.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//                Log.e("Accept-> CloseServer",e.toString());
//            }
//        }
//    }
//    private class ConnectThread extends Thread{
//        private final BluetoothSocket socket;
//        private final BluetoothDevice device;
//
//        public ConnectThread(BluetoothDevice device){
//            this.device = device;
//            BluetoothSocket tmp = null;
//            try {
//                tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
//
////                tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
//            } catch (IOException e) {
//                e.printStackTrace();
//                Log.e("Connect-> Constructor",e.toString());
//            }
//            socket = tmp;
//            setState(STATE_CONNECTING);
//        }
//        public void run(){
//            bluetoothAdapter.cancelDiscovery();
//            try {
//                socket.connect();
//            } catch (IOException e) {
//                Log.e("Connect-> Run",e.toString());
//                try {
//                    socket.close();
//                } catch (IOException e2) {
//                    Log.e("Connect-> CloseSocket",e2.toString());
//                }
//                connectionFailed();
//                return;
//            }
//            synchronized (ChatUtils.this){
//                connectThread = null;
//            }
//            connected(socket,device);
//        }
//        public void cancel(){
//            try {
//                socket.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//                Log.e("Connect-> Cancel",e.toString());
//            }
//        }
//
//    }
//
//
//
//    private synchronized void connectionFailed( ) {
//        Message message = handler.obtainMessage(MESSAGE_TOAST);
//        Bundle bundle = new Bundle();
//        bundle.putString(BluetoothChat.TOAST,"Unable to connect device");
//        message.setData(bundle);
//        handler.sendMessage(message);
//        setState(STATE_NONE);
//        ChatUtils.this.start();
//    }
//
//    private void connectionLost() {
//        Message msg = handler.obtainMessage(MESSAGE_TOAST);
//        Bundle bundle = new Bundle();
//        bundle.putString(BluetoothChat.TOAST, "Device connection was lost");
//        msg.setData(bundle);
//        handler.sendMessage(msg);
//        setState(STATE_NONE);
//        ChatUtils.this.start();
//    }
//
//    private synchronized void connected(BluetoothSocket socket,BluetoothDevice device) {
//        if(connectThread != null){
//            connectThread.cancel();
//            connectThread = null;
//        }
//        if (acceptThread != null) {
//            acceptThread.cancel();
//            acceptThread = null;
//        }
//        // Cancel any thread currently running a connection
//        if (mConnectedThread != null) {
//            mConnectedThread.cancel();
//            mConnectedThread = null;
//        }
//
//        mConnectedThread = new ConnectedThread(socket);
//        mConnectedThread.start();
//
//
//        Message message = handler.obtainMessage(BluetoothChat.MESSAGE_DEVICE_NAME);
//        Bundle bundle = new Bundle();
//        bundle.putString(BluetoothChat.DEVICE_NAME,device.getName());
//        bundle.putString(BluetoothChat.DEVICE_ADDRESS, device.getAddress());
//        name = device.getName();
//        MAC = device.getAddress();
//        message.setData(bundle);
//        handler.sendMessage(message);
//        setState(STATE_CONNECTED);
//    }
//    final SimpleDateFormat sdf = new SimpleDateFormat("y-MM-dd HH:mm:ss");
//
//
//    /**
//     * This thread runs during a connection with a remote device.
//     * It handles all incoming and outgoing transmissions.
//     */
//    private class ConnectedThread extends Thread {
//        private final BluetoothSocket mmSocket;
//        private final InputStream mmInStream;
//        private final OutputStream mmOutStream;
//        private final DataOutputStream OutData;
//        private final DataInputStream inData;
//        public ConnectedThread(BluetoothSocket socket) {
//            mmSocket = socket;
//            InputStream tmpIn = null;
//            OutputStream tmpOut = null;
//
//            // Get the BluetoothSocket input and output streams
//            try {
//                tmpIn = socket.getInputStream();
//                tmpOut = socket.getOutputStream();
//            } catch (IOException e) {
//                Log.e(TAG, "temp sockets not created", e);
//            }
//
//            mmInStream = tmpIn;
//            mmOutStream = tmpOut;
//            state = STATE_CONNECTED;
//        }
//        public void run() {
//            int bytes = 0;
//            int BUFFER_SIZE = 16384 ;
//            byte[] bufferData = new byte[BUFFER_SIZE];
//
//
//
//            while (state == STATE_CONNECTED) {
//                try {
//
//
//                    bytes = mmInStream.read(bufferData);
////                    handler.obtainMessage(MESSAGE_READ_AUDIO, bytes, -1, bufferData).sendToTarget();
////                    System.out.println("截断："+ bytes);
////                    byte[] trimmedBufferData = Arrays.copyOf(bufferData, bytes);
////                    if(trimmedBufferData[0] == 0X00 && trimmedBufferData[1] == 0X00 && trimmedBufferData[2] == 0X00 && trimmedBufferData[3] == 0x18  ){
////                        System.out.println("Read");
//////                        handler.obtainMessage(MESSAGE_READ_AUDIO, bytes, -1, bufferData).sendToTarget();
////                    }
////                    String inputBuffer = new String(trimmedBufferData, 0, bytes);
////                    if(inputBuffer.contains("{") && inputBuffer.contains("}")) {
////                        if(inputBuffer.contains("neighborName")&&inputBuffer.contains("neighborMac")) {
////                            handler.obtainMessage(MESSAGE_READ_MEMBER, bytes, -1, bufferData).sendToTarget();
////                            return;
////                        }
////                        int start = inputBuffer.indexOf("{");
////                        int end = inputBuffer.indexOf("}")+1;
////                        String newBuffer = inputBuffer.substring(start,end);
////                        HashMap<String,Object> messageInfo = JsonUtils.jsonToPojo(newBuffer,HashMap.class);
////                        int dataType = Integer.parseInt(messageInfo.get("dataType").toString());
////                        if(dataType == DATA_TEXT){
////                            handler.obtainMessage(MESSAGE_READ, bytes, -1, bufferData).sendToTarget();
////                        }else if(dataType == DATA_AUDIO){
////                            HandlerTool.tmpMessageInfo = messageInfo;
////                        }
////                    }else {
////                        System.out.println("字节流："+ inputBuffer);
////                        handler.obtainMessage(MESSAGE_READ_AUDIO, bytes, -1, bufferData).sendToTarget();
////                    }
//
//
//
////                    System.out.println(inputBuffer);
////                    if(bytes == 990){
////                        if(inputBuffer.contains("neighborName")&&inputBuffer.contains("neighborMac")) {
////                            handler.obtainMessage(MESSAGE_READ_MEMBER, bytes, -1, bufferData).sendToTarget();
////                            return;
////                        }
////                        if(!inputBuffer.contains("{") || !inputBuffer.contains("}")) return;
////                        int start = inputBuffer.indexOf("{");
////                        int end = inputBuffer.indexOf("}")+1;
////                        String newBuffer = inputBuffer.substring(start,end);
////                        HandlerTool.tmpMessageInfo =  JsonUtils.jsonToPojo(newBuffer,HashMap.class);
////                    }else if(bytes > 990){
////                        if(!inputBuffer.contains("{") || !inputBuffer.contains("}")) return;
//////                        handler.obtainMessage(MESSAGE_READ_AUDIO, bytes, -1, bufferData).sendToTarget();
////                    } else {
////
////                        if(inputBuffer.contains("neighborName")&&inputBuffer.contains("neighborMac")) {
////                            handler.obtainMessage(MESSAGE_READ_MEMBER, bytes, -1, bufferData).sendToTarget();
////                        }else {
////
////                        }
////                    }
//                } catch (IOException e) {
//                    Log.e(TAG, "disconnected", e);
//                    connectionLost();
//                    break;
//                }
//            }
//
//
////            while (state == STATE_CONNECTED) {
////                try {
////                    bytes = mmInStream.read(bufferData);
////
////
////                    handler.obtainMessage(MESSAGE_READ_AUDIO, bytes, -1, bufferData).sendToTarget();
////                    System.out.println("截断："+ bytes);
//////                    byte[] trimmedBufferData = Arrays.copyOf(bufferData, bytes);
//////                    if(trimmedBufferData[0] == 0X00 && trimmedBufferData[1] == 0X00 && trimmedBufferData[2] == 0X00 && trimmedBufferData[3] == 0x18  ){
//////                        System.out.println("Read");
////////                        handler.obtainMessage(MESSAGE_READ_AUDIO, bytes, -1, bufferData).sendToTarget();
//////                    }
//////                    String inputBuffer = new String(trimmedBufferData, 0, bytes);
//////                    if(inputBuffer.contains("{") && inputBuffer.contains("}")) {
//////                        if(inputBuffer.contains("neighborName")&&inputBuffer.contains("neighborMac")) {
//////                            handler.obtainMessage(MESSAGE_READ_MEMBER, bytes, -1, bufferData).sendToTarget();
//////                            return;
//////                        }
//////                        int start = inputBuffer.indexOf("{");
//////                        int end = inputBuffer.indexOf("}")+1;
//////                        String newBuffer = inputBuffer.substring(start,end);
//////                        HashMap<String,Object> messageInfo = JsonUtils.jsonToPojo(newBuffer,HashMap.class);
//////                        int dataType = Integer.parseInt(messageInfo.get("dataType").toString());
//////                        if(dataType == DATA_TEXT){
//////                            handler.obtainMessage(MESSAGE_READ, bytes, -1, bufferData).sendToTarget();
//////                        }else if(dataType == DATA_AUDIO){
//////                            HandlerTool.tmpMessageInfo = messageInfo;
//////                        }
//////                    }else {
//////                        System.out.println("字节流："+ inputBuffer);
//////                        handler.obtainMessage(MESSAGE_READ_AUDIO, bytes, -1, bufferData).sendToTarget();
//////                    }
////
////
////
//////                    System.out.println(inputBuffer);
//////                    if(bytes == 990){
//////                        if(inputBuffer.contains("neighborName")&&inputBuffer.contains("neighborMac")) {
//////                            handler.obtainMessage(MESSAGE_READ_MEMBER, bytes, -1, bufferData).sendToTarget();
//////                            return;
//////                        }
//////                        if(!inputBuffer.contains("{") || !inputBuffer.contains("}")) return;
//////                        int start = inputBuffer.indexOf("{");
//////                        int end = inputBuffer.indexOf("}")+1;
//////                        String newBuffer = inputBuffer.substring(start,end);
//////                        HandlerTool.tmpMessageInfo =  JsonUtils.jsonToPojo(newBuffer,HashMap.class);
//////                    }else if(bytes > 990){
//////                        if(!inputBuffer.contains("{") || !inputBuffer.contains("}")) return;
////////                        handler.obtainMessage(MESSAGE_READ_AUDIO, bytes, -1, bufferData).sendToTarget();
//////                    } else {
//////
//////                        if(inputBuffer.contains("neighborName")&&inputBuffer.contains("neighborMac")) {
//////                            handler.obtainMessage(MESSAGE_READ_MEMBER, bytes, -1, bufferData).sendToTarget();
//////                        }else {
//////
//////                        }
//////                    }
////                } catch (IOException e) {
////                    Log.e(TAG, "disconnected", e);
////                    connectionLost();
////                    break;
////                }
////            }
//        }
//
//        public void write(byte[] buffer) {
//            try {
//                mmOutStream.write(buffer);
//                handler.obtainMessage(BluetoothChat.MESSAGE_WRITE, -1, -1, buffer)
//                        .sendToTarget();
//            } catch (IOException e) {
//                Log.e(TAG, "Exception during write", e);
//            }
//        }
//
//        public static final int MESSAGE_WRITE_IMAGE = 9;
//        public static final int MESSAGE_WRITE_AUDIO = 10;
//        public static final int MESSAGE_WRITE_TEXT = 11;
//
//
//        public void write(byte[] bytes, int datatype, String timeSent,NeighborInfo currentConnectDevice, String localName, String localMacAddress, int isUpload, String txtWriteTime) {
//            try {
//                Message writtenMsg = null;
//                MessageInfo dataSent = new MessageInfo();
//                String uuid = String.valueOf(RandomID.genIDWorker());
//                dataSent.setUuid(uuid);
//                dataSent.setSourceName(localName);
//                dataSent.setIsUpload(isUpload);
//                dataSent.setSendDate(txtWriteTime);
//                dataSent.setIsRead(0);
//                dataSent.setSourceMAC(localMacAddress);
//                dataSent.setTargetMAC(currentConnectDevice.getNeighborMac());
//                dataSent.setTargetName(currentConnectDevice.getNeighborName());
//                dataSent.setRouteList(currentConnectDevice.getPath());
//
//                String api = APIUrl.APICloudSendUnreadMessage;
//                Calendar calendar = Calendar.getInstance();
//                String uploadTime = sdf.format(calendar.getTime());
//                JSONObject jsonObject = new JSONObject();
//                jsonObject.put("uploadName", localName);
//                jsonObject.put("uploadMAC", localMacAddress);
//                jsonObject.put("uploadTime", uploadTime);
//                jsonObject.put("uuid", dataSent.getUuid());
//                jsonObject.put("sendDate", dataSent.getSendDate());
//                jsonObject.put("readDate", dataSent.getReadDate());
//                jsonObject.put("targetName", dataSent.getTargetName());
//                jsonObject.put("targetMAC", dataSent.getTargetMAC());
//                jsonObject.put("sourceName", dataSent.getSourceName());
//                jsonObject.put("sourceMAC", dataSent.getSourceMAC());
//                jsonObject.put("dataType", datatype);
//                dataSent.setDataType(datatype);
//                if (datatype == DATA_IMAGE) {
//                    System.out.println("IMAGE WRITE");
//                    ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
//                    imageStream.write(bytes);
//                    String decodedString = new String(imageStream.toByteArray(),
//                            Charset.defaultCharset());
//                    byte[] decodedStringArray = Base64.decode(decodedString, Base64.DEFAULT);
//                    Bitmap bp = BitmapFactory.decodeByteArray(decodedStringArray,
//                            0, decodedStringArray.length);
//
//                    dataSent.setContent(bp);
//                    dataSent.setMessage("");
//                    jsonObject.put("content", dataSent.getContent());
//                    writtenMsg = handler.obtainMessage(MESSAGE_WRITE_IMAGE, -1, DATA_IMAGE,
//                            dataSent);
//                    imageStream.close();
//                } else if (datatype == DATA_TEXT) {
//                    dataSent.setContent(bytes);
//                    String content = new String(bytes);
//                    dataSent.setMessage(content);
//                    jsonObject.put("content", dataSent.getMessage());
//                    writtenMsg = handler.obtainMessage(MESSAGE_WRITE_TEXT, -1, DATA_TEXT, dataSent);
//                    mmOutStream.write(JsonUtils.objectToJson(dataSent).getBytes());
//
//                }else if (datatype == DATA_AUDIO) {
//                    String fileName = ChatMainActivity.getFileName();
//                    File file = new File(fileName);
//                    dataSent.setMessage(file.toString());
//                    jsonObject.put("content", dataSent.getContent());
//                    writtenMsg = handler.obtainMessage(MESSAGE_WRITE_AUDIO, -1, DATA_AUDIO, dataSent);
//                    byte[] body = JsonUtils.objectToJson(dataSent).getBytes();
//                    byte[] newBytes = addBytes(body,bytes);
//                    System.out.println("字节大小"+bytes.length);
//                    mmOutStream.write();
//                    mmOutStream.write(bytes);
//
////                    mmOutStream.write(bytes);
//                }
//
//                RxHttp.postJson(api).addAll(jsonObject.toString())
//                        .asClass(Response.class)
//                        .subscribe(data -> {
//                            if (data.getCode() == 0) {
//                                dataSent.setIsUpload(1);
//                            }else {
//                                dataSent.setIsUpload(0);
//                            }
//                        }, throwable -> {
//                            dataSent.setIsUpload(0);
//                        });
//
//
//                if (writtenMsg != null) {
//                    writtenMsg.sendToTarget();
//                }
//
//
//            } catch (IOException e) {
//                Log.e(TAG, "Error occurred when sending data", e);
//                Message writeErrorMsg = handler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
//                Bundle bundle = new Bundle();
//                bundle.putString("toast", "Device disconnected. " +
//                        "Couldn't send data to the other device");
//                writeErrorMsg.setData(bundle);
//                handler.sendMessage(writeErrorMsg);
//            }
//        }
//
//        public  byte[] addBytes(byte[] data1, byte[] data2) {
//            byte[] data3 = new byte[1024 + data2.length];
////            System.arraycopy(src, srcPos, dest, destPos, length)
////            src：源字节数组（就是你要从哪个字节数组里截数据）
////            srcPos：开始的位置（0可以，或者src中的某个位置长度）
////            dest：byte[]目标数组（用来存你截出来数据的字节数组）
////            destPos：目标数组开始的位置，空白数组的话可以是0
////            length：截取的数据长度
//            System.arraycopy(data1, 0, data3, 0, data1.length);
//            System.arraycopy(data2, 0, data3, 990, data2.length);
//            return data3;
//        }
//
//        public void write(List<NeighborInfo> dataSent) {
//            try {
//                Message writtenMsg = null;
//                handler.obtainMessage(MESSAGE_WRITE_MEMBER, -1, DATA_IMAGE, dataSent)
//                        .sendToTarget();
//                mmOutStream.write(JsonUtils.objectToJson(dataSent).getBytes());
//                if (writtenMsg != null) {
//                    writtenMsg.sendToTarget();
//                }
//            } catch (IOException e) {
//                Log.e(TAG, "Error occurred when sending data", e);
//                Message writeErrorMsg = handler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
//                Bundle bundle = new Bundle();
//                bundle.putString("toast", "Device disconnected. " +
//                        "Couldn't send data to the other device");
//                writeErrorMsg.setData(bundle);
//                handler.sendMessage(writeErrorMsg);
//            }
//        }
//
//        public void write(MessageInfo dataSent, int datatype) {
//            try {
//                Message writtenMsg = null;
//                if (datatype == DATA_IMAGE) {
//                    handler.obtainMessage(MESSAGE_WRITE_IMAGE, -1, DATA_IMAGE, dataSent)
//                            .sendToTarget();
//                } else if (datatype == DATA_TEXT) {
//                    handler.obtainMessage(MESSAGE_WRITE_TEXT, -1, DATA_TEXT, dataSent)
//                            .sendToTarget();
//
//                } else if (datatype == DATA_AUDIO) {
//                    handler.obtainMessage(MESSAGE_WRITE_AUDIO, -1, DATA_AUDIO, dataSent)
//                            .sendToTarget();
//                }
//                mmOutStream.write(JsonUtils.objectToJson(dataSent).getBytes());
//                if (writtenMsg != null) {
//                    writtenMsg.sendToTarget();
//                }
//
//            } catch (IOException e) {
//                Log.e(TAG, "Error occurred when sending data", e);
//                Message writeErrorMsg = handler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
//                Bundle bundle = new Bundle();
//                bundle.putString("toast", "Device disconnected. " +
//                        "Couldn't send data to the other device");
//                writeErrorMsg.setData(bundle);
//                handler.sendMessage(writeErrorMsg);
//            }
//        }
//
//
//
//
//
//        public void cancel() {
//            try {
//                mmSocket.close();
//            } catch (IOException e) {
//                Log.e(TAG, "close() of connect socket failed", e);
//            }
//        }
//    }
//
//    private void uploadTocloud() {
//
//
//    }
//}
