package com.github.esabook.swaplock.adapters;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.github.esabook.swaplock.R;
import com.github.esabook.swaplock.databinding.RowDeviceBinding;

import java.util.List;

public class PairedBuetoothAdapter extends ArrayAdapter<BluetoothDevice> {
    public PairedBuetoothAdapter(Context context, int resource,List<BluetoothDevice> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position,View convertView, ViewGroup parent) {
        RowDeviceBinding itemBinding;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_device, null);
            itemBinding = DataBindingUtil.bind(convertView);
            convertView.setTag(itemBinding);
        } else {
            itemBinding = (RowDeviceBinding) convertView.getTag();
        }
        BluetoothDevice obj = getItem(position);
        if (obj != null){
            itemBinding.textView2.setText(obj.getName());
        }
        return itemBinding.getRoot();
    }
}
