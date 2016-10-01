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
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 
 * @author pedro.m
 */
public final class BalisesCorrector
{
  private static final String NOM         = "nom";
  private static final String LATITUDE    = "latitude";
  private static final String LONGITUDE   = "longitude";
  private static final String ALTITUDE    = "altitude";
  private static final String COMMENTAIRE = "commentaire";
  private static final String ACTIVE      = "active";
  private static final char   POINT       = '.';

  private final URL           url;
  protected final Properties  properties;

  /**
   * 
   * @param resource
   */
  public BalisesCorrector(final String url)
  {
    try
    {
      properties = new Properties();
      this.url = new URL(url);
    }
    catch (final IOException ioe)
    {
      throw new RuntimeException(ioe);
    }
  }

  /**
   * 
   * @throws IOException
   */
  public boolean checkForCorrectorUpdate() throws IOException
  {
    // Initialisations
    boolean updated = false;
    InputStream input = null;

    try
    {
      // Ouveture du flux
      final URLConnection cnx = url.openConnection();
      cnx.setConnectTimeout(Utils.CONNECT_TIMEOUT);
      cnx.setReadTimeout(Utils.READ_TIMEOUT);
      input = cnx.getInputStream();

      // Chargement des properties
      properties.clear();
      properties.load(input);
      updated = true;
    }
    finally
    {
      if (input != null)
      {
        input.close();
      }
    }

    return updated;
  }

  /**
   * 
   * @param provider
   * @return
   */
  public List<String> correct(final BaliseProvider provider)
  {
    // Initialisations
    final List<String> balisesInconnues = new ArrayList<String>();

    // Pour chaque ligne
    for (final Object object : properties.keySet())
    {
      final String key = (String)object;
      final int point = key.indexOf(POINT);
      if (point > 0)
      {
        final String id = key.substring(0, point);
        final Balise balise = provider.getBaliseById(id);

        if (balise == null)
        {
          balisesInconnues.add(id);
        }
        else
        {
          final String property = key.substring(point + 1);
          final String value = properties.getProperty(key);

          if (NOM.equalsIgnoreCase(property))
          {
            balise.nom = value;
          }
          else if (LATITUDE.equalsIgnoreCase(property))
          {
            balise.latitude = Utils.parsePrimitiveDouble(value);
          }
          else if (LONGITUDE.equalsIgnoreCase(property))
          {
            balise.longitude = Utils.parsePrimitiveDouble(value);
          }
          else if (ALTITUDE.equalsIgnoreCase(property))
          {
            balise.altitude = Utils.parsePrimitiveInteger(value);
          }
          else if (COMMENTAIRE.equalsIgnoreCase(property))
          {
            balise.commentaire = value;
          }
          else if (ACTIVE.equalsIgnoreCase(property))
          {
            balise.active = Utils.parsePrimitiveBoolean(value);
          }
        }
      }
    }

    return balisesInconnues;
  }
}
