/*******************************************************************************
 * Copyright 2013 Geoscience Australia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package au.gov.ga.earthsci.common.buffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import au.gov.ga.earthsci.common.util.Validate;

/**
 * Utility methods for working with buffers
 * 
 * @author James Navin (james.navin@ga.gov.au)
 */
/* 
 * TODO: Could these be made more usable by turning them into something like the BufferWrapper that gives
 * access to the correct type from an underlying byte buffer (similar to the typed views in java.nio
 * but with support for all types listed in BufferType)
 */
public class BufferUtil
{

	/**
	 * Return the next value from the buffer of the provided type.
	 * <p/>
	 * Types will be returned that match the container type given by
	 * {@link BufferType#getContainerClass()}
	 * 
	 * @return The next value from the provided buffer of the provided type
	 */
	public static Number getValue(ByteBuffer buffer, BufferType bufferType)
	{
		if (buffer == null)
		{
			return null;
		}

		Validate.notNull(bufferType, "A valid buffer type is required"); //$NON-NLS-1$

		switch (bufferType)
		{
		case BYTE:
			return buffer.get() & 0xff;
		case UNSIGNED_SHORT:
			return getUInt16(buffer);
		case SHORT:
			return buffer.getShort();
		case UNSIGNED_INT:
			return getUInt32(buffer);
		case INT:
			return buffer.getInt();
		case LONG:
			return buffer.getLong();
		case FLOAT:
			return buffer.getFloat();
		case DOUBLE:
			return buffer.getDouble();
		}

		throw new UnsupportedOperationException("Unsupported buffer type " + bufferType.name()); //$NON-NLS-1$
	}

	/**
	 * Skip forward in the buffer the given number of values of the given type
	 * <p/>
	 * This is generally more efficient than using
	 * {@link #getValue(ByteBuffer, BufferType)} repeatedly and discarding the
	 * value when the number of values to skip is known ahead of time.
	 * 
	 * @param num
	 *            The number of values to skip. A negative value will move
	 *            backwards through the buffer.
	 * @param buffer
	 *            The buffer to skip
	 * @param bufferType
	 *            The type of value contained in the buffer
	 */
	public static void skipValues(int num, ByteBuffer buffer, BufferType bufferType)
	{
		if (buffer == null || num == 0)
		{
			return;
		}

		Validate.notNull(bufferType, "A valid buffer type is required"); //$NON-NLS-1$
		buffer.position(buffer.position() + num * bufferType.getNumberOfBytes());
	}

	/**
	 * Convert the given number into the correct type for use with the given
	 * buffer type.
	 * <p/>
	 * Note that this may lose information (through rounding or truncation) if
	 * not used correctly.
	 * 
	 * @param n
	 *            The number to convert
	 * @param targetType
	 *            The target buffer type to convert the number to
	 */
	public static Number convertTo(Number n, BufferType targetType)
	{
		if (n == null)
		{
			return n;
		}

		Validate.notNull(targetType, "A valid buffer type is required"); //$NON-NLS-1$

		switch (targetType)
		{
		case BYTE:
			return n.byteValue();
		case UNSIGNED_SHORT:
			return n.intValue();
		case SHORT:
			return n.shortValue();
		case UNSIGNED_INT:
			return n.longValue();
		case INT:
			return n.intValue();
		case LONG:
			return n.longValue();
		case FLOAT:
			return n.floatValue();
		case DOUBLE:
			return n.doubleValue();
		}

		throw new UnsupportedOperationException("Unsupported buffer type " + targetType.name()); //$NON-NLS-1$
	}

	private static int getUInt16(ByteBuffer buffer)
	{
		int first = 0xff & buffer.get();
		int second = 0xff & buffer.get();
		if (buffer.order() == ByteOrder.LITTLE_ENDIAN)
		{
			return (first << 8 | second);
		}
		else
		{
			return (first | second << 8);
		}
	}

	private static long getUInt32(ByteBuffer buffer)
	{
		long first = 0xff & buffer.get();
		long second = 0xff & buffer.get();
		long third = 0xff & buffer.get();
		long fourth = 0xff & buffer.get();
		if (buffer.order() == ByteOrder.LITTLE_ENDIAN)
		{
			return (first << 24l | second << 16l | third << 8l | fourth);
		}
		else
		{
			return (first | second << 8l | third << 16l | fourth << 24l);
		}
	}

}
