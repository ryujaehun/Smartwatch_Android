package com.polidea.rxandroidble.exceptions;

import android.bluetooth.BluetoothGatt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Exception emitted when the BLE link has been interrupted as a result of an error. The exception contains
 * detailed explanation of the error source (type of operation) and the code proxied from
 * the <a href="https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/android-5.1.0_r1/stack/include/gatt_api.h">
 * Android system</a>.
 *
 * @see com.polidea.rxandroidble.RxBleDevice#establishConnection(Context, boolean)
 */
public class BleGattException extends BleException {

    public static final int UNKNOWN_STATUS = -1;

    @Nullable
    private final BluetoothGatt gatt;

    private final int status;
    private final BleGattOperationType bleGattOperationType;

    @Deprecated
    public BleGattException(int status, BleGattOperationType bleGattOperationType) {
        this.gatt = null;
        this.status = status;
        this.bleGattOperationType = bleGattOperationType;
    }

    public BleGattException(@NonNull BluetoothGatt gatt, int status, BleGattOperationType bleGattOperationType) {
        this.gatt = gatt;
        this.status = status;
        this.bleGattOperationType = bleGattOperationType;
    }

    public BleGattException(BluetoothGatt gatt, BleGattOperationType bleGattOperationType) {
        this(gatt, UNKNOWN_STATUS, bleGattOperationType);
    }

    public String getMacAddress() {
        return gatt != null ? gatt.getDevice().getAddress() : null;
    }

    public BleGattOperationType getBleGattOperationType() {
        return bleGattOperationType;
    }

    public int getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "{macAddress=" + getMacAddress()
                + ", status=" + status + String.format(" (0x%x)", status)
                + ", bleGattOperationType=" + bleGattOperationType
                + '}';
    }
}
