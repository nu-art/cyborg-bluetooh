/*
 * The cyborg-bluetooth module, meant to simplify your life when
 * dealing with connecting to remote Android devices, and stream data...
 *
 * Copyright (C) 2018  Adam van der Kruk aka TacB0sS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nu.art.cyborg.bluetooth.core;

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.preference.PreferenceManager.OnActivityResultListener;
import android.provider.Settings.Secure;

import com.nu.art.core.generics.Processor;
import com.nu.art.cyborg.annotations.ModuleDescriptor;
import com.nu.art.cyborg.bluetooth.constants.BT_AdapterState;
import com.nu.art.cyborg.bluetooth.constants.BT_ConnectionState;
import com.nu.art.cyborg.bluetooth.core.interfaces.BluetoothAdapterListener;
import com.nu.art.cyborg.bluetooth.core.interfaces.BluetoothDeviceListener;
import com.nu.art.cyborg.bluetooth.core.interfaces.BluetoothInquiryListener;
import com.nu.art.cyborg.core.ActivityStack.ActivityStackAction;
import com.nu.art.cyborg.core.CyborgActivityBridge;
import com.nu.art.cyborg.core.CyborgModule;
import com.nu.art.cyborg.io.transceiver.ConnectionState;
import com.nu.art.cyborg.io.transceiver.PacketSerializer;

import java.lang.reflect.Method;
import java.util.HashMap;

import static com.nu.art.cyborg.bluetooth.constants.BT_AdapterState.Off;
import static com.nu.art.cyborg.bluetooth.constants.BT_AdapterState.On;
import static com.nu.art.cyborg.bluetooth.constants.BT_AdapterState.TurningOff;

@ModuleDescriptor(usesPermissions = {
	permission.BLUETOOTH,
	permission.BLUETOOTH_ADMIN
})
public final class BluetoothModule
	extends CyborgModule
	implements OnActivityResultListener {

	public static final int EnableBluetoothIntentActionCode = 10;

	private BluetoothAdapter btAdapter;

	private String macAddress;

	private BT_AdapterState state = Off;

	private InquiryLogic inquiryLogic;

	private HashMap<String, CyborgBT_Device> registered = new HashMap<>();

	private Runnable turnAdapterOff = new Runnable() {
		@Override
		public void run() {
			logWarning("did not discover any device... could this be a bug??? restarting the BT Adapter");
			turnBluetoothOff();
		}
	};

	@Override
	@SuppressLint("HardwareIds")
	protected void init() {
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		if (btAdapter == null) {
			toastDebug("Device does not support Bluetooth!");
			return;
			//			try {
			//				Thread.sleep(5000);
			//			} catch (InterruptedException e) {
			//				logError(e);
			//			}
			//			throw new RuntimeException("Device does not support Bluetooth!");
		}

		inquiryLogic = new InquiryLogic();
		macAddress = Secure.getString(getContentResolver(), "bluetooth_address");
		registerReceiver(BT_AdapterReceiver.class);
		postOnUI(new Runnable() {
			@Override
			public void run() {
				setState(btAdapter.isEnabled() ? On : BT_AdapterState.Off);
			}
		});
	}

	public final void registerDevice(CyborgBT_Device device) {
		registered.put(device.getAddress(), device);
	}

	public final String getMyMacAddress() {
		return macAddress;
	}

	@Override
	protected void printModuleDetails() {
		logInfo("Bluetooth MAC: " + macAddress);
	}

	void setState(BT_AdapterState state) {
		if (this.state == state)
			return;

		logDebug("Adapter sate changed: " + this.state + " ==> " + state);
		if (this.state == null) {
			this.state = state;
			return;
		}

		this.state = state;
		dispatchBluetoothAdapterStateChanged(this.state);
	}

	public final boolean isState(BT_AdapterState state) {
		return this.state == state;
	}

	private void setRealState() {
		setState(btAdapter.isEnabled() ? On : BT_AdapterState.Off);
	}

	public final void forceDiscovery() {
		Method method;
		final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
		if (!btAdapter.isEnabled())
			return;

		try {
			method = btAdapter.getClass().getMethod("setScanMode", int.class);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		try {
			method.invoke(btAdapter, 23);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}

	public final void turnBluetoothOn_Admin() {
		if (!isState(Off))
			return;

		if (btAdapter.isEnabled()) {
			logInfo("Bluetooth adapter is already ON");
			setState(On);
			return;
		}

		try {
			btAdapter.enable();
		} catch (Exception e) {
			// HACK: Android in some cases crashes here... we'd set the adapter state to off and dispatch the state
			setState(BT_AdapterState.Off);
			dispatchBluetoothAdapterStateChanged(this.state);
			return;
		}

		setState(BT_AdapterState.TurningOn);
	}

	@Override
	public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode != BluetoothModule.EnableBluetoothIntentActionCode)
			return false;

		if (resultCode != Activity.RESULT_OK) {
			setRealState();
		}

		if (isState(BT_AdapterState.Off))
			setState(Off);
		else
			setState(On);

		return true;
	}

	public final void turnBluetoothOn() {
		if (btAdapter.isEnabled()) {
			logInfo("Bluetooth adapter is already ON");
			setState(On);
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

				btAdapter.enable();
				setState(BT_AdapterState.TurningOn);
			}
		});
	}

	public final void turnBluetoothOff() {
		if (!btAdapter.isEnabled()) {
			logInfo("Bluetooth adapter is already OFF");
			setState(Off);
			return;
		}

		logInfo("Turning bluetooth adapter OFF...");
		setState(TurningOff);
		btAdapter.disable();
	}

	final void newDeviceDetected(final BluetoothDevice device) {
		removeActionFromBackground(turnAdapterOff);
		dispatchModuleEvent("New Bluetooth device found: " + device, new Processor<BluetoothDeviceListener>() {
			@Override
			public void process(BluetoothDeviceListener listener) {
				listener.onNewDeviceDetected(device);
			}
		});
	}

	@SuppressWarnings("unchecked")
	final void deviceStateChange(BluetoothDevice device, BT_ConnectionState newState) {
		CyborgBT_Device _device = registered.get(device.getAddress());
		if (_device == null)
			return;

		ConnectionState deviceState = null;
		switch (newState) {
			case ACL_Disconnected:
				deviceState = ConnectionState.Idle;
				break;

			case ACL_Connecting:
			case ACL_Connected:
			case Bonding:
			case SPP_Connected:
			case ACL_Disconnecting:
			default:
				logWarning("STATE: " + newState);
		}

		if (deviceState == null)
			return;

		_device.setState(deviceState);
	}

	public final void stopInquiry() {
		inquiryLogic.stopInquiry();
	}

	public final void startInquiry() {
		inquiryLogic.startInquiry();
	}

	final void onInquiryEnded() {
		inquiryLogic.onInquiryEnded();
	}

	public BluetoothServerTransceiver listen(String name, String uuid, PacketSerializer packetSerializer) {
		return new BluetoothServerTransceiver(btAdapter, name, uuid, packetSerializer);
	}

	public void setAdapterName(String newAdapterName) {
		btAdapter.setName(newAdapterName);
	}

	private class InquiryLogic {

		boolean discovering = false;

		InquiryLogic() {
			discovering = isInquiryInProcess();
		}

		final boolean startInquiry() {
			if (!btAdapter.isEnabled()) {
				logDebug("Will not start inquiry, adapter is disabled");
				return false;
			}

			logInfo("Starting inquiry");
			btAdapter.startDiscovery();
			postOnBackground(20000, turnAdapterOff);
			return true;
		}

		final boolean isInquiryInProcess() {
			return btAdapter.isDiscovering();
		}

		final void stopInquiry() {
			if (!discovering)
				return;

			logDebug("Stopping inquiry");
			btAdapter.cancelDiscovery();
			discovering = isInquiryInProcess();
		}

		final void onInquiryEnded() {
			dispatchGlobalEvent("On inquiry ended", new Processor<BluetoothInquiryListener>() {
				@Override
				public void process(BluetoothInquiryListener listener) {
					listener.onInquiryEnded();
				}
			});
		}
	}

	private void dispatchBluetoothAdapterStateChanged(final BT_AdapterState state) {

		dispatchGlobalEvent("Bluetooth adapter state changed: " + state, new Processor<BluetoothAdapterListener>() {
			@Override
			public void process(BluetoothAdapterListener listener) {
				listener.onBluetoothAdapterStateChanged(state);
			}
		});
	}
}
