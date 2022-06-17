package com.yang.myapplication.Adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yang.myapplication.Interface.RecycleViewInterface;
import com.yang.myapplication.R;
import com.yang.myapplication.entity.DeviceInfo;

import java.util.HashMap;
import java.util.List;

public class DeviceCloudAdapter extends  RecyclerView.Adapter<DeviceCloudAdapter.ViewHolder>  {
    private Context context;
    private List<HashMap> dataList;
    private int rowId;
    private final RecycleViewInterface recycleViewInterface;

    public static final int BOND_NONE = 10;
    public static final int BOND_BONDING = 11;
    public static final int BOND_BONDED = 12;

    public DeviceCloudAdapter(Context context, int rowId, List<HashMap> dataList, RecycleViewInterface recycleViewInterface) {
        this.context = context;
        this.rowId = rowId;
        this.dataList = dataList;
        this.recycleViewInterface = recycleViewInterface;
    }
    public static class ViewHolder extends RecyclerView.ViewHolder{
        private TextView username ,MacAddress,uuid ;
        private ImageView online,offline;
        public ViewHolder(@NonNull View itemView, RecycleViewInterface recycleViewInterface) {
            super(itemView);
            MacAddress =itemView.findViewById(R.id.MacAddress);
            username =itemView.findViewById(R.id.username);
            uuid=itemView.findViewById(R.id.uuid);
            online=itemView.findViewById(R.id.online);
            offline=itemView.findViewById(R.id.offline);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(recycleViewInterface != null){
                        int pos = getAdapterPosition();
                        if(pos != RecyclerView.NO_POSITION){
                            recycleViewInterface.onItemClickPair(pos);
                        }
                    }
                }
            });
        }
    }

    @NonNull
    @Override
    public DeviceCloudAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(rowId,parent,false);
        return new ViewHolder(view,recycleViewInterface);
    }
    @Override
    public void onBindViewHolder(@NonNull DeviceCloudAdapter.ViewHolder holder, int position) {
        HashMap current = dataList.get(position);
        holder.username.setText(current.get("username").toString());
        holder.MacAddress.setText(current.get("mac").toString());
        holder.uuid.setText(current.get("uuid").toString());
        String status = current.get("status").toString();
        if(status.equals("1")){
            holder.online.setVisibility(View.VISIBLE);
            holder.offline.setVisibility(View.GONE);
        }else {
            holder.online.setVisibility(View.GONE);
            holder.offline.setVisibility(View.VISIBLE);
        }
    }


    @Override
    public int getItemCount() {
        return dataList.size();
    }
}
