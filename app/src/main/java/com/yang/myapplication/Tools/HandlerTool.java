package com.yang.myapplication.Tools;

import static android.content.ContentValues.TAG;
import static com.yang.myapplication.Activity.ChatMainActivity.isupdateView;
import static com.yang.myapplication.entity.MessageInfo.DATA_AUDIO;
import static com.yang.myapplication.entity.MessageInfo.DATA_IMAGE;
import static com.yang.myapplication.entity.MessageInfo.DATA_TEXT;

import static com.yang.myapplication.service.ChatUtils.MESSAGE_READ_AUDIO;
import static com.yang.myapplication.service.ChatUtils.MESSAGE_READ_IMAGE;
import static com.yang.myapplication.service.ChatUtils.MESSAGE_READ_MEMBER;
import static com.yang.myapplication.service.ChatUtils.MESSAGE_READ_TEXT;
import static com.yang.myapplication.service.ChatUtils.MESSAGE_WRITE_AUDIO;
import static com.yang.myapplication.service.ChatUtils.MESSAGE_WRITE_IMAGE;
import static com.yang.myapplication.service.ChatUtils.MESSAGE_WRITE_MEMBER;
import static com.yang.myapplication.service.ChatUtils.MESSAGE_WRITE_TEXT;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSONObject;
import com.yang.myapplication.Activity.BluetoothChat;
import com.yang.myapplication.Activity.ChatMainActivity;
import com.yang.myapplication.Activity.GroupChat;
import com.yang.myapplication.entity.MessageInfo;
import com.yang.myapplication.entity.NeighborInfo;
import com.yang.myapplication.entity.Response;
import com.yang.myapplication.http.APIUrl;
import com.yang.myapplication.service.ChatUtils;
import com.yang.myapplication.service.MessageDB;

import org.litepal.LitePal;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rxhttp.RxHttp;

public class HandlerTool {
    public static String connectedDevice;
    public static final int MESSAGE_STATE_CHANGE = 0;
    public static final int MESSAGE_WRITE = 2;
    public static final int MESSAGE_DEVICE_NAME = 3;
    public static final int MESSAGE_TOAST = 4;
    public static final String DEVICE_NAME = "deviceName";
    public static final String DEVICE_ADDRESS = "device_address";
    public static final String TOAST = "toast";
    public static int STATE = -1;
    public static String  localName = null;
    public static String  localMacAddress = null;
    public static Context context = null;
    static public GroupChat chatManager = null;
    public static Bitmap imageBitmap;
    private static final String FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ChatApp/";

    public static void init(List<NeighborInfo> list) {
        if (chatManager == null) {
            chatManager = new GroupChat(list, handler);
            chatManager.setHandler(handler);
        }
        if(list.size() != 0){
            for (NeighborInfo info: list){
                chatManager.newAdd(info);
            }
            chatManager.startConnection(list);
        }
    }

    public static void broadcastNeighbourInfo() {
        chatManager.broadcastNeighbourInfo();
    }


    public static String getConnectedDevice() {
        return connectedDevice;
    }

    public static void setConnectedDevice(String connectedDevice) {
        HandlerTool.connectedDevice = connectedDevice;
    }

    public static String getLocalName() {
        return localName;
    }

    public static void setLocalName(String localName) {
        HandlerTool.localName = localName;
    }

    public static Context getContext() {
        return context;
    }

    public static String getLocalMacAddress() {
        return localMacAddress;
    }

    public static void setLocalMacAddress(String localMacAddress) {
        HandlerTool.localMacAddress = localMacAddress;
    }

    public static String WhichPage = "MAIN";

    public static String getWhichPage() {
        return WhichPage;
    }

    public static void setWhichPage(String whichPage) {
        WhichPage = whichPage;
    }

    public static void setContext(Context context) {
        HandlerTool.context = context;
    }

    public static GroupChat getChatManager() {
        return chatManager;
    }

    public static void setChatManager(GroupChat chatManager) {
        HandlerTool.chatManager = chatManager;
    }

    static final SimpleDateFormat sdf = new SimpleDateFormat("y-MM-dd HH:mm:ss");

    static private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


