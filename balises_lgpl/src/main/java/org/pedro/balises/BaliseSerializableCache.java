/*******************************************************************************
 * BalisesLib is Copyright 2012 by Pedro M.
 * 
 * This file is part of BalisesLib.
 *
 * BalisesLib is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * BalisesLib is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * Commercial Distribution License
 * If you would like to distribute BalisesLib (or portions thereof) under a
 * license other than the "GNU Lesser General Public License, version 3", please
 * contact Pedro M (pedro.pub@free.fr).
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with BalisesLib. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.pedro.balises;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * 
 * @author pedro.m
 */
public abstract class BaliseSerializableCache implements BaliseCache
{
  @Override
  @SuppressWarnings("unchecked")
  public Map<String, Balise> restoreBalises(final String key, final CachedProvider provider) throws IOException
  {
    return (Map<String, Balise>)deserializeObject(getCacheInputStream(key));
  }

  @Override
  public void storeBalises(final String key, final Map<String, Balise> balises) throws IOException
  {
    serializeObject(getCacheOutputStream(key), balises);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Map<String, Releve> restoreReleves(final String key, final CachedProvider provider) throws IOException
  {
    return (Map<String, Releve>)deserializeObject(getCacheInputStream(key));
  }

  @Override
  public void storeReleves(final String key, final Map<String, Releve> releves) throws IOException
  {
    serializeObject(getCacheOutputStream(key), releves);
  }

  /**
   * 
   * @param os
   * @param object
   * @throws IOException
   */
  protected static void serializeObject(final OutputStream os, final Object object) throws IOException
  {
    // Initialisations
    BufferedOutputStream bos = null;
    ObjectOutputStream oos = null;

    try
    {
      bos = new BufferedOutputStream(os, 16000);
      oos = new ObjectOutputStream(bos);
      oos.writeObject(object);
    }
    finally
    {
      if (oos != null)
      {
        oos.close();
      }
      if (bos != null)
      {
        bos.close();
      }
      if (os != null)
      {
        os.close();
      }
    }
  }

  /**
   * 
   * @param is
   * @return
   * @throws IOException
   */
  protected static Object deserializeObject(final InputStream is) throws IOException
  {
    // Initialisations
    BufferedInputStream bis = null;
    ObjectInputStream ois = null;

    try
    {
      bis = new BufferedInputStream(is, 16000);
      ois = new ObjectInputStream(bis);

      return ois.readObject();
    }
    catch (final ClassNotFoundException cnfe)
    {
      final IOException ioe = new IOException(cnfe.getMessage());
      ioe.setStackTrace(cnfe.getStackTrace());
      throw ioe;
    }
    finally
    {
      if (ois != null)
      {
        ois.close();
      }
      if (bis != null)
      {
        bis.close();
      }
      if (is != null)
      {
        is.close();
      }
    }
  }
}
