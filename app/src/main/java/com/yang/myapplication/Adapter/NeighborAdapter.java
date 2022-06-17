package com.yang.myapplication.Adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yang.myapplication.Interface.RecycleViewInterface;
import com.yang.myapplication.R;
import com.yang.myapplication.entity.NeighborInfo;

import java.util.List;

public class NeighborAdapter extends  RecyclerView.Adapter<NeighborAdapter.ViewHolder>  {
    private Context context;
    private List<NeighborInfo> dataList;
    private int rowId;
    private final RecycleViewInterface recycleViewInterface;

    public NeighborAdapter(Context context, int rowId, List<NeighborInfo> dataList,RecycleViewInterface recycleViewInterface) {
        this.context = context;
        this.rowId = rowId;
        this.dataList = dataList;
        this.recycleViewInterface = recycleViewInterface;

    }
    public static class ViewHolder extends RecyclerView.ViewHolder{
        private TextView neighborMac ,neighborName,hop ,timestamp, rssi,myMac,myName;
        private Button netCommunication;
        public ViewHolder(@NonNull View itemView, RecycleViewInterface recycleViewInterface) {
            super(itemView);
            neighborMac =itemView.findViewById(R.id.neighborMac);
            neighborName =itemView.findViewById(R.id.neighborName);
            hop=itemView.findViewById(R.id.hop);
            timestamp=itemView.findViewById(R.id.timestamp);
            netCommunication = itemView.findViewById(R.id.netCommunication);
//            rssi=itemView.findViewById(R.id.rssi);
//            myMac=itemView.findViewById(R.id.myMac);
//            myName=itemView.findViewById(R.id.myName);
            netCommunication.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(recycleViewInterface != null){
                        int pos = getAdapterPosition();
                        if(pos != RecyclerView.NO_POSITION){
//                            recycleViewInterface.onItemClick(pos);
                        }
                    }
                }
            });

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
    public NeighborAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(rowId,parent,false);
        return new ViewHolder(view,recycleViewInterface);
    }
    //    String neighborMac, String neighborName, int hop, Date timestamp,int rssi,String myMac,String myName

    @Override
    public void onBindViewHolder(@NonNull NeighborAdapter.ViewHolder holder, int position) {
        NeighborInfo current = dataList.get(position);
        holder.neighborMac.setText(current.getNeighborMac());
        holder.neighborName.setText(current.getNeighborName());
        holder.hop.setText(String.valueOf(current.getHop()));
        holder.timestamp.setText(current.getTimestamp().toString());
//        holder.rssi.setText(String.valueOf(current.getRssi()));
//        holder.myMac.setText(current.getMyMac());
//        holder.myName.setText(current.getMyName());
    }


    @Override
    public int getItemCount() {
        return dataList.size();
    }
}
