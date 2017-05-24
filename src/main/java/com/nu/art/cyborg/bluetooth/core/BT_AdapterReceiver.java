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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

import com.nu.art.cyborg.bluetooth.constants.BT_AdapterState;
import com.nu.art.cyborg.bluetooth.constants.BT_ConnectionState;
import com.nu.art.cyborg.core.CyborgReceiver;

public class BT_AdapterReceiver
		extends CyborgReceiver<BluetoothModule> {

	private static final String[] DefaultActions = new String[]{
			BluetoothDevice.ACTION_FOUND,
			BluetoothAdapter.ACTION_DISCOVERY_STARTED,
			BluetoothAdapter.ACTION_SCAN_MODE_CHANGED,
			BluetoothAdapter.ACTION_STATE_CHANGED,
			BluetoothAdapter.ACTION_DISCOVERY_FINISHED,
			BluetoothDevice.ACTION_ACL_CONNECTED,
			BluetoothDevice.ACTION_ACL_DISCONNECTED,
			BluetoothDevice.ACTION_BOND_STATE_CHANGED,
			BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED
	};

	public BT_AdapterReceiver() {
		super(BluetoothModule.class, DefaultActions);
	}

	@Override
	protected void onReceive(Intent intent, BluetoothModule module) {
		String action = intent.getAction();
		switch (action) {
			case BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:
				int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
				if (newState == -1)
					return;

				module.setState(BT_AdapterState.getInstanceForState(newState));
				break;

			case BluetoothAdapter.ACTION_STATE_CHANGED:
				newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
				module.setState(BT_AdapterState.getInstanceForState(newState));
				break;
			case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
				module.setState(BT_AdapterState.Inquiring);
				break;
			case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
				module.setState(BT_AdapterState.InquiringEnded);
				break;
			case BluetoothDevice.ACTION_FOUND:
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				module.newBT_DeviceDetected(device);
				break;
			default:
				logDebug("ACL State: " + action);
				BT_ConnectionState connectionState = BT_ConnectionState.getStateByAction(action);
				device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				module.deviceStateChange(device, connectionState);
				break;
		}
	}
}
