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

public class MessageAdapter extends ArrayAdapter<MessageInfo> {
    private int resoureId;
    private List<MessageInfo> objects;
    private Context context;

    public MessageAdapter(@NonNull Context context, int resource, List<MessageInfo> objects) {
        super(context, resource);
        this.objects = objects;
        this.context = context;
    }

    private static class ViewHolder {
        ImageView unread;
        ImageView read;
        TextView content;
        TextView Sendtime;
        TextView Readtime;
    }

    @Override
    public int getCount() {
        return objects.size();
    }

    @Override
    public MessageInfo getItem(int position) {
        // TODO Auto-generated method stub
        return objects.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater mInflater = LayoutInflater.from(context);
            convertView = mInflater.inflate(R.layout.message, null);
            viewHolder.unread = (ImageView) convertView.findViewById(R.id.unread);
            viewHolder.read = (ImageView) convertView.findViewById(R.id.read);
            viewHolder.Sendtime = (TextView) convertView.findViewById(R.id.Sendtime);
            viewHolder.Readtime = (TextView) convertView.findViewById(R.id.Readtime);
            viewHolder.content = (TextView) convertView.findViewById(R.id.content);
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
            viewHolder.Sendtime.setText(msg.getSendDate());
            String name = "";
                name = msg.getSourceName();

            viewHolder.content.setText(name+":"+msg.getContent());

            if(!msg.getReadDate().equals("END"))
                viewHolder.Readtime.setText(msg.getReadDate());
        }

        return convertView;
    }
}
