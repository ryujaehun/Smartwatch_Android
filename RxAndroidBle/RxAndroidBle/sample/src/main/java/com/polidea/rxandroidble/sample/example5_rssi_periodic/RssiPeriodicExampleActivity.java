package com.polidea.rxandroidble.sample.example5_rssi_periodic;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.widget.Button;
import android.widget.TextView;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.sample.DeviceActivity;
import com.polidea.rxandroidble.sample.R;
import com.polidea.rxandroidble.sample.SampleApplication;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import static com.trello.rxlifecycle.android.ActivityEvent.DESTROY;
import static com.trello.rxlifecycle.android.ActivityEvent.PAUSE;
import static java.util.concurrent.TimeUnit.SECONDS;

public class RssiPeriodicExampleActivity extends RxAppCompatActivity {

    @BindView(R.id.connection_state)
    TextView connectionStateView;
    @BindView(R.id.rssi)
    TextView rssiView;
    @BindView(R.id.connect_toggle)
    Button connectButton;
    private RxBleDevice bleDevice;
    private Subscription connectionSubscription;

    @OnClick(R.id.connect_toggle)
    public void onConnectToggleClick() {

        if (isConnected()) {
            triggerDisconnect();
        } else {
            connectionSubscription = bleDevice.establishConnection(this, false)
                    .compose(bindUntilEvent(PAUSE))
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnUnsubscribe(this::clearSubscription)
                    .flatMap(rxBleConnection -> // Set desired interval.
                            Observable.interval(2, SECONDS).flatMap(sequence -> rxBleConnection.readRssi()))
                    .subscribe(this::updateRssi, this::onConnectionFailure);
        }
    }

    private void updateRssi(int rssiValue) {
        rssiView.setText(getString(R.string.read_rssi, rssiValue));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example5);
        ButterKnife.bind(this);
        String macAddress = getIntent().getStringExtra(DeviceActivity.EXTRA_MAC_ADDRESS);
        setTitle(getString(R.string.mac_address, macAddress));
        bleDevice = SampleApplication.getRxBleClient(this).getBleDevice(macAddress);

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

    private void onConnectionStateChange(RxBleConnection.RxBleConnectionState newState) {
        connectionStateView.setText(newState.toString());
        updateUI();
    }

    private void clearSubscription() {
        connectionSubscription = null;
        updateUI();
    }

    private void triggerDisconnect() {

        if (connectionSubscription != null) {
            connectionSubscription.unsubscribe();
        }
    }

    private void updateUI() {
        final boolean connected = isConnected();
        connectButton.setText(connected ? R.string.disconnect : R.string.connect);
    }
}
