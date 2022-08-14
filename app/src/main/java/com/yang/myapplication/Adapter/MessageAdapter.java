package com.yang.myapplication.Adapter;

import static com.yang.myapplication.entity.MessageInfo.DATA_AUDIO;
import static com.yang.myapplication.entity.MessageInfo.DATA_IMAGE;
import static com.yang.myapplication.entity.MessageInfo.DATA_TEXT;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.yang.myapplication.R;
import com.yang.myapplication.Tools.BluetoothTools;
import com.yang.myapplication.Tools.HandlerTool;
import com.yang.myapplication.entity.MessageInfo;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends ArrayAdapter<MessageInfo> {
    private int resoureId;
    private List<MessageInfo> objects = new ArrayList<>();
    private Context context;

    public MessageAdapter(@NonNull Context context, int resource, List<MessageInfo> objects) {
        super(context, resource);
        this.objects.addAll(objects);
        this.context = context;
    }

    private static class ViewHolder {
        ImageView unread;
        ImageView read;
        TextView Sendtime;
        TextView Readtime;
        TextView username_left;
        TextView username_right;
        TextView content_right;
        TextView content_left;
        ImageView imageView ;

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
            viewHolder.content_left = (TextView) convertView.findViewById(R.id.content_left);
            viewHolder.content_right = (TextView) convertView.findViewById(R.id.content_right);
            viewHolder.read = (ImageView) convertView.findViewById(R.id.read);
            viewHolder.Sendtime = (TextView) convertView.findViewById(R.id.Sendtime);
            viewHolder.Readtime = (TextView) convertView.findViewById(R.id.Readtime);
            viewHolder.username_left = (TextView) convertView.findViewById(R.id.username_left);
            viewHolder.username_right = (TextView) convertView.findViewById(R.id.username_right);
            viewHolder.imageView  = (ImageView) convertView.findViewById(R.id.chat_image);
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
            String name = msg.getSourceName();
            String showMessageTXT = null;
            int dataType = msg.getDataType();
            if (dataType == DATA_TEXT) {
                showMessageTXT =  msg.getMessage();
                viewHolder.imageView.setVisibility(View.GONE);
            } else if (dataType == DATA_IMAGE) {
                viewHolder.imageView.setVisibility(View.VISIBLE);

                String base64 = msg.getMessage();
                if(base64 == null){
                    if( msg.getContent() != null) {
                        base64 = msg.getContent().toString();
                        msg.imageBitmap = HandlerTool.base64ToBitmapOption(base64);
                        Bitmap b = msg.imageBitmap;
                        viewHolder.imageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                        viewHolder.imageView.setImageBitmap(b);
                        viewHolder.imageView.invalidate();
                    }
                }else {
                    msg.imageBitmap = HandlerTool.base64ToBitmapOption(base64);
                    Bitmap b = msg.imageBitmap;
                    viewHolder.imageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                    viewHolder.imageView.setImageBitmap(b);
                    viewHolder.imageView.invalidate();
                }

            } else if (dataType == DATA_AUDIO) {
                viewHolder.imageView.setVisibility(View.GONE);
                showMessageTXT = "Audio-"+msg.getUuid()+ ".mp3";
            }

            String localName = BluetoothTools.bluetoothAdapter.getName();
            String username = name+":";
            if(localName.equals(name)){
//                viewHolder.username_right.setText(username);
                viewHolder.username_left.setVisibility(View.GONE);
                viewHolder.username_right.setVisibility(View.GONE);
                if(dataType == DATA_IMAGE){
                    viewHolder.content_right.setVisibility(View.GONE);
                    viewHolder.content_left.setVisibility(View.GONE);
                    viewHolder.imageView.setBackgroundResource(R.color.teal_100);
                }else {
                    viewHolder.imageView.setBackgroundResource(R.color.gray);
                    viewHolder.content_right.setText(showMessageTXT);
                    viewHolder.content_right.setVisibility(View.VISIBLE);
                    viewHolder.content_left.setVisibility(View.GONE);
                }
            }else {
//                viewHolder.username_left.setText(username);
                viewHolder.username_left.setVisibility(View.GONE);
                viewHolder.username_right.setVisibility(View.GONE);
                if(dataType == DATA_IMAGE){
                    viewHolder.content_right.setVisibility(View.GONE);
                    viewHolder.content_left.setVisibility(View.GONE);
                }else {
                    viewHolder.content_left.setText(showMessageTXT);
                    viewHolder.content_right.setVisibility(View.GONE);
                    viewHolder.content_left.setVisibility(View.VISIBLE);
                }
            }
            if (!msg.getReadDate().equals("END"))
                viewHolder.Readtime.setText(msg.getReadDate());
        }

        return convertView;
    }



}
