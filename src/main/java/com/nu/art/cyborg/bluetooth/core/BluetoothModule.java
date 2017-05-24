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

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.preference.PreferenceManager.OnActivityResultListener;
import android.provider.Settings.Secure;
import android.util.Log;

import com.nu.art.core.generics.Processor;
import com.nu.art.cyborg.annotations.ModuleDescriptor;
import com.nu.art.cyborg.bluetooth.constants.BT_AdapterState;
import com.nu.art.cyborg.bluetooth.constants.BT_ConnectionState;
import com.nu.art.cyborg.core.ActivityStack.ActivityStackAction;
import com.nu.art.cyborg.core.CyborgActivityBridge;
import com.nu.art.cyborg.core.CyborgModule;
import com.nu.art.cyborg.core.modules.PreferencesModule;
import com.nu.art.cyborg.core.modules.PreferencesModule.PreferenceEnum;
import com.nu.art.cyborg.core.modules.ThreadsModule;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import static com.nu.art.cyborg.bluetooth.constants.BT_AdapterState.TurningOff;
import static com.nu.art.cyborg.bluetooth.core.BluetoothModule.BluetoothConnectivityMethod.TBD;

@ModuleDescriptor(usesPermissions = {
		permission.BLUETOOTH,
		permission.BLUETOOTH_ADMIN
})
public final class BluetoothModule
		extends CyborgModule {

	private static final int DefaultMaximumInquiryDuration = 30000;

	public interface BluetoothAdapterListener {

		void onBluetoothAdapterOn();

		void onBluetoothAdapterOff();
	}

	enum BluetoothConnectivityMethod {
		/**
		 * The Bluetooth connection method needs to be decided.
		 */
		TBD,

		/**
		 * Connecting the Bluetooth adapter in the <b>standard</b> way.
		 */
		Standard,

		/**
		 * Connecting the Bluetooth adapter in the <b>reflective</b> way.
		 */
		Reflection
	}

	public static final int EnableBluetoothIntentActionCode = 10;

	private static Method createRfcommSocket_Method;

	static {
		try {
			createRfcommSocket_Method = BluetoothDevice.class.getMethod("createRfcommSocket", new Class[]{int.class});
			createRfcommSocket_Method.setAccessible(true);
		} catch (Exception e) {
			Log.e("BluetoothModule", "Error getting method via reflection for workaround", e);
		}
	}

	private BluetoothAdapter btAdapter;

	private String macAddress;

	private boolean forcedStop;

	private long inquiryStartedTimeStamp = -1;

	private int maximumInquiryDuration = DefaultMaximumInquiryDuration;

	private PreferenceEnum<BluetoothConnectivityMethod> phoneConnectivityMethod;

	/**
	 * OtherImplementation Consider making this a Vector of {@link BluetoothModule}
	 */
	private BluetoothModel<?, ?> model;

	private BT_AdapterState state;

	@Override
	@SuppressLint("HardwareIds")
	protected void init() {
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		if (btAdapter == null) {
			toastDebug("Device does not support Bluetooth!");
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				logError(e);
			}
			throw new RuntimeException("Device does not support Bluetooth!");
		}

		macAddress = Secure.getString(getContentResolver(), "bluetooth_address");
		registerReceiver(BT_AdapterReceiver.class);
		phoneConnectivityMethod = getModule(PreferencesModule.class).new PreferenceEnum<>("ConnectivityMethod", BluetoothConnectivityMethod.class, TBD);
		setState(btAdapter.isEnabled() ? BT_AdapterState.On : BT_AdapterState.Off);
	}

	public final String getMyMacAddress() {
		return macAddress;
	}

	@Override
	protected void printModuleDetails() {
		logInfo("Bluetooth MAC: " + macAddress);
	}

	synchronized void setState(BT_AdapterState state) {
		if (this.state == state)
			return;

		logDebug("Adapter sate changed: " + this.state + " ==> " + state);
		this.state = state;
	}

	public synchronized final boolean isState(BT_AdapterState state) {
		return this.state == state;
	}

	private void setRealState() {
		setState(btAdapter.isEnabled() ? BT_AdapterState.On : BT_AdapterState.Off);
	}

	public final void turnBluetoothOn() {
		if (btAdapter.isEnabled()) {
			logInfo("Bluetooth adapter is already ON");
			dispatchBluetoothTurnedOn();
			return;
		}

		logInfo("Turning bluetooth adapter ON... waiting for UI");
		postActivityAction(new ActivityStackAction() {
			@Override
			public void execute(CyborgActivityBridge activity) {

				logInfo("Turning bluetooth adapter ON...");
				setRealState();

				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				activity.startActivityForResult(enableBtIntent, EnableBluetoothIntentActionCode);
				activity.addResultListener(new OnActivityResultListener() {

					@Override
					public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
						if (requestCode != BluetoothModule.EnableBluetoothIntentActionCode)
							return false;

						if (resultCode != Activity.RESULT_OK) {
							setRealState();
						}

						if (isState(BT_AdapterState.Off))
							dispatchBluetoothTurnedOff();
						else
							dispatchBluetoothTurnedOn();

						return true;
					}
				});

				btAdapter.enable();
				setState(BT_AdapterState.TurningOn);
			}
		});
	}

	public final void turnBluetoothOff() {
		if (!btAdapter.isEnabled()) {
			logInfo("Bluetooth adapter is already OFF");
			dispatchBluetoothTurnedOff();
			return;
		}

		logInfo("Turning bluetooth adapter OFF...");
		setState(TurningOff);
		btAdapter.disable();
	}

	public final boolean startInquiry() {
		if (!btAdapter.isEnabled()) {
			logDebug("Will not start inquiry, adapter is disabled");
			return false;
		}
		if (isInquiryInProcess()) {
			logDebug("Will not start inquiry, already in progress");
			return false;
		}

		if (model.allDevicesFound()) {
			logDebug("Will not start inquiry, all devices found");
			return false;
		}

		logInfo("Starting inquiry");
		inquiryStartedTimeStamp = System.currentTimeMillis();
		btAdapter.startDiscovery();
		return true;
	}

	public final boolean isInquiryInProcess() {
		return btAdapter.isDiscovering();
	}

	public final void stopInquiry() {
		if (!isInquiryInProcess())
			return;

		forcedStop = true;
		btAdapter.cancelDiscovery();
	}

	public final void setMaximumInquiryDuration(int maximumInquiryDuration) {
		this.maximumInquiryDuration = maximumInquiryDuration;
	}

	public final void cancelDiscovery() {
		btAdapter.cancelDiscovery();
	}

	public final void setModel(BluetoothModel<?, ?> model) {
		this.model = model;
		cyborg.setBeLogged(model);
	}

	public final void disable() {
		unregisterReceiver(BT_AdapterReceiver.class);
	}

	final void newBT_DeviceDetected(BluetoothDevice androidBT_Device) {
		if (!model.isValidDevice(androidBT_Device)) {
			logDebug("Bluetooth device Does not match criteria: " + androidBT_Device.getName() + " (" + androidBT_Device.getAddress() + ")");
			return;
		}

		logInfo("Bluetooth device found: " + androidBT_Device.getName() + ", " + androidBT_Device.getBluetoothClass() + ", " + androidBT_Device.getAddress());
		model.newBT_DeviceDetected(androidBT_Device);

		if (model.allDevicesFound()) {
			toastDebug("Inquiry Completed");
			setState(BT_AdapterState.On);
			stopInquiry();
		}
	}

	@SuppressWarnings("unchecked")
	final <DeviceType extends CyborgBT_Device<?>> void deviceStateChange(BluetoothDevice device, BT_ConnectionState newState) {
		DeviceType _device = (DeviceType) model.getDevice(device.getAddress());
		if (_device == null)
			return;
		((BluetoothModel<DeviceType, ?>) model).changeBT_DeviceState(_device, newState);
	}

	public BluetoothAdapter getBT_Adapter() {
		return btAdapter;
	}

	final void onInquiryCompleted() {
		if (forcedStop) {
			logDebug("Inquiry has been canceled. Inquiry duration '" + maximumInquiryDuration + "ms' Inquiry is not completed");
			forcedStop = false;
			return;
		}

		if (model.allDevicesFound()) {
			return;
		}

		if (System.currentTimeMillis() - inquiryStartedTimeStamp > maximumInquiryDuration) {
			logDebug("Inquiry duration '" + maximumInquiryDuration + "ms' has expired. Inquiry is not completed");
			return;
		}

		logDebug("Inquiring again not all device are found... and timer did not expire yet");
		btAdapter.startDiscovery();
		return;
	}

	class DevicesConnectivityHandler {

		private final Handler handler;

		private ArrayList<CyborgBT_Device<?>> inProgress = new ArrayList<>();

		protected boolean stopConnecting = false;

		DevicesConnectivityHandler() {
			handler = getModule(ThreadsModule.class).getDefaultHandler("bt-connectivity-thread");
		}

		void stop() {
			stopConnecting = false;
			handler.removeCallbacks(null);
		}

		void connect(final CyborgBT_Device<?>... devices) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					for (CyborgBT_Device<?> remoteDevice : devices) {
						if (inProgress.contains(remoteDevice) || remoteDevice.isConnected()) {
							logDebug("Will not connect to device... already in connecting");
							continue;
						}

						if (stopConnecting) {
							logDebug("Will not connect to device... canceling");
							continue;
						}

						try {
							inProgress.add(remoteDevice);
							connectToRemoteDevice(remoteDevice);
						} finally {
							inProgress.remove(remoteDevice);
						}
					}
				}
			});
		}

		void disconnect(final CyborgBT_Device<?>... devices) {
			for (final CyborgBT_Device<?> remoteDevice : devices) {
				if (inProgress.contains(remoteDevice))
					continue;

				inProgress.add(remoteDevice);

				Thread disconnectingThread = new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							remoteDevice.disconnect();
						} catch (Exception e) {
							logError("Error connecting too BT Device: " + remoteDevice, e);
							remoteDevice.setError(e);
						} finally {
							inProgress.remove(remoteDevice);
						}
					}
				}, "BT Device(" + remoteDevice.getBluetoothDevice().getName() + ") - Connecting thread");
				disconnectingThread.start();
			}
		}
	}

	private void connectToRemoteDevice(CyborgBT_Device<?> remoteDevice) {
		try {
			deviceStateChange(remoteDevice.getBluetoothDevice(), BT_ConnectionState.ACL_Connecting);
			BluetoothConnectivityMethod connectedMethod = remoteDevice.connectToDevice(phoneConnectivityMethod.get());
			phoneConnectivityMethod.set(connectedMethod);

			// The ACL_Connected state should come from the Android frame work via the bluetooth broadcast receiver
			deviceStateChange(remoteDevice.getBluetoothDevice(), BT_ConnectionState.SPP_Connected);
		} catch (Exception e) {
			logError("Error connecting to BT Device: " + remoteDevice, e);
			deviceStateChange(remoteDevice.getBluetoothDevice(), BT_ConnectionState.ACL_Disconnected);
			remoteDevice.setError(e);
			try {
				remoteDevice.disconnect();
			} catch (IOException e1) {
				logError("Error disconnecting from BT Device: " + remoteDevice, e);
			}
		}
	}

	private void dispatchBluetoothTurnedOn() {
		dispatchEvent("Bluetooth adapter is on", BluetoothAdapterListener.class, new Processor<BluetoothAdapterListener>() {
			@Override
			public void process(BluetoothAdapterListener listener) {
				listener.onBluetoothAdapterOn();
			}
		});
	}

	private void dispatchBluetoothTurnedOff() {
		dispatchEvent("Bluetooth adapter is off", BluetoothAdapterListener.class, new Processor<BluetoothAdapterListener>() {
			@Override
			public void process(BluetoothAdapterListener listener) {
				listener.onBluetoothAdapterOff();
			}
		});
	}
}
