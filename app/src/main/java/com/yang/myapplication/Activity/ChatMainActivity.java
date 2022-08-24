package com.yang.myapplication.Activity;

import static android.content.ContentValues.TAG;
import static android.os.Environment.getExternalStorageDirectory;
import static com.yang.myapplication.Activity.BluetoothChat.SER_KEY;
import static com.yang.myapplication.entity.MessageInfo.DATA_AUDIO;
import static com.yang.myapplication.entity.MessageInfo.DATA_IMAGE;
import static com.yang.myapplication.entity.MessageInfo.DATA_TEXT;
import static com.yang.myapplication.service.ChatUtils.MESSAGE_WRITE_TEXT;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.yang.myapplication.Adapter.MessageAdapter;
import com.yang.myapplication.R;
import com.yang.myapplication.Tools.HandlerTool;
import com.yang.myapplication.Tools.JsonUtils;
import com.yang.myapplication.Tools.NetworkTool;
import com.yang.myapplication.Tools.RouterTool;
import com.yang.myapplication.entity.DeviceInfo;
import com.yang.myapplication.entity.MessageInfo;
import com.yang.myapplication.entity.NeighborInfo;
import com.yang.myapplication.http.APIUrl;
import com.yang.myapplication.service.ChatUtils;
import com.yang.myapplication.service.MessageDB;

import org.litepal.LitePal;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class ChatMainActivity extends AppCompatActivity {
    private EditText edCreateMessage;
    private Button button_send;
    private ImageButton btnRecord;
    private ListView listMainChat;
    private  MessageAdapter adapterMainChat;
    NeighborInfo currentConnectDevice = null;
    private Context context ;
    static String   localName = null;
    static  String  localMacAddress = null;
    static private BluetoothAdapter bluetoothAdapter = null;
    static final SimpleDateFormat sdf = new SimpleDateFormat("y-MM-dd HH:mm:ss");
    static String nextrouters = null;
    static private GroupChat chatManager = null;

    public static boolean isupdateView = false;
    public List<NeighborInfo> neighborInfos = new ArrayList<>();

    List<BluetoothDevice> pairedDevicesSet = new ArrayList<>();

    private void initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "No Bluetooth found", Toast.LENGTH_SHORT).show();
        }
        enable();
    }
    private void enable() {
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }
        if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3000);
            startActivity(discoverableIntent);
        }
    }
    private void reloadConnectMember(){
        localName = bluetoothAdapter.getName();
        localMacAddress = NetworkTool.getMacAddr();
        currentConnectDevice = (NeighborInfo)getIntent().getSerializableExtra(SER_KEY);
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        pairedDevicesSet.addAll(pairedDevices);
    }
    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "onStart");
        reloadConnectMember();
        reLoadMessageFromDB();
//        Timer timer = new Timer();
//        timer.schedule(new TimerTask() {
//            public void run() {
//
//            }
//        }, 1000,   2000);
    }


    private String target = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate");
        setContentView(R.layout.chat_main);
        context = this;
        initBluetooth();
        listMainChat = findViewById(R.id.list_conversation);
        edCreateMessage = findViewById(R.id.ed_enter_message);
        button_send = findViewById(R.id.button_send);
        btnRecord = findViewById(R.id.btn_record);
        edCreateMessage.clearFocus();
        reloadConnectMember();
        reloadConnectList();
        initConfig() ;
        setState(currentConnectDevice.getNeighborName());
        target = currentConnectDevice.getNeighborName();
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);

        if (chatManager == null) {
            chatManager = BluetoothChat.groupChatManager;
        }
        if (currentConnectDevice.getHop() == 1) {
            chatManager.newAdd(currentConnectDevice);
            nextrouters = currentConnectDevice.getNeighborName();
            setNextrouters(nextrouters);
            chatManager.startConnection(neighborInfos, currentConnectDevice.getNeighborName());
        } else {
            String[] routers = RouterTool.routerList(currentConnectDevice.getPath());
            for (int i = 0; i < routers.length; i++) {
                String path = routers[i].trim();
                if (path.equals(localName)) {
                    nextrouters = routers[i + 1].trim();
                    setNextrouters(nextrouters);
                    chatManager.startConnection(neighborInfos, nextrouters);
                    break;
                }
            }
        }
//        reLoadMessageFromDB();


        button_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = edCreateMessage.getText().toString().trim();
                if (!message.isEmpty() || message != null) {
                    sendGroupMessage(message);
                    edCreateMessage.setText("");
                }
            }
        });


        btnRecord.setOnClickListener(new View.OnClickListener() {
            boolean mStartRecording = true;

            @Override
            public void onClick(View view) {
                onRecord(mStartRecording);
                if (mStartRecording) {
                    btnRecord.setImageResource(R.drawable.ic_stop_black_24dp);
                } else {
                    btnRecord.setImageResource(android.R.drawable.ic_btn_speak_now);
                }
                mStartRecording = !mStartRecording;
            }
        });


        listMainChat.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String target = currentConnectDevice.getNeighborName();
                List<MessageInfo> list = MessageDB.queryAllFromDB(target, localName);
