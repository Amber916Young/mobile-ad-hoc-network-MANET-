package com.yang.myapplication.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.yang.myapplication.Adapter.HistoryMessageAdapter;
import com.yang.myapplication.Adapter.MessageAdapter;
import com.yang.myapplication.R;
import com.yang.myapplication.Tools.BluetoothTools;
import com.yang.myapplication.service.MessageDB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MessageListActivity extends AppCompatActivity {
    private HistoryMessageAdapter historyMessageAdapter ;
    private Context context;
    private ListView historyMsg;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        historyMsg = findViewById(R.id.historyMsg);
        context = this;
        historyMessageAdapter = new HistoryMessageAdapter(context, R.layout.history_message, MessageDB.queryAllFromDB() );
        historyMsg.setAdapter(historyMessageAdapter);
        setState("history messages");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_msg, menu);
        return true;
    }

    private void setState(String menuTitle) {
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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()){
            case R.id.menuClear:
                deleteMessage();
                break;
        }
        return true;
    }
    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
        finish();
    }
    private void deleteMessage(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MessageListActivity.this);
        builder.setTitle("Delete all messages？ ");
        builder.setMessage("Click 「YES」 to delete the history");
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String notification = "cancel";
                Toast.makeText(context, notification, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MessageDB.deleteAllMsgDB();
                historyMessageAdapter = new HistoryMessageAdapter(context, R.layout.history_message, MessageDB.queryAllFromDB() );
                historyMsg.setAdapter(historyMessageAdapter);
                finish();
            }
        });
        builder.create().show();

    }

}