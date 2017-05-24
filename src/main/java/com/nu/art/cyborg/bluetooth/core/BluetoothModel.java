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

import com.nu.art.belog.Logger;
import com.nu.art.core.exceptions.runtime.ClassInstantiationRuntimeException;
import com.nu.art.core.tools.ArrayTools;
import com.nu.art.cyborg.bluetooth.constants.BT_ConnectionState;
import com.nu.art.cyborg.bluetooth.interfaces.BT_DeviceListener;
import com.nu.art.cyborg.bluetooth.interfaces.OnIncomingPacketListener;

import java.util.HashMap;

public abstract class BluetoothModel<DeviceType extends CyborgBT_Device<PacketType>, PacketType extends BluetoothPacket>
		extends Logger {

	private BT_DeviceListener<DeviceType>[] deviceStateChangedListeners = new BT_DeviceListener[0];

	private OnIncomingPacketListener<DeviceType, PacketType>[] packetListeners = new OnIncomingPacketListener[0];

	private HashMap<String, DeviceType> devicesFound = new HashMap<String, DeviceType>();

	private final Class<DeviceType> deviceType;

	protected BluetoothModel(Class<DeviceType> deviceType) {
		super();
		this.deviceType = deviceType;
	}

	public final void addOnIncomingPacketListener(OnIncomingPacketListener<DeviceType, PacketType> incomingPacketListener) {
		packetListeners = ArrayTools.appendElement(packetListeners, incomingPacketListener);
	}

	public final void removeOnIncomingPacketListener(OnIncomingPacketListener<DeviceType, PacketType> incomingPacketListener) {
		packetListeners = ArrayTools.removeElement(packetListeners, incomingPacketListener);
	}

	public final void addDeviceStateChangedListener(BT_DeviceListener<DeviceType> deviceStateChangedListener) {
		deviceStateChangedListeners = ArrayTools.appendElement(deviceStateChangedListeners, deviceStateChangedListener);
	}

	public final void removeDeviceStateChangedListener(BT_DeviceListener<DeviceType> deviceStateChangedListener) {
		deviceStateChangedListeners = ArrayTools.removeElement(deviceStateChangedListeners, deviceStateChangedListener);
	}

	public final DeviceType getDevice(String address) {
		return devicesFound.get(address);
	}

	public final DeviceType[] getAllDevices() {
		return ArrayTools.asArray(devicesFound.values(), deviceType);
	}

	/**
	 * Removes all the mapped instances of the Devices registered in this BluetoothModel.
	 */
	public final void clear() {
		devicesFound.clear();
	}

	/**
	 * This method is for development purposes only... when you need to simulate the BT instances, and manipulate their
	 * states!
	 *
	 * @param remoteDevice
	 * @param newState
	 */
	public final void changeBT_DeviceState(DeviceType remoteDevice, BT_ConnectionState newState) {
		BT_ConnectionState previousState = remoteDevice.getState();
		if (!remoteDevice.setState(newState))
			return;

		for (BT_DeviceListener<DeviceType> listener : deviceStateChangedListeners)
			listener.onBT_DeviceStateChange(remoteDevice, previousState, newState);
	}

	@SuppressWarnings("unchecked")
	final void onIncomingPacket(CyborgBT_Device<PacketType> cyborgBT_Device, PacketType packet) {
		DeviceType device = (DeviceType) cyborgBT_Device;
		for (OnIncomingPacketListener<DeviceType, PacketType> listener : packetListeners)
			listener.onIncomingPacket(device, packet);
	}

	final void changeBT_DeviceState(BluetoothDevice device, BT_ConnectionState newState) {
		DeviceType remoteDevice = devicesFound.get(device.getAddress());
		if (remoteDevice == null)
			return;
		changeBT_DeviceState(remoteDevice, newState);
	}

	final void newBT_DeviceDetected(BluetoothDevice androidBT_Device) {
		if (devicesFound.get(androidBT_Device.getAddress()) != null) {
			logDebug("Bluetooth device " + androidBT_Device + " - Already detected.");
			return;
		}

		DeviceType remoteDevice;
		try {
			remoteDevice = deviceType.newInstance();
		} catch (Exception e) {
			throw new ClassInstantiationRuntimeException(deviceType, e);
		}

		remoteDevice.setBluetoothDevice(androidBT_Device);
		remoteDevice.setModel(this);
		devicesFound.put(androidBT_Device.getAddress(), remoteDevice);
		setupNewRemoteBT_Device(remoteDevice);
		logDebug("Remote Bluetooth device instance created: '" + deviceType.getSimpleName() + "'==" + remoteDevice + "");

		for (BT_DeviceListener<DeviceType> listener : deviceStateChangedListeners)
			listener.onNewBT_DeviceDetected(remoteDevice);
	}

	protected abstract void setupNewRemoteBT_Device(DeviceType cyborgBT_Device);

	protected abstract boolean isValidDevice(BluetoothDevice androidBT_Device);

	protected abstract boolean allDevicesFound();
}
