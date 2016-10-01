/*******************************************************************************
 * PedroUtilsLib is Copyright 2014 by Pedro M.
 * 
 * This file is part of PedroUtilsLib.
 *
 * PedroUtilsLib is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * PedroUtilsLib is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * Commercial Distribution License
 * If you would like to distribute PedroUtilsLib (or portions thereof) under a
 * license other than the "GNU Lesser General Public License, version 3", please
 * contact Pedro M (pedro.pub@free.fr).
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with PedroUtilsLib. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.pedro.saveable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 
 * @author pedro.m
 */
public interface Saveable
{
  /**
   * 
   * @return
   */
  public long getSerialUID();

  /**
   * 
   * @param in
   * @return
   * @throws IOException
   */
  public void loadSaveable(DataInputStream in) throws IOException;

  /**
   * 
   * @param out
   * @throws IOException
   */
  public void saveSaveable(DataOutputStream out) throws IOException;
}
