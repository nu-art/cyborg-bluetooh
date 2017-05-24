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


import android.bluetooth.BluetoothDevice;


public enum BT_ConnectionState {
	ACL_Connecting("ACL Connecting"),
	ACL_Connected(BluetoothDevice.ACTION_ACL_CONNECTED, "ACL Connected"),
	Bonding(BluetoothDevice.ACTION_BOND_STATE_CHANGED, "New Bonding State"),
	SPP_Connected("", "SPP Connected"),
	ACL_Disconnecting(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED, "ACL Disconnecting"),
	ACL_Disconnected(BluetoothDevice.ACTION_ACL_DISCONNECTED, "ACL Disconnected");
	
	private static final String NoAndroidConstant = "No Android Action";
	
	private String label;
	
	private String action;
	
	BT_ConnectionState(String label) {
		this(NoAndroidConstant, label);
	}
	
	BT_ConnectionState(String action, String label) {
		this.label = label;
		this.action = action;
	}
	
	public String getLabel() {
		return label;
	}
	
	public String getAction() {
		return action;
	}
	
	public static BT_ConnectionState getStateByAction(String action) {
		BT_ConnectionState[] states = values();
		for (BT_ConnectionState state : states)
			if (state.action.equals(action))
				return state;
		throw new EnumConstantNotPresentException(BT_ConnectionState.class, action);
	}
}
