package com.yang.myapplication.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import com.yang.myapplication.Adapter.HistoryMessageAdapter;
import com.yang.myapplication.Adapter.MessageAdapter;
import com.yang.myapplication.R;
import com.yang.myapplication.service.MessageDB;

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
        historyMessageAdapter = new HistoryMessageAdapter(context, R.layout.history_message, MessageDB.queryAllFromDB(null) );
        historyMsg.setAdapter(historyMessageAdapter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_msg, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()){
            case R.id.menuClear:
                deleteMessage();
                break;
            case R.id.menuBack:
                intent = new Intent(MessageListActivity.this,BluetoothChat.class);
                startActivity(intent);
                break;
        }
        return true;
    }

    private void deleteMessage(){
        MessageDB.deleteAllMsgDB();
        historyMessageAdapter = new HistoryMessageAdapter(context, R.layout.history_message, MessageDB.queryAllFromDB(null) );
        historyMsg.setAdapter(historyMessageAdapter);
    }

}