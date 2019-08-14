package com.example.fitness;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class DeviceService extends Service {
    public DeviceService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
