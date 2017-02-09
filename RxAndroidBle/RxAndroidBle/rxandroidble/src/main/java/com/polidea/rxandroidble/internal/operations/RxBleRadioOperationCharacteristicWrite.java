package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleGattRadioOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble.internal.util.ByteAssociation;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Func1;

public class RxBleRadioOperationCharacteristicWrite extends RxBleGattRadioOperation<byte[]> {

    private final BluetoothGattCharacteristic bluetoothGattCharacteristic;

    private final byte[] data;

    private final Scheduler timeoutScheduler;

    public RxBleRadioOperationCharacteristicWrite(RxBleGattCallback rxBleGattCallback, BluetoothGatt bluetoothGatt,
                                                  BluetoothGattCharacteristic bluetoothGattCharacteristic, byte[] data,
                                                  Scheduler timeoutScheduler) {
        super(bluetoothGatt, rxBleGattCallback, BleGattOperationType.CHARACTERISTIC_WRITE);
        this.bluetoothGattCharacteristic = bluetoothGattCharacteristic;
        this.data = data;
        this.timeoutScheduler = timeoutScheduler;
    }

    @Override
    protected void protectedRun() {
        //noinspection Convert2MethodRef
        final Subscription subscription = rxBleGattCallback
                .getOnCharacteristicWrite()
                .takeFirst(new Func1<ByteAssociation<UUID>, Boolean>() {
                    @Override
                    public Boolean call(ByteAssociation<UUID> uuidPair) {
                        return uuidPair.first.equals(bluetoothGattCharacteristic.getUuid());
                    }
                })
                .map(new Func1<ByteAssociation<UUID>, byte[]>() {
                    @Override
                    public byte[] call(ByteAssociation<UUID> uuidPair) {
                        return uuidPair.second;
                    }
                })
                .timeout(
                        30,
                        TimeUnit.SECONDS,
                        Observable.<byte[]>error(newTimeoutException()),
                        timeoutScheduler
                )
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        RxBleRadioOperationCharacteristicWrite.this.releaseRadio();
                    }
                })
                .subscribe(getSubscriber());

        bluetoothGattCharacteristic.setValue(data);
        final boolean success = bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
        if (!success) {
            subscription.unsubscribe();
            onError(newCannotStartException());
        }
    }
}
