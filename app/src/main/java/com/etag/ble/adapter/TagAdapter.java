package com.etag.ble.adapter;

import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.etag.ble.R;

import java.util.ArrayList;
import java.util.List;

public class TagAdapter extends  RecyclerView.Adapter<TagAdapter.TagViewHolder> {

    private List<BleEntity> items;
    private OnItemClickListener<BleEntity> onItemClickListener;
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
        BleEntity device= items.get(position);
        holder.setText(R.id.txt_name, TextUtils.concat("name:",device.getName()));
        holder.setText(R.id.txt_mac,TextUtils.concat("MAC:",device.getAddress()));

        CheckBox checkBox= holder.findViewById(R.id.checkbox);
        checkBox.setChecked(device.isCheck());
        checkBox.setOnCheckedChangeListener((compoundButton, b) -> {
            device.setCheck(b);
            notifyDataSetChanged();
        });
        if(onItemClickListener!=null)
            holder.itemView.setOnClickListener(v->onItemClickListener.onItemClick(holder.itemView,device,position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void addItem(BluetoothDevice device){
        BleEntity bleEntity=new BleEntity();
        bleEntity.setAddress(device.getAddress());
        bleEntity.setName(device.getName());
        this.items.add(bleEntity);
        notifyDataSetChanged();
    }

    public void clear(){
        this.items.clear();
        notifyDataSetChanged();
    }

    public List<String> getCheckList(){
        List<String> list=new ArrayList<>();
        for (BleEntity ble:this.items) {
            if (ble.isCheck())
                list.add(ble.getName());
        }
        return list;
    }

    public void setOnItemClickListener(OnItemClickListener<BleEntity> onItemClickListener) {
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


    public class BleEntity{
        private String name;
        private String address;
        private boolean check;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public boolean isCheck() {
            return check;
        }

        public void setCheck(boolean check) {
            this.check = check;
        }
    }
}

