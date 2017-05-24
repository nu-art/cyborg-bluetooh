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
package com.nu.art.cyborg.bluetooth.constants;

import android.bluetooth.BluetoothAdapter;

public enum BT_AdapterState {
	NotAvailable("Device does not support Bluetooth", 0),
	TurningOff("Turning Bluetooth Off", BluetoothAdapter.STATE_TURNING_OFF),
	Off("Bluetooth Off", BluetoothAdapter.STATE_OFF),
	TurningOn("Turning Bluetooth On", BluetoothAdapter.STATE_TURNING_ON),
	On("Bluetooth On", BluetoothAdapter.STATE_ON),
	Inquiring("Inquiry Started", 101),
	CancelInquiry("Inquiry Canceled", 103),
	Advertising("Advertising Started", BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE),
	CancelAdvertising("Advertising Canceled", BluetoothAdapter.SCAN_MODE_NONE),
	InquiringEnded("Inquiry Ended", 150),;

	public static BT_AdapterState getInstanceForState(int newState) {
		BT_AdapterState[] states = BT_AdapterState.values();
		for (BT_AdapterState btState : states)
			if (btState.stateValue == newState)
				return btState;
		throw new EnumConstantNotPresentException(BT_AdapterState.class, "For state value=" + newState);
	}

	private final int stateValue;

	private String stateLabel;

	BT_AdapterState(String stateLabel, int stateValue) {
		this.stateValue = stateValue;
		this.stateLabel = stateLabel;
	}

	public String getStateLabel() {
		return stateLabel;
	}
}
