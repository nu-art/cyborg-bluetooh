/*
 * Copyright 2008-2013, 'Nu-Art Software' AND/OR 'Adam Zehavi AKA TacB0sS'
 *
 * -------------------  !!! THIS FILE IS NOT FOR PUBLIC EYES !!!  -------------------
 *
 * If you have obtained a source file with this header without proper authorization,
 *
 * please take the time and report this to: <Support@Nu-Art-Software.com>
 * ----------------------------------------------------------------------------------
 *
 * IF THIS HEADER IS ATTACHED TO ANY SOURCE/RESOURCE FILE, YOU ARE HERE BY INFORMED
 * THAT THE CONTENT OF THIS FILE IS A PROPERTY OF 'Nu-Art Software' AND/OR 'Adam Zehavi AKA TacB0sS.
 * VIEWING THIS CONTENT IS A VIOLATION OF OUR INTELECTUAL PROPERTIES AND/OR PRIVACY
 * UNLESS GIVEN A SPECIFIC WRITTEN AUTHORIZATION, AND/OR YOU HAVE SIGNED
 * 'Nu-Art Software' CYBORG FRAMEWORK NDA.
 * ----------------------------------------------------------------------------------
 *
 * If the above was not clear enough...
 * Without proper authorization, you are not allowed to do anything with this code...
 *
 * Do NOT:
 * VIEW IT !!			AND/OR
 * COPY IT !!			AND/OR
 * USE IT !!			AND/OR
 * DISTRIBUTE IT !!		AND/OR
 * CHANGE IT !!			AND/OR
 * AND/OR DO ANY OTHER THING WITH IT!!!
 *
 * [I'M SURE YOU GET THE PICTURE]
 * ----------------------------------------------------------------------------------
 *
 * Last Builder: TacB0sS
 */
package com.nu.art.cyborg.bluetooth.core;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import com.nu.art.core.exceptions.runtime.BadImplementationException;
import com.nu.art.cyborg.bluetooth.exceptions.BluetoothConnectionException;
import com.nu.art.cyborg.io.transceiver.PacketSerializer;
import com.nu.art.cyborg.io.transceiver.SocketWrapper;

import java.io.IOException;

public class CyborgBT_Device
		extends BluetoothTransceiver {

	private final BluetoothDevice bluetoothDevice;

	private ConnectivityType type = ConnectivityType.Insecure;

	public CyborgBT_Device(BluetoothDevice bluetoothDevice, String uuid, PacketSerializer packetSerializer) {
		super(bluetoothDevice.getName(), uuid, packetSerializer);
		this.bluetoothDevice = bluetoothDevice;
	}

	protected final BluetoothDevice getBluetoothDevice() {
		return bluetoothDevice;
	}

	public SocketWrapper connectImpl()
			throws BluetoothConnectionException, IOException {
		setOneShot();

		logInfo("+---+ Connecting to device...");
		//			connecting = true;
		if (socket != null)
			throw new BadImplementationException("Error socket is not null!!");

		BluetoothSocket socket = type.createSocket(this);
		logInfo("+---+ Connecting to socket...");
		socket.connect();
		logInfo("+---+ Connected to socket");

		return new BluetoothSocketWrapper(socket);
	}

	public final String getName() {
		return bluetoothDevice.getName();
	}

	public final String getAddress() {
		return bluetoothDevice.getAddress();
	}

	@Override
	public String toString() {
		String toRet = "";
		toRet += getName() + "@[" + getAddress() + "]:<" + getState().name() + ">";
		return toRet;
	}

	public void setSocketType(ConnectivityType type) {
		this.type = type;
	}
}
