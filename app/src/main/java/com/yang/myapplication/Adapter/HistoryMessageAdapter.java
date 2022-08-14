package com.yang.myapplication.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.yang.myapplication.R;
import com.yang.myapplication.entity.MessageInfo;

import java.util.List;

public class HistoryMessageAdapter extends ArrayAdapter<MessageInfo> {
    private int resoureId;
    private List<MessageInfo> objects;
    private Context context;

    public HistoryMessageAdapter(@NonNull Context context, int resource, List<MessageInfo> objects) {
        super(context, resource);
        this.objects = objects;
        this.context = context;
    }

    private static class ViewHolder {
        ImageView unread;
        ImageView read;
        ImageView upload;
        ImageView unupload;
        TextView content;
        TextView UUID;
        TextView targetName;
        TextView targetMAC;
        TextView Sendtime;
        TextView Readtime;
    }

    @Override
    public int getCount() {
        return objects.size();
    }

    @Override
    public MessageInfo getItem(int position) {
        return objects.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater mInflater = LayoutInflater.from(context);
            convertView = mInflater.inflate(R.layout.history_message, null);
            viewHolder.unread = (ImageView) convertView.findViewById(R.id.unread);
            viewHolder.read = (ImageView) convertView.findViewById(R.id.read);
            viewHolder.Sendtime = (TextView) convertView.findViewById(R.id.Sendtime);
            viewHolder.Readtime = (TextView) convertView.findViewById(R.id.Readtime);
            viewHolder.upload = (ImageView)convertView.findViewById(R.id.upload);
            viewHolder.unupload = (ImageView)convertView.findViewById(R.id.unupload);
            viewHolder.UUID = (TextView)convertView.findViewById(R.id.UUID);
            viewHolder.targetName = (TextView)convertView.findViewById(R.id.targetName);
            viewHolder.targetMAC =(TextView) convertView.findViewById(R.id.targetMAC);
            viewHolder.content = (TextView)convertView.findViewById(R.id.content);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        MessageInfo msg = objects.get(position);
        if (null != msg) {
            if (msg.getIsRead() == 1) {
                viewHolder.unread.setVisibility(View.GONE);
                viewHolder.read.setVisibility(View.VISIBLE);
            } else {
                viewHolder.unread.setVisibility(View.VISIBLE);
                viewHolder.read.setVisibility(View.GONE);
            }

            if (msg.getIsUpload() == 1) {
                viewHolder.unupload.setVisibility(View.GONE);
                viewHolder.upload.setVisibility(View.VISIBLE);
            } else {
                viewHolder.unupload.setVisibility(View.VISIBLE);
                viewHolder.upload.setVisibility(View.GONE);
            }

            viewHolder.UUID.setText(msg.getUuid());
            viewHolder.targetName.setText(msg.getTargetName());
            viewHolder.targetMAC.setText(msg.getTargetMAC());
            viewHolder.Sendtime.setText(msg.getSendDate());
            if(msg.getDataType() == 1 ){
                viewHolder.content.setText(msg.getMessage().toString());
            }else if(msg.getDataType() == 2 ){
                viewHolder.content.setText("[image message]");
            }else {
                viewHolder.content.setText("[audio message]");
            }



            if(!msg.getReadDate().equals("END"))
                viewHolder.Readtime.setText(msg.getReadDate());
        }

        return convertView;
    }
}
