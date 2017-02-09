package com.polidea.rxandroidble.sample.example2_connection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.SwitchCompat;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothGatt;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.sample.example1_scanning.ScanActivity;
import com.polidea.rxandroidble.sample.R;
import com.polidea.rxandroidble.sample.SampleApplication;
import com.polidea.rxandroidble.utils.ConnectionSharingAdapter;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;

import static com.trello.rxlifecycle.android.ActivityEvent.DESTROY;
import static com.trello.rxlifecycle.android.ActivityEvent.PAUSE;

public class ConnectionExampleActivity extends RxAppCompatActivity {

    @BindView(R.id.connection_state)
    TextView connectionStateView;
    @BindView(R.id.connect_toggle)
    Button connectButton;
    @BindView(R.id.newMtu)
    EditText textMtu;
    @BindView(R.id.set_mtu)
    Button setMtuButton;
    @BindView(R.id.autoconnect)
    SwitchCompat autoConnectToggleSwitch;
    @BindView(R.id.recvText)
    EditText recvText;
    @BindView(R.id.trvText)
    EditText trvText;
    @BindView(R.id.send_button)
    Button sendButton;


    private RxBleDevice bleDevice;
    private Subscription connectionSubscription;
    private  BluetoothGattServer gattServer;
    private  BluetoothManager mBluetoothManager;
    private PublishSubject<Void> disconnectTriggerSubject = PublishSubject.create();
    private Observable<RxBleConnection> connectionObservable;

    BluetoothGattService cscService;
    BluetoothGattCharacteristic cscChar;
    BluetoothGattDescriptor cscDesc;

    @OnClick(R.id.connect_toggle)
    public void onConnectToggleClick() {

//        if (isConnected()) {
//            triggerDisconnect();
//        } else {
//            connectionSubscription = bleDevice.establishConnection(this, autoConnectToggleSwitch.isChecked())
//                    .compose(bindUntilEvent(PAUSE))
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .doOnUnsubscribe(this::clearSubscription)
//                    .subscribe(this::onConnectionReceived, this::onConnectionFailure);
//            gattServer.connect(bleDevice.getBluetoothDevice(), false);
//        }
        if (isConnected()) {
            triggerDisconnect();
        } else {
            connectionObservable.compose(bindUntilEvent(PAUSE)).observeOn(AndroidSchedulers.mainThread()).subscribe(this::onConnectionReceived, this::onConnectionFailure);

        }
    }
    private Observable<RxBleConnection> prepareConnectionObservable() {
        return bleDevice
                .establishConnection(this, false)
                .takeUntil(disconnectTriggerSubject)
                .compose(bindUntilEvent(PAUSE))
                .doOnUnsubscribe(this::clearSubscription)
                .compose(new ConnectionSharingAdapter());
    }

    @OnClick(R.id.send_button)
    public  void onSendButton()
    {
        sendResponse(trvText.getText().toString());
    }