//                MessageInfo msg = (MessageInfo) parent.getItemAtPosition(position);
                MessageInfo msg = list.get(position);
                if (msg.getDataType() == DATA_AUDIO) {
                    File file = null;
                    if (msg.getMessage().contains("ChatApp")) {
                        file = new File(msg.getMessage());
                    } else {
                        String path = getFilename(msg.getMessage());
                        assert path != null;
                        file = new File(path);
                    }
                    mPlayer = MediaPlayer.create(context, Uri.fromFile(file));
                    mPlayer.start();
                }
            }
        });

    }
    private MediaRecorder mRecorder = null;
    private MediaPlayer mPlayer = null;
    public static String  fileName = null;
    private static final int CAMERA_REQUEST = 1888;
    private static final int REQUEST_CONNECT_DEVICE = 3;
    private static final int MY_PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 2;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int SELECT_IMAGE = 11;
    private final static int MAX_IMAGE_SIZE = 200000;

    // record start and stop
    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }
    public static String getFilename(String name) {
        String filepath = getExternalStorageDirectory().getPath();
        File appFolder = new File(filepath, "ChatApp");
        if (!appFolder.exists()) {
            if (!appFolder.mkdirs()) {
                Log.e(TAG,"Could not create App folder. Any activity requiring storage is suspended");
                return null;
            }
        }
        if(name.contains(".mp3")){
            return appFolder.getAbsolutePath() + File.separator + name;
        }else {
            return appFolder.getAbsolutePath() + File.separator + name + ".mp3";
        }
    }

    public static String getFilename() {
        String filepath = getExternalStorageDirectory().getPath();
        File appFolder = new File(filepath, "ChatApp");
        if (!appFolder.exists()) {
            if (!appFolder.mkdirs()) {
                Log.e(TAG,"Could not create App folder. Any activity requiring storage is suspended");
                return null;
            }
        }
        return appFolder.getAbsolutePath() + File.separator + System.currentTimeMillis() + ".mp3";
    }

    public static String getFileName() {
        return fileName;
    }

    public static void setFileName(String file) {
        fileName = file;
    }

    // TODO  Create a new recorder
    private void startRecording() {
        Toast.makeText( context, "Recording started", Toast.LENGTH_SHORT).show();
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        fileName = getFilename();
        mRecorder.setOutputFile(fileName);
        try {
            mRecorder.prepare();
            mRecorder.start();
        } catch (Exception e) {
            Log.e(TAG, "Recording failed", e);
        }
    }

    // TODO Stop the recorder clean cache
    private void stopRecording() {
        Toast.makeText(context, "Recording Stopped", Toast.LENGTH_SHORT).show();
        if (mRecorder != null ) {
            mRecorder.stop();
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            if (chatManager != null) {
                try {
                    File f = new File(fileName);
                    setFileName(fileName);
                    FileInputStream fis = new FileInputStream(fileName);
                    byte[] buff = new byte[(int) f.length()];
                    fis.read(buff);
                    Calendar calendar = Calendar.getInstance();
                    String txtWriteTime = sdf.format(calendar.getTime());
                    nextrouters = getNextrouters();
                    int isUpload = 0;
                    if( buff.length <= 2048){
                        Toast.makeText(context, "Record is too short, please try again", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if(buff.length > 2048*5) {
                        Toast.makeText(context, "Record is too large, please try again", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    chatManager.sendMessage(buff, DATA_AUDIO,currentConnectDevice,
                            localName,localMacAddress,isUpload,txtWriteTime,nextrouters);
                    fis.close();
                } catch (Exception e) {
                    Log.e(TAG, "Could not open stream to save data", e);
                }
            }
        }
    }
//    //take a photo
    public void PhotoMessage(View view) {
        permissionCheck();
    }
    public void CameraPhoto(View view) {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SELECT_IMAGE:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                            byte[] imageSend;
                            try {
                                imageSend = compressBitmap(bitmap, true);
                            } catch (NullPointerException e) {
                                Log.d(TAG, "Image cannot be compressed");
                                Toast.makeText(context, "Image can not be found or is too large to be sent",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (imageSend.length > MAX_IMAGE_SIZE) {
                                Toast.makeText(getApplicationContext(), "Image is too large",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            Calendar calendar = Calendar.getInstance();
                            String txtWriteTime = sdf.format(calendar.getTime());
                            nextrouters = getNextrouters();
                            int isUpload = 0;
                            chatManager.sendMessage(imageSend, DATA_IMAGE,currentConnectDevice, localName,localMacAddress,isUpload,txtWriteTime,nextrouters);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            case CAMERA_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                        byte[] cameraSend;
                        try {
                            cameraSend = compressBitmap(bitmap, true);
                        } catch (Exception e) {
                            Log.d(TAG, "Could not find the image");
                            Toast.makeText(getApplicationContext(), "Image could not be sent",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (cameraSend.length > MAX_IMAGE_SIZE) {
                            Toast.makeText(getApplicationContext(), "Image is too large to be sent",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Calendar calendar = Calendar.getInstance();
                        String txtWriteTime = sdf.format(calendar.getTime());
                        nextrouters = getNextrouters();
                        int isUpload = 0;
                        chatManager.sendMessage(cameraSend, DATA_IMAGE,currentConnectDevice, localName,localMacAddress,isUpload,txtWriteTime,nextrouters);

                    }
                }
                break;
//            case REQUEST_CONNECT_DEVICE:
//                // When DeviceListActivity returns with a device to connect
//                if (resultCode == Activity.RESULT_OK) {
//                    String macAddress = data.getExtras()
//                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
//                    connectDevice(macAddress);
//                }
//                break;

        }
    }

    public void permissionCheck() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "You have declined the permissions. " +
                        "Please allow them first to proceed.", Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]
                                {Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
            }
        } else {
            requestImageFromGallery();
        }
    }


    public void requestImageFromGallery() {
        Intent attachImageIntent = new Intent();
        attachImageIntent.setType("image/*");
        attachImageIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(attachImageIntent, "Select Picture"),
                SELECT_IMAGE);
    }

    private void reloadConnectList(){
        List<NeighborInfo> list = LitePal.findAll(NeighborInfo.class);
        neighborInfos.clear();
        neighborInfos.addAll(list);
    }

    private void initConfig() {
        HandlerTool.setWhichPage("CHAT");
        HandlerTool.setContext(context);
        HandlerTool.setLocalName(localName);
        HandlerTool.setConnectedDevice(currentConnectDevice.getNeighborName());
        isupdateView = true;
        HandleTimer();
    }

    private void sendGroupMessage(String message) {
        if (message.length() > 0) {
            if(currentConnectDevice == null) return;
            int isUpload = 0;
            Calendar calendar = Calendar.getInstance();
            String txtWriteTime = sdf.format(calendar.getTime());
            Log.e(TAG,message);
            if(nextrouters != null){
                chatManager.sendMessage(message.getBytes(), DATA_TEXT,currentConnectDevice,localName,localMacAddress,isUpload,txtWriteTime,nextrouters);
            }else {
//                chatManager.sendMessage(message.getBytes(), DATA_TEXT, currentConnectDevice, localName, localMacAddress, isUpload, txtWriteTime);
            }
        }
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.menuOff:
                //close socket connect
                break;
            case R.id.historyMsg:
                intent = new Intent(context, MessageListActivity.class);
                startActivity(intent);
                break;
            case R.id.menuConnect:
                chatManager.startConnection(neighborInfos,nextrouters);
                break;
            case android.R.id.home:
                finish();
                return true;
        }
        return true;
    }
    public static int STATE = 0;

    public static byte[] compressBitmap(Bitmap image, boolean isBeforeSocketSend) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 50, bos);
        String encodedImage = Base64.encodeToString(bos.toByteArray(),
                Base64.DEFAULT);
        byte[] compressed = isBeforeSocketSend ? encodedImage.getBytes()
                : Base64.decode(encodedImage, Base64.DEFAULT);
        return compressed;
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.arg1 == 0) {
                reLoadMessageFromDB();
            } else if (msg.arg1 == 1) {
                switch (STATE) {
                    case ChatUtils.STATE_NONE:
                        setState("None");
                        break;
                    case ChatUtils.STATE_LISTEN:
                        setState("LISTENING");
                        break;
                    case ChatUtils.STATE_CONNECTING:
                        setState("CONNECTING");
                        break;
                    case ChatUtils.STATE_CONNECTED:
                        setState("Connected: " + currentConnectDevice.getNeighborName());
                        break;
                }
            }
        }
    };
    Timer timerViewupdate = new Timer();
    public void HandleTimer(){
        timerViewupdate.schedule(new TimerTask() {
            public void run() {
                Message msg = new Message();
                msg.arg1 = 0;
                mHandler.sendMessage(msg);
            }
        }, 0, 500);
    }

    //reload message from DB
    private void reLoadMessageFromDB() {
//        String target = currentConnectDevice.getNeighborName();
        System.out.println(target +"---"+"Refresh the list again");
        List<MessageInfo> list = MessageDB.queryAllFromDB(target,localName);
        adapterMainChat = new MessageAdapter(context, R.layout.message, list);
        listMainChat.setAdapter(adapterMainChat);
    }

    public static String getNextrouters() {
        return nextrouters;
    }

    public static void setNextrouters(String name) {
        nextrouters = name;
    }

    private void setState(CharSequence subTitle) {
        String menuTitle = subTitle.toString();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setCustomView(R.layout.title_layout);
            TextView textView = (TextView) actionBar.getCustomView().findViewById(R.id.display_title);
            textView.setText(menuTitle);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowCustomEnabled(true);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.e(TAG, "onRestart");
    }



    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG, "onStop");
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
        isupdateView = false;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        Log.d("SecondActivity", "onDestroy");
        isupdateView = false;
        finish();
        timerViewupdate.cancel();
    }
}