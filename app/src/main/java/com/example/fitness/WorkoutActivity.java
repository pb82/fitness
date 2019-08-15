package com.example.fitness;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.UUID;

public class WorkoutActivity extends AppCompatActivity {
    private BluetoothGattCallback callback;
    private BroadcastReceiver gattReceiver;
    private BluetoothGatt globalGatt;

    public WorkoutActivity() {
        gattReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("FITNESS_SCAN", "received broadcast");
            }
        };

        callback = new BluetoothGattCallback() {
            public String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
            public final UUID UUID_HEART_RATE_MEASUREMENT =
                    UUID.fromString(HEART_RATE_MEASUREMENT);

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d("FITNESS_SCAN", "device connected");
                    boolean success = globalGatt.discoverServices();
                    if (!success) {
                        Log.d("FITNESS_SCAN", "false discovering services");
                    } else {
                        Log.d("FITNESS_SCAN", "discovering services");
                    }
                }
            }

            private final UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

            public boolean setCharacteristicNotification(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic characteristic,boolean enable) {
                bluetoothGatt.setCharacteristicNotification(characteristic, enable);
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
                descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : new byte[]{0x00, 0x00});
                return bluetoothGatt.writeDescriptor(descriptor); //descriptor write operation successfully started?
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    for (BluetoothGattService service : globalGatt.getServices()) {
                        if (service.getUuid().toString().equals("0000180d-0000-1000-8000-00805f9b34fb")) {
                            Log.d("FITNESS_SCAN", String.format("service uuid: %s", service.getUuid().toString()));
                            for (BluetoothGattCharacteristic c : service.getCharacteristics()) {
                                if (c.getUuid().toString().equals("00002a37-0000-1000-8000-00805f9b34fb")) {
                                    boolean success = setCharacteristicNotification(gatt, c, true);
                                    if (success) {
                                        Log.d("FITNESS_SCAN", "receiving notifications");
                                    } else {
                                        Log.d("FITNESS_SCAN", "error notifications");
                                    }
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                int value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
                Log.d("FITNESS_SCAN", String.format("value is %d", value));
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                int value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
                Log.d("FITNESS_SCAN", String.format("value is %d", value));
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorRead(gatt, descriptor, status);
                Log.d("FITNESS_SCAN", "descriptor changed");
            }

            private void broadcastUpdate(final String action,
                                         final BluetoothGattCharacteristic characteristic) {
                int flag = characteristic.getProperties();
                int format = -1;
                if ((flag & 0x01) != 0) {
                    format = BluetoothGattCharacteristic.FORMAT_UINT16;
                } else {
                    format = BluetoothGattCharacteristic.FORMAT_UINT8;
                }
                final int heartRate = characteristic.getIntValue(format, 1);
                Log.d("FITNESS_SCAN", String.format("Received heart rate: %d", heartRate));
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        BluetoothDevice device = intent.getParcelableExtra("device");
        if (device == null) {
            Log.e("FITNESS_APP", "no device passed");
        } else {
            Log.e("FITNESS_APP", "connecting to device");
            globalGatt = device.connectGatt(this, false, callback);
            Log.e("FITNESS_APP", "connected");
        }
    }
}
