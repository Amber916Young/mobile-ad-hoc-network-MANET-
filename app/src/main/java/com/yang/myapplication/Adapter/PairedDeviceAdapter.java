package com.yang.myapplication.Adapter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yang.myapplication.Interface.RecycleViewInterface;
import com.yang.myapplication.R;

import java.util.List;

public class PairedDeviceAdapter extends  RecyclerView.Adapter<PairedDeviceAdapter.ViewHolder>  {
    private Context context;
    private List<BluetoothDevice> dataList;
    private int rowId;
    private final RecycleViewInterface recycleViewInterface;



    public static final int BOND_NONE = 10;
    public static final int BOND_BONDING = 11;
    public static final int BOND_BONDED = 12;
    public PairedDeviceAdapter(Context context, int rowId, List<BluetoothDevice> dataList,RecycleViewInterface recycleViewInterface) {
        this.context = context;
        this.rowId = rowId;
        this.dataList = dataList;
        this.recycleViewInterface = recycleViewInterface;
    }
    public static class ViewHolder extends RecyclerView.ViewHolder{
        private TextView name ,MacAddress,globalIP ;
        public ViewHolder(@NonNull View itemView, RecycleViewInterface recycleViewInterface) {
            super(itemView);
            MacAddress =(TextView)itemView.findViewById(R.id.MacAddress);
            name =(TextView)itemView.findViewById(R.id.name);
            globalIP=itemView.findViewById(R.id.globalIP);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(recycleViewInterface != null){
                        int pos = getAdapterPosition();
                        if(pos != RecyclerView.NO_POSITION){
                            recycleViewInterface.onItemClick(pos);
                        }
                    }
                }
            });
        }


    }

    @NonNull
    @Override
    public PairedDeviceAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(rowId,parent,false);
        return new ViewHolder(view,recycleViewInterface);
    }
    @Override
    public void onBindViewHolder(@NonNull PairedDeviceAdapter.ViewHolder holder, int position) {
        BluetoothDevice current = dataList.get(position);
        holder.name.setText(current.getName());
        //MANET_ipAddress
        holder.MacAddress.setText(current.getAddress());
//        holder.globalIP.setText(String.valueOf(current.getBondState()));
    }


    @Override
    public int getItemCount() {
        return dataList.size();
    }
}
