package com.nu.art.cyborg.bluetooth.core.interfaces;

import com.nu.art.cyborg.bluetooth.constants.BT_AdapterState;

public interface BluetoothAdapterListener {

	void onBluetoothAdapterStateChanged(BT_AdapterState state);
}