    private static boolean storeRecord(  byte[] readBuf, String uuid){
        String filename = ChatMainActivity.getFilename(uuid);
        FileOutputStream fos;
        try {
            if (filename != null) {
                fos = new FileOutputStream(filename);
                fos.write(readBuf);
                fos.flush();
                fos.close();
            }
        } catch (Exception e) {
            Toast.makeText(context, "Could not save the file",
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Could not save the file", e);
            return false;
        }
        return true;

    }
    public static HashMap<String,Object> tmpMessageInfo = new HashMap<>();


    private static void formMessage(String buffer ,Set<BluetoothDevice> set){
        HashMap<String,Object> messageInfo = JsonUtils.jsonToPojo(buffer,HashMap.class);
        int dataType = Integer.parseInt(messageInfo.get("dataType").toString());
        File audioFile = (File) messageInfo.get("audioFile");
        Object content = messageInfo.get("content");
        int isRead = Integer.parseInt(messageInfo.get("isRead").toString());
        int isUpload = Integer.parseInt(messageInfo.get("isUpload").toString());
        String messageC = messageInfo.get("message").toString();
        String readDate = messageInfo.get("readDate").toString();
        String routeList = messageInfo.get("routeList").toString();
        String sendDate = messageInfo.get("sendDate").toString();
        String sourceMAC = messageInfo.get("sourceMAC").toString();
        String sourceName = messageInfo.get("sourceName").toString();
        String targetMAC = messageInfo.get("targetMAC").toString();
        String targetName = messageInfo.get("targetName").toString();
        String uuid = messageInfo.get("uuid").toString();
        MessageInfo info = new MessageInfo(uuid,  content,  messageC,  sendDate,  readDate,  isRead,  isUpload, imageBitmap,  audioFile,  dataType,  targetName,  targetMAC,  sourceName, sourceMAC,  routeList);

        String[] routers = RouterTool.routerList(routeList);
        String api = APIUrl.APICloudSendUnreadMessage;
        JSONObject jsonObject = new JSONObject();
        Calendar calendar = Calendar.getInstance();
        String uploadTime = sdf.format(calendar.getTime());
        jsonObject.put("uploadName", localName);
        jsonObject.put("uploadMAC", localMacAddress);
        jsonObject.put("uploadTime", uploadTime);
        jsonObject.put("sendDate", sendDate);
        jsonObject.put("dataType", dataType);
        jsonObject.put("targetName", targetName);
        jsonObject.put("targetMAC", targetMAC);
        jsonObject.put("sourceName", sourceName);
        jsonObject.put("sourceMAC", sourceMAC);
        jsonObject.put("uuid", uuid);

        if(isUpload == 1 && targetName.equals(localName)) {
            Calendar calTest = Calendar.getInstance();
            System.out.println(localName+" 到达目标设备");
            readDate = sdf.format(calTest.getTime());
            jsonObject.put("readDate", readDate);
            RxHttp.postJson(api).addAll(jsonObject.toString())
                    .asClass(Response.class)
                    .subscribe(data -> {
                        if (data.getCode() == 0) {
                            info.setIsUpload(1);
                        } else {
                            info.setIsUpload(0);
                        }
                    }, throwable -> {
                        info.setIsUpload(0);
                    });

        }
        if(isUpload == 0) {
            jsonObject.put("content", messageC);
            if (targetName.equals(localName) && info.getIsRead() == 0) {
                Calendar calTest = Calendar.getInstance();
                readDate = sdf.format(calTest.getTime());
            } else if (info.getIsRead() == 1) {
            }
            jsonObject.put("readDate", readDate);
            RxHttp.postJson(api).addAll(jsonObject.toString())
                    .asClass(Response.class)
                    .subscribe(data -> {
                        if (data.getCode() == 0) {
                            info.setIsUpload(1);
                        } else {
                            info.setIsUpload(0);
                        }
                    }, throwable -> {
                        info.setIsUpload(0);
                    });
        }

        if(targetName.equals(localName)){
            if(info.getIsRead()==0) {
                info.setReadDate(readDate);
                info.setIsRead(1);
                if (MessageDB.storeMessageEachTime(info)) {
                    BluetoothChat.isupdateNeiView = true;
                }
                //ACK send

                List<String> newRouter = new ArrayList<>();
                for (int i = routers.length - 1; i >= 0; i--) {
                    newRouter.add(routers[i].trim());
                }
                if(newRouter.size()>2){
                    System.out.println("ssss"+newRouter.toString());
                    NeighborInfo tmp = new NeighborInfo(info.getSourceMAC(), info.getSourceName(), newRouter.size()-1, new Date(), newRouter.toString().trim());
                    LitePal.deleteAll(NeighborInfo.class, "neighborMac = ? and neighborName = ?", info.getSourceMAC(), info.getSourceName());
                    tmp.save();
                    BluetoothChat.isupdateNeiView = true;
                }
                routers = RouterTool.routerList(newRouter.toString().trim());
                for (int i = 0; i < routers.length; i++) {
                    String path = routers[i].trim();
                    if (path.equals(localName)) {
                        info.setRouteList(newRouter.toString());
                        switch (dataType){
                            case DATA_TEXT:
                                break;
                            case DATA_AUDIO:
                                info.setContent("");
                                info.setMessage("");
                                break;
                            case DATA_IMAGE:
                                info.setContent("");
                                info.setMessage("");
                                break;
                        }
                        chatManager.sendMessage(info, DATA_TEXT, routers[i + 1].trim(), set);
                        break;
                    }
                }
            }
        }else {
            //next
            if (info.getIsRead() == 1 && !readDate.equals("END")) {
                if (sourceName.equals(localName)) {
                    MessageInfo update = new MessageInfo();
                    update.setReadDate(readDate);
                    update.setIsRead(1);
                    update.setIsUpload(info.getIsUpload());
                    update.updateAll("uuid = ?", uuid);
                    BluetoothChat.isupdateNeiView = true;
                    long endTime =  calendar.getTimeInMillis();

                    System.out.println("ACK到达   "+  Math.abs(endTime - ChatUtils.startTime)/1000.0);
                } else {
                    for (int i = 0; i < routers.length; i++) {
                        String path = routers[i].trim();
                        if (path.equals(localName)) {
                            chatManager.sendMessage(info, DATA_TEXT, routers[i + 1].trim(), set);
                            break;
                        }
                    }
                }
            } else {

                for (int i = 0; i < routers.length; i++) {
                    String path = routers[i].trim();
                    if (path.equals(localName)) {
                        switch (dataType){
                            case DATA_TEXT:
                                chatManager.sendMessage(info, DATA_TEXT, routers[i + 1].trim(), set);
                                break;
                            case DATA_AUDIO:
                                chatManager.sendMessage(info, DATA_AUDIO, routers[i + 1].trim(), set);
                                break;
                            case DATA_IMAGE:
                                chatManager.sendMessage(info, DATA_IMAGE, routers[i + 1].trim(), set);
                                break;
                        }
                        break;
                    }
                }
            }
        }
    }
    public static Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            Message msg = new Message();
            switch (message.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (message.arg1) {
                        case ChatUtils.STATE_NONE:
                            STATE = ChatUtils.STATE_NONE;
                            break;
                        case ChatUtils.STATE_LISTEN:
                            STATE = ChatUtils.STATE_LISTEN;
                            break;
                        case ChatUtils.STATE_CONNECTING:
                            STATE = ChatUtils.STATE_CONNECTING;
                            break;
                        case ChatUtils.STATE_CONNECTED:
                            STATE = ChatUtils.STATE_CONNECTED;
                            BluetoothChat.isupdateNeiView = true;
                            break;
                    }
                    break;
                case MESSAGE_WRITE_MEMBER:
                    break;
                case MESSAGE_WRITE_TEXT:
                    MessageInfo textWriteInstance = (MessageInfo) message.obj;
                    if(textWriteInstance.getSourceName() != null ) {
                        if (textWriteInstance.getSourceName().equals(localName)) {
                            if (MessageDB.storeMessageEachTime(textWriteInstance)) {
                                System.out.println("MESSAGE_WRITE_TEXT 自动update");

                            }
                        }
                    }
                    break;

                case MESSAGE_WRITE_AUDIO:
                    textWriteInstance = (MessageInfo) message.obj;
                    if(textWriteInstance.getSourceName() != null ) {
                        if (textWriteInstance.getSourceName().equals(localName)) {
                            if(textWriteInstance.getIsRead() == 0) {
                                String fileName = ChatMainActivity.getFileName();
                                File f = new File(fileName);
                                textWriteInstance.setMessage(f.toString());
                            }
                            if (MessageDB.storeMessageEachTime(textWriteInstance)) {
                                System.out.println("MESSAGE_WRITE_AUDIO 自动update");

                            }
                        }
                    }
                    break;
                case MESSAGE_WRITE_IMAGE:
                    MessageInfo writeImage = (MessageInfo) message.obj;
                    if(writeImage.getSourceName() != null ) {
                        if (writeImage.getSourceName().equals(localName)) {
                            if (MessageDB.storeMessageEachTime(writeImage)) {
                                System.out.println("图片更新");

                            }
                        }
                    }
                    break;
                case MESSAGE_READ_AUDIO:
                    String readAudio = String.valueOf(message.obj);
                    Set<BluetoothDevice>  set = bluetoothAdapter.getBondedDevices();
                    formMessage(readAudio,set);
                    break;
                case MESSAGE_READ_IMAGE:
                    String readImage = String.valueOf(message.obj);
                    set = bluetoothAdapter.getBondedDevices();
                    formMessage(readImage,set);
                    break;
                case MESSAGE_READ_TEXT:
                    String readText = String.valueOf(message.obj);
                    set = bluetoothAdapter.getBondedDevices();
                    formMessage(readText,set);
                    break;
                case MESSAGE_READ_MEMBER:
//                    if(true) break;
//                    byte[]  memberBuf = (byte[]) message.obj;
                    String inputmemberBuf = String.valueOf(message.obj);
//                    String inputmemberBuf = new String(memberBuf, 0, message.arg1);
                    set = bluetoothAdapter.getBondedDevices();

                    if(inputmemberBuf.contains("neighborName")&&inputmemberBuf.contains("neighborMac")) {
                        HashSet<String> seen = new HashSet<>();
                        for(BluetoothDevice d : set){
                            seen.add(d.getName());
                        }
                        List<HashMap> mapList = JsonUtils.jsonToList(inputmemberBuf, HashMap.class);
                        for (HashMap map : mapList) {
                            String neighborMac = map.get("neighborMac").toString();
                            String neighborName = map.get("neighborName").toString();
                            if (neighborName.equals(localName) || seen.contains(neighborName)) continue;
                            String path = map.get("path").toString();
                            String[] routers = RouterTool.routerList(path);
                            List<String> newRouter = new ArrayList<>();
                            newRouter.add(localName);
                            for (int i = 0; i < routers.length; i++) {
                                newRouter.add(routers[i].trim());
                            }
//                            System.out.println("llll"+newRouter.toString());
                            LitePal.deleteAll(NeighborInfo.class, "neighborMac = ? and neighborName = ?", neighborMac, neighborName);
                            NeighborInfo tmp = new NeighborInfo(neighborMac, neighborName, newRouter.size()-1, new Date(), newRouter.toString().trim());
                            tmp.save();
                            BluetoothChat.isupdateNeiView = true;

                        }
//                        List<NeighborInfo> list = LitePal.findAll(NeighborInfo.class);
//                        if(list.size()>1){
//                            chatManager.sendNeighbour(list);
//                        }
                        break;
                    }
                    break;

                case MESSAGE_DEVICE_NAME:
                    connectedDevice = message.getData().getString(DEVICE_NAME);
                    String deviceAddress = message.getData().getString(DEVICE_ADDRESS);
                    chatManager.setConnectionAsTrue(deviceAddress);
                    if (chatManager.isConnectedToAll()) {
                        Log.d(TAG, "Connected to all in group");
                    }
                    Log.d(TAG, "current"+connectedDevice);
                    if(getWhichPage().equals("MAIN")){
                        Toast.makeText(context, connectedDevice, Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(context, connectedDevice, Toast.LENGTH_SHORT).show();
                    }

                    break;
                case MESSAGE_TOAST:
                    Log.d(TAG, "MESSAGE_TOAST"+ message.getData());
                     BluetoothChat.isupdateNeiView = true;
                    if(getWhichPage().equals("MAIN")){
                        Toast.makeText(context, message.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(context, message.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
            return true;
        }
    });

    /**
     * @param base64Data
     * @return
     */
    public static Bitmap base64ToBitmap(String base64Data) {
        byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
    public static Bitmap base64ToBitmapOption(String base64Data) {
        byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inSampleSize = calculateInSampleSize(options, 100, 100);
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
    /*
     * bitmap  ---> base64
     * */
    public static String bitmapToBase64(Bitmap bitmap) {
        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                baos.flush();
                baos.close();
                byte[] bitmapBytes = baos.toByteArray();
                result = Base64.encodeToString(bitmapBytes, Base64.DEFAULT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }



}
