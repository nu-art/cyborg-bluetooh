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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.nu.art.core.tools.StreamTools;

public abstract class BluetoothPacket {

	public final void transmitPacket(OutputStream os)
			throws IOException {
		writePacketData(os);
	}

	protected abstract void writePacketData(OutputStream os)
			throws IOException;

	protected abstract void readPacketData(InputStream is)
			throws IOException;

	protected final long readByteArrayValue(InputStream is, int length)
			throws IOException {
		if (length > 8)
			throw new IllegalArgumentException("Length must be of sizes: 1, 2, 4, 8");
		byte[] bytes = new byte[length];
		readBuffer(is, bytes);
		return StreamTools.fromByteArray(bytes);
	}

	protected final int readBuffer(InputStream is, byte[] buffer)
			throws IOException {
		int readIndex = 0;
		while (readIndex != buffer.length)
			buffer[readIndex++] = (byte) is.read();
		return readIndex;
	}
}
