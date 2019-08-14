package com.example.fitness;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends Activity {
    private static int REQUEST_ENABLE_BT = 1;

    private RecyclerView deviceList;
    private RecyclerView.LayoutManager deviceListLayout;
    private DeviceListAdapter deviceAdapter;
    private BluetoothAdapter bluetoothAdapter;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        deviceList = findViewById(R.id.device_list);
        deviceList.setHasFixedSize(true);

        deviceListLayout = new LinearLayoutManager(this);
        deviceList.setLayoutManager(deviceListLayout);

        deviceAdapter = new DeviceListAdapter(new DeviceListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BluetoothDevice device) {
                Log.d("FITNESS_SCAN", String.format("clicked on %s", device.getAddress()));
            }
        });
        deviceList.setAdapter(deviceAdapter);

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
            }
        };

        setupBluetooth();
    }

    private void setupBluetooth() {
        final BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = manager.getAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBt, REQUEST_ENABLE_BT);
        }
    }

    private void scan(final boolean enabled) {
        final BluetoothAdapter.LeScanCallback callback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (deviceAdapter.addDevice(bluetoothDevice)) {
                            deviceAdapter.notifyDataSetChanged();
                            Log.d("FITNESS_SCAN", String.format("device discovered: %s", bluetoothDevice.getName()));
                        }
                    }
                });
            }
        };

        if (enabled) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bluetoothAdapter.stopLeScan(callback);
                    Log.d("FITNESS_SCAN", "stop scan");
                }
            }, 10000);

            bluetoothAdapter.startLeScan(callback);
            Log.d("FITNESS_SCAN", "start scan");
        } else {
            bluetoothAdapter.stopLeScan(callback);
        }
    }

    private void connect(BluetoothDevice device) {

    }

    @Override
    protected void onStop() {
        super.onStop();
        scan(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        scan(true);
    }
}