    @OnClick(R.id.set_mtu)
    public void onSetMtu() {
        bleDevice.establishConnection(this, false)
                .flatMap(rxBleConnection -> rxBleConnection.requestMtu(72))
                .first() // Disconnect automatically after discovery
                .compose(bindUntilEvent(PAUSE))
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(this::updateUI)
                .subscribe(this::onMtuReceived, this::onConnectionFailure);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_example2);
        ButterKnife.bind(this);
        String macAddress = getIntent().getStringExtra(ScanActivity.EXTRA_MAC_ADDRESS);
        setTitle(getString(R.string.mac_address, macAddress));
        bleDevice = SampleApplication.getRxBleClient(this).getBleDevice(macAddress);
        connectionObservable = prepareConnectionObservable();
        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        gattServer = mBluetoothManager.openGattServer(this, new BluetoothGattServerCallback() {
            @Override
            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                super.onConnectionStateChange(device, status, newState);
                if(newState == 0) {
                    connectionStateView.setText("Disconnected");
                }
                else if(newState == 2){
                    connectionStateView.setText("Connected");
                }
                else if(newState == 1){
                    connectionStateView.setText("Connecting");
                }
                else if(newState == 3){
                    connectionStateView.setText("Disconnecting");
                }
                updateUI();
            }

            @Override
            public void onDescriptorReadRequest(BluetoothDevice device, int requestId,
                                                int offset, BluetoothGattDescriptor descriptor) {
                //Log.d("HELLO", "Our gatt server descriptor was read.");
                super.onDescriptorReadRequest(device, requestId, offset, descriptor);
                //Log.d("DONE", "Our gatt server descriptor was read.");
            }

            @Override
            public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                                 BluetoothGattDescriptor descriptor,
                                                 boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                //Log.d("HELLO", "Our gatt server descriptor was written.");
                super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
                //Log.d("DONE", "Our gatt server descriptor was written.");

                //NOTE: Its important to send response. It expects response else it will disconnect
                if (responseNeeded) {
                    gattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            value);

                }

            }
        });

        cscService = new BluetoothGattService(UUID.fromString("fd5abba0-3935-11e5-85a6-0002a5d5c51b"), BluetoothGattService.SERVICE_TYPE_PRIMARY);
        cscChar = new BluetoothGattCharacteristic(UUID.fromString("fd5abba0-3935-11e5-85a6-0002a5d5c51b"), BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_WRITE);
        cscDesc = new BluetoothGattDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"), BluetoothGattDescriptor.PERMISSION_WRITE);
        cscChar.addDescriptor(cscDesc);
        cscService.addCharacteristic(cscChar);
        gattServer.addService(cscService);
        // How to listen for connection state changes
        bleDevice.observeConnectionStateChanges()
                .compose(bindUntilEvent(DESTROY))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onConnectionStateChange);
    }


        private boolean isConnected() {
        return bleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
    }

    private void onConnectionFailure(Throwable throwable) {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(android.R.id.content), "Connection error: " + throwable, Snackbar.LENGTH_SHORT).show();
    }

    private void onConnectionReceived(RxBleConnection connection) {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(android.R.id.content), "Connection received", Snackbar.LENGTH_SHORT).show();
        gattServer.connect(bleDevice.getBluetoothDevice(), true);
        if (isConnected()) {
            connectionObservable
                    .flatMap(rxBleConnection -> rxBleConnection.setupNotification(UUID.fromString("fd5abba1-3935-11e5-85a6-0002a5d5c51b")))
                    .doOnNext(notificationObservable -> runOnUiThread(this::notificationHasBeenSetUp))
                    .flatMap(notificationObservable -> notificationObservable)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onNotificationReceived, this::onNotificationSetupFailure);
        }
    }

    private void onConnectionStateChange(RxBleConnection.RxBleConnectionState newState) {
        connectionStateView.setText(newState.toString());
        if(newState == RxBleConnection.RxBleConnectionState.DISCONNECTED) sendResponse("disconnected!");
        updateUI();
    }

    private void onMtuReceived(Integer mtu) {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(android.R.id.content), "MTU received: " + mtu, Snackbar.LENGTH_SHORT).show();
    }

    private void clearSubscription() {
        connectionSubscription = null;
        updateUI();
    }

    private void triggerDisconnect() {

//        if (connectionSubscription != null) {
//            connectionSubscription.unsubscribe();
//        }
        sendResponse("disconnect!");//TODO: Neeed to implement having MCU to disconnect on receiving this message.
        disconnectTriggerSubject.onNext(null);
        gattServer.close();
    }

    private void updateUI() {
        final boolean connected = isConnected();
        connectButton.setText(connected ? R.string.disconnect : R.string.connect);
        autoConnectToggleSwitch.setEnabled(!connected);
    }
    private void onNotificationReceived(byte[] bytes) {
        //noinspection ConstantConditions
        //Snackbar.make(findViewById(R.id.main), "Change: " + HexString.bytesToHex(bytes), Snackbar.LENGTH_SHORT).show();
        recvText.setText(new String(bytes));
    }

    private void onNotificationSetupFailure(Throwable throwable) {
        //noinspection ConstantConditions
        //Snackbar.make(findViewById(R.id.main), "Notifications error: " + throwable, Snackbar.LENGTH_SHORT).show();
    }

    private void notificationHasBeenSetUp() {
        //noinspection ConstantConditions
        //Snackbar.make(findViewById(R.id.main), "Notifications has been set up", Snackbar.LENGTH_SHORT).show();
    }

    //Send notification to all the devices once you write
    private void sendResponse(String msg) {
        cscChar.setValue(msg.getBytes());
        gattServer.notifyCharacteristicChanged(bleDevice.getBluetoothDevice(), cscChar, false);
    }

}
