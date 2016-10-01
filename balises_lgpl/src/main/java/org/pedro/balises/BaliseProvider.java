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
import java.util.Collection;

/**
 * 
 * @author pedro.m
 */
public interface BaliseProvider
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
  public Balise newBalise();

  /**
   * 
   * @return
   * @throws IOException
   */
  public boolean updateBalisesUpdateDate() throws IOException;

  /**
   * 
   * @return
   * @throws IOException
   */
  public long getBalisesUpdateDate() throws IOException;

  /**
   * 
   * @throws IOException
   */
  public boolean updateBalises() throws IOException;

  /**
   * 
   * @return
   */
  public Collection<Balise> getBalises();

  /**
   * 
   * @param id
   * @return
   */
  public Balise getBaliseById(String id);

  /**
   * 
   * @param id
   * @return
   */
  public String getBaliseDetailUrl(String id);

  /**
   * 
   * @param id
   * @return
   */
  public String getBaliseHistoriqueUrl(String id);

  /**
   * 
   * @return
   */
  public Releve newReleve();

  /**
   * 
   * @return
   * @throws IOException
   */
  public boolean updateRelevesUpdateDate() throws IOException;

  /**
   * 
   * @return
   * @throws IOException
   */
  public long getRelevesUpdateDate() throws IOException;

  /**
   * 
   * @throws IOException
   */
  public boolean updateReleves() throws IOException;

  /**
   * 
   * @return
   */
  public Collection<Releve> getReleves();

  /**
   * 
   * @param id
   * @return
   */
  public Releve getReleveById(String id);

  /**
   * 
   * @param message
   * @return
   */
  public String filterExceptionMessage(String message);

  /**
   * 
   * @return
   */
  public String getCountry();

  /**
   * 
   * @return
   */
  public boolean isMultiCountries();

  /**
   * 
   * @return
   */
  public String getName();

  /**
   * 
   * @return
   */
  public Collection<Releve> getUpdatedReleves();

  /**
   * 
   * @return
   */
  public Class<? extends BaliseProvider> getBaliseProviderClass();

  /**
   * Intervalle par defaut (en minutes) entre 2 releves
   * 
   * @return
   */
  public int getDefaultDeltaReleves();
}
