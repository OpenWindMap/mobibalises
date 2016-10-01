/*******************************************************************************
 * SaveableLib is Copyright 2012 by Pedro M.
 * 
 * This file is part of SaveableLib.
 *
 * SaveableLib is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * SaveableLib is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * Commercial Distribution License
 * If you would like to distribute SaveableLib (or portions thereof) under a
 * license other than the "GNU Lesser General Public License, version 3", please
 * contact Pedro M (pedro.pub@free.fr).
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with SaveableLib. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.pedro.saveable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;

/**
 * 
 * @author pedro.m
 */
public abstract class SaveableUtils
{
  private static final String STRING_VALUE_NULL        = "nuUull";

  private static final byte   BOOLEAN_VALUE_NULL       = 2;
  private static final byte   BOOLEAN_VALUE_TRUE       = 1;
  private static final byte   BOOLEAN_VALUE_FALSE      = 0;

  private static final String MESSAGE_WRONG_SERIAL_UID = "Wrong serial UID";

  /**
   * 
   * @param out
   * @param saveable
   * @throws IOException
   */
  public static void saveSerialUID(final DataOutputStream out, final Saveable saveable) throws IOException
  {
    out.writeLong(saveable.getSerialUID());
  }

  /**
   * 
   * @param out
   * @param saveable
   * @throws IOException
   */
  public static void checkSerialUID(final DataInputStream in, final Saveable saveable) throws IOException
  {
    final long serialUID = in.readLong();
    if (serialUID != saveable.getSerialUID())
    {
      throw new IOException(MESSAGE_WRONG_SERIAL_UID);
    }
  }

  /**
   * 
   * @param out
   * @param value
   * @throws IOException
   */
  public static void writeString(final DataOutputStream out, final String value) throws IOException
  {
    out.writeUTF(value == null ? STRING_VALUE_NULL : value);
  }

  /**
   * 
   * @param out
   * @param value
   * @throws IOException
   */
  public static void writeDate(final DataOutputStream out, final Date value) throws IOException
  {
    out.writeLong(value == null ? Long.MIN_VALUE : value.getTime());
  }

  /**
   * 
   * @param out
   * @param value
   * @throws IOException
   */
  public static void writeDouble(final DataOutputStream out, final Double value) throws IOException
  {
    out.writeDouble(value == null ? Double.NaN : value.doubleValue());
  }

  /**
   * 
   * @param out
   * @param value
   * @throws IOException
   */
  public static void writeLong(final DataOutputStream out, final Long value) throws IOException
  {
    out.writeLong(value == null ? Long.MIN_VALUE : value.longValue());
  }

  /**
   * 
   * @param out
   * @param value
   * @throws IOException
   */
  public static void writeInteger(final DataOutputStream out, final Integer value) throws IOException
  {
    out.writeInt(value == null ? Integer.MIN_VALUE : value.intValue());
  }

  /**
   * 
   * @param out
   * @param value
   * @throws IOException
   */
  public static void writeBoolean(final DataOutputStream out, final Boolean value) throws IOException
  {
    out.writeByte(value == null ? BOOLEAN_VALUE_NULL : (value.booleanValue() ? BOOLEAN_VALUE_TRUE : BOOLEAN_VALUE_FALSE));
  }

  /**
   * 
   * @param in
   * @return
   * @throws IOException
   */
  public static String readString(final DataInputStream in) throws IOException
  {
    final String read = in.readUTF();
    return (STRING_VALUE_NULL.equals(read) ? null : read);
  }

  /**
   * 
   * @param in
   * @return
   * @throws IOException
   */
  public static Date readDate(final DataInputStream in) throws IOException
  {
    final long read = in.readLong();
    return (read == Long.MIN_VALUE ? null : new Date(read));
  }

  /**
   * 
   * @param in
   * @return
   * @throws IOException
   */
  public static Double readDouble(final DataInputStream in) throws IOException
  {
    final double read = in.readDouble();
    return (Double.isNaN(read) ? null : Double.valueOf(read));
  }

  /**
   * 
   * @param in
   * @return
   * @throws IOException
   */
  public static Long readLong(final DataInputStream in) throws IOException
  {
    final long read = in.readLong();
    return (read == Long.MIN_VALUE ? null : Long.valueOf(read));
  }

  /**
   * 
   * @param in
   * @return
   * @throws IOException
   */
  public static Integer readInteger(final DataInputStream in) throws IOException
  {
    final int read = in.readInt();
    return (read == Integer.MIN_VALUE ? null : Integer.valueOf(read));
  }

  /**
   * 
   * @param in
   * @return
   * @throws IOException
   */
  public static Boolean readBoolean(final DataInputStream in) throws IOException
  {
    final byte read = in.readByte();
    return (read == BOOLEAN_VALUE_NULL ? null : read == BOOLEAN_VALUE_TRUE ? Boolean.TRUE : Boolean.FALSE);
  }
}
