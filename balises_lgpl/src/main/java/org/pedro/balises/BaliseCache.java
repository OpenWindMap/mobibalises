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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * 
 * @author pedro.m
 */
public interface BaliseCache
{
  /**
   * 
   * @param key
   * @return
   * @throws IOException
   */
  public OutputStream getCacheOutputStream(String key) throws IOException;

  /**
   * 
   * @param key
   * @return
   * @throws IOException
   */
  public InputStream getCacheInputStream(String key) throws IOException;

  /**
   * 
   * @param key
   * @return
   * @throws IOException
   */
  public boolean clearCache(String key) throws IOException;

  /**
   * 
   * @param key
   * @return
   */
  public long getCacheTimestamp(String key);

  /**
   * 
   * @param key
   * @param stamp
   */
  public void setCacheTimestamp(String key, long stamp);

  /**
   * 
   * @param key
   * @param balises
   * @throws IOException
   */
  public void storeBalises(String key, Map<String, Balise> balises) throws IOException;

  /**
   * 
   * @param key
   * @param provider
   * @return
   * @throws IOException
   */
  public Map<String, Balise> restoreBalises(String key, CachedProvider provider) throws IOException;

  /**
   * 
   * @param key
   * @param releves
   * @throws IOException
   */
  public void storeReleves(String key, Map<String, Releve> releves) throws IOException;

  /**
   * 
   * @param key
   * @param provider
   * @return
   * @throws IOException
   */
  public Map<String, Releve> restoreReleves(String key, CachedProvider provider) throws IOException;

  /**
   * 
   */
  public void onShutdown();
}
