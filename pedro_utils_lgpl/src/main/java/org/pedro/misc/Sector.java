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
package org.pedro.misc;

import java.util.List;

/**
 * 
 * @author pedro.m
 */
public enum Sector
{
  N_NNE, NNE_NE, NE_ENE, ENE_E, E_ESE, ESE_SE, SE_SSE, SSE_S, S_SSO, SSO_SO, SO_OSO, OSO_O, O_ONO, ONO_NO, NO_NNO, NNO_N;

  private static int  nbSectors = Sector.values().length;
  public static float amplitude = ((float)360) / nbSectors;

  /**
   * 
   * @return
   */
  public float getStartAngle()
  {
    return ordinal() * amplitude;
  }

  /**
   * 
   * @return
   */
  public float getEndAngle()
  {
    return (ordinal() + 1) * amplitude;
  }

  /**
   *  
   * @return
   */
  private String toAngleString()
  {
    return (getStartAngle() - 90) + "/" + (getEndAngle() - getStartAngle());
  }

  /**
   * 
   * @param sectors
   * @return
   */
  @SuppressWarnings("unused")
  private static String toAngleString(final List<Sector> sectors)
  {
    String retour = "[";

    boolean first = true;
    for (final Sector sector : sectors)
    {
      if (!first)
      {
        retour += ", ";
      }
      retour += sector.toAngleString();
      // Next
      first = false;
    }

    retour += "]";
    return retour;
  }

  /**
   * 
   * @param angle
   * @return
   */
  public static Sector getFromAngle(final float angle)
  {
    for (final Sector sector : Sector.values())
    {
      if ((angle >= sector.getStartAngle()) && (angle < sector.getEndAngle()))
      {
        return sector;
      }
    }

    return null;
  }
}
