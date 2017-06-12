package com.nu.art.cyborg.bluetooth.core;

import android.bluetooth.BluetoothSocket;

import com.nu.art.cyborg.bluetooth.exceptions.BluetoothConnectionException;

import java.lang.reflect.Method;
import java.util.UUID;

public enum ConnectivityType {
	Reflective {
		@Override
		protected BluetoothSocket createSocket(CyborgBT_Device device)
				throws BluetoothConnectionException {
			Method m;
			try {
				device.logInfo("+---+ Fetching BT RFcomm Socket workaround index " + 1 + "...");
				m = device.getBluetoothDevice().getClass().getMethod("createRfcommSocket", new Class[]{int.class});
				return (BluetoothSocket) m.invoke(device.getBluetoothDevice(), 1);
			} catch (Exception e) {
				throw new BluetoothConnectionException("Error Fetching BT RFcomm Socket!", e);
			}
		}
	},

	Secured {
		@Override
		protected BluetoothSocket createSocket(CyborgBT_Device device)
				throws BluetoothConnectionException {
			try {
				device.logInfo("+---+ Fetching BT RFcomm Socket standard for UUID: " + device.uuid + "...");
				return device.getBluetoothDevice().createRfcommSocketToServiceRecord(UUID.fromString(device.uuid));
			} catch (Exception e) {
				throw new BluetoothConnectionException("Error Fetching BT RFcomm Socket!", e);
			}
		}
	},

	Insecure {
		@Override
		protected BluetoothSocket createSocket(CyborgBT_Device device)
				throws BluetoothConnectionException {
			try {
				device.logInfo("+---+ Fetching BT Insecure RFcomm Socket standard for UUID: " + device.uuid + "...");
				return device.getBluetoothDevice().createInsecureRfcommSocketToServiceRecord(UUID.fromString(device.uuid));
			} catch (Exception e) {
				throw new BluetoothConnectionException("Error Fetching BT Insecure RFcomm Socket!", e);
			}
		}
	};

	protected abstract BluetoothSocket createSocket(CyborgBT_Device device)
			throws BluetoothConnectionException;
}