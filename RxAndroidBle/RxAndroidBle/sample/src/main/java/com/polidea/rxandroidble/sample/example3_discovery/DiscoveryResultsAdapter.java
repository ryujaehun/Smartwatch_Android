package com.polidea.rxandroidble.sample.example3_discovery;


import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.polidea.rxandroidble.RxBleDeviceServices;
import com.polidea.rxandroidble.sample.R;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;

class DiscoveryResultsAdapter extends RecyclerView.Adapter<DiscoveryResultsAdapter.ViewHolder> {

    static class AdapterItem {

        public static final int SERVICE = 1;
        public static final int CHARACTERISTIC = 2;
        final int type;
        final String description;
        final UUID uuid;

        AdapterItem(int type, String description, UUID uuid) {
            this.type = type;
            this.description = description;
            this.uuid = uuid;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(android.R.id.text1)
        public TextView line1;
        @BindView(android.R.id.text2)
        public TextView line2;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public interface OnAdapterItemClickListener {

        void onAdapterViewClick(View view);
    }

    private final List<AdapterItem> data = new ArrayList<>();
    private OnAdapterItemClickListener onAdapterItemClickListener;
    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (onAdapterItemClickListener != null) {
                onAdapterItemClickListener.onAdapterViewClick(v);
            }
        }
    };

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).type;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final int itemViewType = holder.getItemViewType();
        final AdapterItem item = getItem(position);

        if (itemViewType == AdapterItem.SERVICE) {
            holder.line1.setText(String.format("Service: %s", item.description));
        } else {
            holder.line1.setText(String.format("Characteristic: %s", item.description));
        }

        holder.line2.setText(item.uuid.toString());
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final int layout = viewType == AdapterItem.SERVICE ? R.layout.item_discovery_service : R.layout.item_discovery_characteristic;
        final View itemView = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        itemView.setOnClickListener(onClickListener);
        return new ViewHolder(itemView);
    }

    public void setOnAdapterItemClickListener(OnAdapterItemClickListener onAdapterItemClickListener) {
        this.onAdapterItemClickListener = onAdapterItemClickListener;
    }

    public void swapScanResult(RxBleDeviceServices services) {
        data.clear();

        for (BluetoothGattService service : services.getBluetoothGattServices()) {
            // Add service
            data.add(new AdapterItem(AdapterItem.SERVICE, getServiceType(service), service.getUuid()));
            final List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();

            for (BluetoothGattCharacteristic characteristic : characteristics) {
                data.add(new AdapterItem(AdapterItem.CHARACTERISTIC, describeProperties(characteristic), characteristic.getUuid()));
            }
        }

        notifyDataSetChanged();
    }

    private String describeProperties(BluetoothGattCharacteristic characteristic) {
        List<String> properties = new ArrayList<>();
        if (isCharacteristicReadable(characteristic)) properties.add("Read");
        if (isCharacteristicWriteable(characteristic)) properties.add("Write");
        if (isCharacteristicNotifiable(characteristic)) properties.add("Notify");
        return TextUtils.join(" ", properties);
    }

    public AdapterItem getItem(int position) {
        return data.get(position);
    }

    private String getServiceType(BluetoothGattService service) {
        return service.getType() == BluetoothGattService.SERVICE_TYPE_PRIMARY ? "primary" : "secondary";
    }

    private boolean isCharacteristicNotifiable(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
    }

    private boolean isCharacteristicReadable(BluetoothGattCharacteristic characteristic) {
        return ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0);
    }

    private boolean isCharacteristicWriteable(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE
                | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0;
    }
}
