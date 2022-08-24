package com.yang.myapplication.Adapter;

import static com.yang.myapplication.entity.NeighborInfo.onLine;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
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
        private TextView neighborMac ,neighborName,hop ,lastMessage, lastTime;
        private ImageView avatar;
        private ImageView online;
        private ImageView offline;
        public ViewHolder(@NonNull View itemView, RecycleViewInterface recycleViewInterface) {
            super(itemView);
            neighborMac =itemView.findViewById(R.id.neighborMac);
            neighborName =itemView.findViewById(R.id.neighborName);
            hop=itemView.findViewById(R.id.hop);
            avatar=itemView.findViewById(R.id.imageView);
            lastMessage=itemView.findViewById(R.id.lastMessage);
            lastTime=itemView.findViewById(R.id.lastTime);
            offline=itemView.findViewById(R.id.offline);
            online=itemView.findViewById(R.id.online);
//            timestamp=itemView.findViewById(R.id.timestamp);


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

    @Override
    public void onBindViewHolder(@NonNull NeighborAdapter.ViewHolder holder, int position) {
        NeighborInfo current = dataList.get(position);
        holder.neighborMac.setText(current.getNeighborMac());
        holder.neighborName.setText(current.getNeighborName());

        if(current.getConnection_status() == onLine){
            holder.offline.setVisibility(View.GONE);
            holder.online.setVisibility(View.VISIBLE);
        }else {
            holder.online.setVisibility(View.GONE);
            holder.offline.setVisibility(View.VISIBLE);
        }

//        holder.neighborName.setTextColor(Color.CYAN);
        String hop = "(distance:"+String.valueOf(current.getHop())+" hop(s))";



        holder.hop.setText(hop);
        if(current.getNeighborName().equals("KFW")){
            holder.avatar.setImageResource(R.drawable.ik);
        }else if(current.getNeighborName().equals("B")){
            holder.avatar.setImageResource(R.drawable.ib);
        }else if(current.getNeighborName().equals("C")){
            holder.avatar.setImageResource(R.drawable.ic);
        }else if(current.getNeighborName().equals("D")){
            holder.avatar.setImageResource(R.drawable.id);
        }else {
            holder.avatar.setImageResource(R.drawable.ig);
        }
        holder.lastMessage.setText(current.getLastMessage());
        holder.lastTime.setText(current.getLastTime());
//        holder.timestamp.setText(current.getTimestamp().toString());
//        holder.rssi.setText(String.valueOf(current.getRssi()));
//        holder.myMac.setText(current.getMyMac());
//        holder.myNaRefer tome.setText(current.getMyName());
    }


    @Override
    public int getItemCount() {
        return dataList.size();
    }
}
