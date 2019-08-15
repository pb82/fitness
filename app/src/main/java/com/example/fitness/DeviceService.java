package com.example.fitness;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.UUID;

public class DeviceService extends Service {
    private BluetoothGattCallback callback;

    public DeviceService() {
        Log.d("FITNESS_SCAN", "device service constructor");
    }

    @Override
    public void onCreate() {
        Log.d("FITNESS_SCAN", "onCreate service");

        callback = new BluetoothGattCallback() {
            public String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
            public final UUID UUID_HEART_RATE_MEASUREMENT =
                    UUID.fromString(HEART_RATE_MEASUREMENT);

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d("FITNESS_SCAN", "device connected");
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                if (status ==BluetoothGatt.GATT_SUCCESS) {
                    Log.d("FITNESS_SCAN", "services discovered");
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                broadcastUpdate("ACTION_DATA_AVAIL", characteristic);
            }

            private void broadcastUpdate(final String action,
                                         final BluetoothGattCharacteristic characteristic) {
                final Intent intent = new Intent(action);

                Log.d("FITNESS_APP", String.format("broadcast: %s", characteristic.getUuid()));

                // This is special handling for the Heart Rate Measurement profile. Data
                // parsing is carried out as per profile specifications.
                if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
                    int flag = characteristic.getProperties();
                    int format = -1;
                    if ((flag & 0x01) != 0) {
                        format = BluetoothGattCharacteristic.FORMAT_UINT16;
                        Log.d("FITNESS_SCAN", "Heart rate format UINT16.");
                    } else {
                        format = BluetoothGattCharacteristic.FORMAT_UINT8;
                        Log.d("FITNESS_SCAN", "Heart rate format UINT8.");
                    }
                    final int heartRate = characteristic.getIntValue(format, 1);
                    Log.d("FITNESS_SCAN", String.format("Received heart rate: %d", heartRate));
                } else {
                    // For all other profiles, writes the data formatted in HEX.
                    final byte[] data = characteristic.getValue();
                    if (data != null && data.length > 0) {
                        final StringBuilder stringBuilder = new StringBuilder(data.length);
                        for(byte byteChar : data)
                            stringBuilder.append(String.format("%02X ", byteChar));
                        intent.putExtra("heartRate", new String(data) + "\n" +
                                stringBuilder.toString());
                    }
                }
                sendBroadcast(intent);
            }
        };

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("FITNESS_SCAN", "onStartCommand service");

        BluetoothDevice device = intent.getParcelableExtra("device");
        if (device == null) {
            Log.e("FITNESS_APP", "error receiving bluetooth device in service");
        } else {
            BluetoothGatt gatt = device.connectGatt(this, false, callback);
            Log.d("FITNESS_APP", "gatt connected");
        }

        Log.d("FITNESS_APP", "service started");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
