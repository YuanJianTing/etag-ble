package com.etag.ble.adapter;

import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.etag.ble.R;

import java.util.ArrayList;
import java.util.List;

public class TagAdapter extends  RecyclerView.Adapter<TagAdapter.TagViewHolder> {

    private List<BluetoothDevice> items;
    private OnItemClickListener<BluetoothDevice> onItemClickListener;
    public TagAdapter(){
        items=new ArrayList<>();
    }

    @NonNull
    @Override
    public TagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view =LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ble,parent,false);
        return new TagViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TagViewHolder holder, int position) {
        BluetoothDevice device= items.get(position);
        holder.setText(R.id.txt_name, TextUtils.concat("name:",device.getName()));
        holder.setText(R.id.txt_mac,TextUtils.concat("MAC:",device.getAddress()));
        if(onItemClickListener!=null)
            holder.itemView.setOnClickListener(v->onItemClickListener.onItemClick(holder.itemView,device,position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void addItem(BluetoothDevice device){
        this.items.add(device);
        notifyDataSetChanged();
    }

    public void clear(){
        this.items.clear();
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener<BluetoothDevice> onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public class TagViewHolder extends RecyclerView.ViewHolder{

        public TagViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void setText(@IdRes int id, CharSequence text){
            ((TextView)findViewById(id)).setText(text);
        }
        public <T extends View> T findViewById(@IdRes int id){
            return itemView.findViewById(id);
        }

    }
}
