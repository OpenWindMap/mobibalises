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
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;

/**
 * 
 * @author pedro.m
 */
public abstract class AbstractMobibalisesWebcamProvider<WebcamType extends Webcam> implements WebcamProvider<WebcamType>
{
  private static final String STRING_UTF_8           = "UTF-8";

  private static final String URL_API_KEY_KEY        = "apiKey";
  private static final String URL_API_KEY_GROUP      = "\\{" + URL_API_KEY_KEY + "\\}";
  private static final String URL_DERNIERE_MAJ_KEY   = "derniereMaj";
  private static final String URL_DERNIERE_MAJ_GROUP = "\\{" + URL_DERNIERE_MAJ_KEY + "\\}";

  private static final String URL                    = "http://data.mobibalises.net/webcams/webcams-get.php?apiKey={" + URL_API_KEY_KEY + "}&sortie=json&statutsKo=2&derniereMaj={" + URL_DERNIERE_MAJ_KEY + "}";

  private final String        apiKey;

  /**
   * 
   * @param apiKey
   */
  public AbstractMobibalisesWebcamProvider(final String apiKey)
  {
    this.apiKey = apiKey;
  }

  @Override
  public boolean isAvailable()
  {
    boolean retour = false;

    try
    {
      final String finalUrl = URL.replaceAll(URL_DERNIERE_MAJ_GROUP, "-1").replaceAll(URL_API_KEY_GROUP, apiKey);
      final URL url = new URL(finalUrl);
      final URLConnection cnx = url.openConnection();
      if (cnx != null)
      {
        cnx.setConnectTimeout(Utils.CONNECT_TIMEOUT);
        cnx.setReadTimeout(Utils.READ_TIMEOUT);
        cnx.connect();
        retour = true;
      }
    }
    catch (final IOException ioe)
    {
      retour = false;
    }

    return retour;
  }

  @Override
  public Webcam newWebcam()
  {
    return new Webcam();
  }

  @Override
  public Collection<WebcamType> getWebcams(final long lastUpdate) throws IOException
  {
    final String finalUrl = URL.replaceAll(URL_DERNIERE_MAJ_GROUP, Long.toString(lastUpdate / 1000, 10)).replaceAll(URL_API_KEY_GROUP, apiKey);
    final URL url = new URL(finalUrl);
    final StringBuilder buffer = new StringBuilder(Utils.READ_BUFFER_SIZE + 10);
    Utils.readData(url, STRING_UTF_8, buffer);

    return parseMobibalisesResponse(buffer);
  }

  /**
   * 
   * @param buffer
   * @return
   * @throws IOException
   */
  public abstract Collection<WebcamType> parseMobibalisesResponse(final StringBuilder buffer) throws IOException;
}
