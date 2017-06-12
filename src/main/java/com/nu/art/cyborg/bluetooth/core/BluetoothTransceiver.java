package com.nu.art.cyborg.bluetooth.core;

import android.bluetooth.BluetoothSocket;

import com.nu.art.cyborg.io.transceiver.BaseTransceiver;
import com.nu.art.cyborg.io.transceiver.PacketSerializer;
import com.nu.art.cyborg.io.transceiver.SocketWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class BluetoothTransceiver
		extends BaseTransceiver {

	class BluetoothSocketWrapper
			implements SocketWrapper {

		final BluetoothSocket socket;

		BluetoothSocketWrapper(BluetoothSocket socket)
				throws IOException {
			this.socket = socket;
		}

		@Override
		public OutputStream getOutputStream()
				throws IOException {
			return socket.getOutputStream();
		}

		@Override
		public InputStream getInputStream()
				throws IOException {
			return socket.getInputStream();
		}

		@Override
		public void close()
				throws IOException {
			socket.close();
		}
	}

	final String uuid;

	BluetoothTransceiver(String name, String uuid, PacketSerializer packetSerializer) {
		super(name, packetSerializer);
		this.uuid = uuid;
	}
}