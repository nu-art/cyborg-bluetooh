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

import com.nu.art.belog.Logger;
import com.nu.art.core.exceptions.runtime.BadImplementationException;
import com.nu.art.cyborg.bluetooth.constants.BT_ConnectionState;
import com.nu.art.cyborg.bluetooth.core.BluetoothModule.BluetoothConnectivityMethod;
import com.nu.art.cyborg.bluetooth.exceptions.BluetoothConnectionException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import static com.nu.art.cyborg.bluetooth.core.BluetoothModule.BluetoothConnectivityMethod.Reflection;
import static com.nu.art.cyborg.bluetooth.core.BluetoothModule.BluetoothConnectivityMethod.Standard;
import static com.nu.art.cyborg.bluetooth.core.BluetoothModule.BluetoothConnectivityMethod.TBD;

public abstract class CyborgBT_Device
		extends Logger {

	private final class PacketReceiver
			implements Runnable {

		private boolean stop;

		private boolean neverUsed = true;

		private IOException injectedCloseException;

		@Override
		public void run() {
			InputStream inputStream;
			try {
				stop = false;
				neverUsed = true;
				inputStream = socket.getInputStream();
				while (!stop) {
					Packet packet = extractPacket(inputStream);
					lastPacket = packet;
					logDebug("New Packet received from Remote BT Device: " + CyborgBT_Device.this + "\n  Data: " + packet.toString());
					synchronized (this) {
						model.onIncomingPacket(CyborgBT_Device.this, packet);
					}
					neverUsed = false;
				}
			} catch (IOException e) {
				if (injectedCloseException != null)
					logError("----- App Injected Error -----\n BT Device: " + CyborgBT_Device.this, injectedCloseException);
				else if (stop)
					logInfo("Silent BT packet reader exception: " + e);
				else if (neverUsed) {
					logError("----- So far two things can be wrong... \n" + "1. The remote peer didn't send a packet yet, and the connection was dropped.\n" + "2. Your phone has a 'Native Bluetooth Error' and you should restart your phone!!!", e);
				} else {
					logError("----- BLUETOOTH PACKET READING IO ERROR -----\n BT Device: " + CyborgBT_Device.this, e);
					logError("----- Remote peer Lost... \n");
				}
			} catch (RuntimeException e) {
				logError("----- BLUETOOTH LISTENER ERROR -----\n BT Device: " + CyborgBT_Device.this, e);
				throw e;
			} finally {
				neverUsed = true;
				cleanup();
			}
		}
	}

	private BluetoothDevice bluetoothDevice;

	private BluetoothSocket socket;

	private Thread socketListeningThread;

	protected Throwable lastException;

	private Packet lastPacket;

	private final PacketReceiver socketListener = new PacketReceiver();

	private volatile BT_ConnectionState state = BT_ConnectionState.ACL_Disconnected;

	private volatile boolean connecting;

	private BluetoothModel model;

	private String uuid;

	final void setModel(BluetoothModel model) {
		this.model = model;
	}

	final void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
		this.bluetoothDevice = bluetoothDevice;
		setTag(bluetoothDevice.getName());
	}

	protected final BluetoothDevice getBluetoothDevice() {
		return bluetoothDevice;
	}

	final synchronized BluetoothConnectivityMethod connectToDevice(BluetoothConnectivityMethod connectingMethod)
			throws BluetoothConnectionException {
		if (socket != null)
			throw new BadImplementationException("Error socket is not null!!");
		connecting = true;
		logInfo("+---+ Connecting to device...");

		try {
			lastException = null;
			lastPacket = null;
			if (connectingMethod == Reflection || connectingMethod == TBD)
				try {
					socket = fetchBT_Socket_Reflection(1);
					connectToSocket(socket);
					listenForIncomingSPP_Packets();
					onConnectionEstablished();
					return Reflection;
				} catch (BluetoothConnectionException e) {
					socket = null;
					if (connectingMethod == Reflection) {
						throw e;
					}
					logWarning("Error creating socket!", e);
				}

			if (connectingMethod == Standard || connectingMethod == TBD)
				try {
					socket = fetchBT_Socket_Normal();
					connectToSocket(socket);
					listenForIncomingSPP_Packets();
					onConnectionEstablished();
					return Standard;
				} catch (BluetoothConnectionException e) {
					socket = null;
					if (connectingMethod == Standard) {
						throw e;
					}
					logWarning("Error creating socket!", e);
				}

			throw new BluetoothConnectionException("Error creating RFcomm socket for BT Device:" + this + "\n BAD connectingMethod==" + connectingMethod);
		} finally {
			connecting = false;
		}
	}

	private void connectToSocket(BluetoothSocket socket)
			throws BluetoothConnectionException {
		try {
			logInfo("+---+ Connecting to socket...");
			socket.connect();
			logInfo("+---+ Connected to socket");
		} catch (IOException e) {
			try {
				socket.close();
			} catch (IOException e1) {
				logError("Error while closing socket", e1);
			}
			throw new BluetoothConnectionException("Error connecting to socket with Device" + this, e);
		}
	}

	private BluetoothSocket fetchBT_Socket_Normal()
			throws BluetoothConnectionException {
		try {
			logInfo("+---+ Fetching BT RFcomm Socket standard for UUID: " + uuid + "...");
			return bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(uuid));
		} catch (Exception e) {
			throw new BluetoothConnectionException("Error Fetching BT RFcomm Socket!", e);
		}
	}

	private BluetoothSocket fetchBT_Socket_Reflection(int connectionIndex)
			throws BluetoothConnectionException {
		Method m;
		try {
			logInfo("+---+ Fetching BT RFcomm Socket workaround index " + connectionIndex + "...");
			m = bluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
			return (BluetoothSocket) m.invoke(bluetoothDevice, connectionIndex);
		} catch (Exception e) {
			throw new BluetoothConnectionException("Error Fetching BT RFcomm Socket!", e);
		}
	}

	private void onConnectionEstablished() {
		logInfo("+---+ Connection established");
	}

	protected final synchronized void disconnect(IOException injectedCloseException)
			throws IOException {
		socketListener.injectedCloseException = injectedCloseException;
		disconnect();
	}

	final synchronized void disconnect()
			throws IOException {
		if (socket == null || connecting)
			return;
		model.changeBT_DeviceState(bluetoothDevice, BT_ConnectionState.ACL_Disconnecting);

		logInfo("+---+ Disconnecting...");
		connecting = true;
		socketListener.stop = true;
		socket.close();
	}

	private void cleanup() {
		lastPacket = null;
		socketListeningThread = null;
		socket = null;
		onDisconnectionCompleted();
		connecting = false;
		model.changeBT_DeviceState(bluetoothDevice, BT_ConnectionState.ACL_Disconnected);
		logInfo("+---+ Disconnection completed");
	}

	protected void onDisconnectionCompleted() {}

	private synchronized void listenForIncomingSPP_Packets() {
		if (socketListeningThread != null)
			throw new BadImplementationException("Already listening on Socket for BT Device" + this);
		logInfo("+---+ Listening for incoming packets");
		socketListeningThread = new Thread(socketListener, "Packet Listener - " + bluetoothDevice.getName());
		socketListeningThread.start();
	}

	protected void sendSPP_Packet(Packet packet)
			throws IOException {
		if (socket == null) {
			throw new IOException("Cannot send spp packet, No ACL Connection to BT Device: " + this);
		}

		if (state != BT_ConnectionState.SPP_Connected) {
			throw new IOException("Cannot send spp packet, No SPP Connection to BT Device: " + this);
		}

		OutputStream os = socket.getOutputStream();
		serializePacket(os, packet);
	}

	protected abstract void serializePacket(OutputStream os, Packet packet)
			throws IOException;

	public final boolean isConnecting() {
		return connecting;
	}

	public final boolean isSPP_Connected() {
		return state == BT_ConnectionState.SPP_Connected;
	}

	public final boolean isConnected() {
		return socket != null;
	}

	public final String getUuid() {
		return uuid;
	}

	public final void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public final String getName() {
		return bluetoothDevice.getName();
	}

	public final String getAddress() {
		return bluetoothDevice.getAddress();
	}

	final boolean setState(BT_ConnectionState newState) {
		if (state.ordinal() > newState.ordinal() && state != BT_ConnectionState.ACL_Disconnected) {
			logWarning("Bluetooth device state hierarchy is not ordinal!!!");
			return false;
		}
		this.state = newState;
		return true;
	}

	public final Throwable getLastException() {
		return lastException;
	}

	public final void setError(Throwable e) {
		lastException = e;
	}

	public final Packet getLastPacket() {
		return lastPacket;
	}

	public final BT_ConnectionState getState() {
		return state;
	}

	public abstract Packet extractPacket(InputStream inputStream)
			throws IOException;

	@Override
	public String toString() {
		String toRet = "";
		toRet += getName() + "@[" + getAddress() + "]:<" + state.getLabel() + ">";
		return toRet;
	}
}
