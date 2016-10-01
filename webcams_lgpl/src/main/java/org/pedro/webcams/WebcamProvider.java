/*******************************************************************************
 * WebcamsLib is Copyright 2014 by Pedro M.
 * 
 * This file is part of WebcamsLib.
 *
 * WebcamsLib is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * WebcamsLib is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * Commercial Distribution License
 * If you would like to distribute WebcamsLib (or portions thereof) under a
 * license other than the "GNU Lesser General Public License, version 3", please
 * contact Pedro M (pedro.pub@free.fr).
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with WebcamsLib. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.pedro.webcams;

import java.io.IOException;
import java.util.Collection;

/**
 * 
 * @author pedro.m
 */
public interface WebcamProvider<WebcamType extends Webcam>
{
  /**
   * 
   * @return
   */
  public boolean isAvailable();

  /**
   * 
   * @return
   */
  public Webcam newWebcam();

  /**
   * 
   * @param lastUpdate
   * @return
   * @throws IOException
   */
  public Collection<WebcamType> getWebcams(final long lastUpdate) throws IOException;
}
