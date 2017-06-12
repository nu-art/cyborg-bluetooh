package com.nu.art.cyborg.bluetooth.core.interfaces;

import android.bluetooth.BluetoothDevice;

public interface BluetoothDeviceListener {

	void onNewDeviceDetected(BluetoothDevice device);
}
