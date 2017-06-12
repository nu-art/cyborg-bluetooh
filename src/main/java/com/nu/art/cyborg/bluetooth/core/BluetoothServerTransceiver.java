package com.nu.art.cyborg.bluetooth.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;

import com.nu.art.cyborg.io.transceiver.PacketSerializer;
import com.nu.art.cyborg.io.transceiver.SocketWrapper;

import java.io.IOException;
import java.util.UUID;

public class BluetoothServerTransceiver
		extends BluetoothTransceiver {

	private final String name;

	private final BluetoothAdapter btAdapter;

	private BluetoothServerSocket serverSocket;

	BluetoothServerTransceiver(BluetoothAdapter btAdapter, String name, String uuid, PacketSerializer packetSerializer) {
		super(name, uuid, packetSerializer);
		this.name = name;
		this.btAdapter = btAdapter;
	}

	public final SocketWrapper connectImpl()
			throws IOException {
		serverSocket = btAdapter.listenUsingInsecureRfcommWithServiceRecord(name, UUID.fromString(uuid));
		return new BluetoothSocketWrapper(serverSocket.accept(-1));
	}

	public void disconnect() {
		super.disconnect();
		try {
			serverSocket.close();
		} catch (IOException e) {
			notifyError(e);
		}
	}
}
