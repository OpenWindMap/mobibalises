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
import java.util.List;

/**
 * 
 * @author pedro.m
 */
public class CachedProvider implements BaliseProvider
{
  private static final String          BALISES_KEY_SUFFIX = ".balises";
  private static final String          RELEVES_KEY_SUFFIX = ".releves";

  private final AbstractBaliseProvider provider;
  private final BalisesCorrector       corrector;
  private final BaliseCache            cache;

  private final String                 balisesKey;
  private final String                 relevesKey;

  private boolean                      available          = true;

  /**
   * 
   * @param key
   * @param provider
   * @param corrector
   * @param cache
   */
  public CachedProvider(final String key, final AbstractBaliseProvider provider, final BalisesCorrector corrector, final BaliseCache cache)
  {
    this.provider = provider;
    this.corrector = corrector;
    this.cache = cache;

    balisesKey = key.concat(BALISES_KEY_SUFFIX);
    relevesKey = key.concat(RELEVES_KEY_SUFFIX);
  }

  /**
   * 
   * @return
   * @throws IOException
   */
  public List<String> correct() throws IOException
  {
    if (corrector == null)
    {
      return null;
    }

    corrector.checkForCorrectorUpdate();
    return corrector.correct(provider);
  }

  /**
   * 
   * @throws IOException
   */
  public void storeBalises() throws IOException
  {
    cache.storeBalises(balisesKey, provider.getBalisesMap());
  }

  /**
   * 
   * @throws IOException
   */
  public void restoreBalises() throws IOException
  {
    provider.setBalisesMap(cache.restoreBalises(balisesKey, this));
  }

  /**
   * 
   * @throws IOException
   */
  public void storeReleves() throws IOException
  {
    cache.storeReleves(relevesKey, provider.getRelevesMap());
  }

  /**
   * 
   * @throws IOException
   */
  public void restoreReleves() throws IOException
  {
    provider.setRelevesMap(cache.restoreReleves(relevesKey, this));
  }

  @Override
  public Balise getBaliseById(final String id)
  {
    return provider.getBaliseById(id);
  }

  @Override
  public String getBaliseDetailUrl(final String id)
  {
    return provider.getBaliseDetailUrl(id);
  }

  @Override
  public String getBaliseHistoriqueUrl(final String id)
  {
    return provider.getBaliseHistoriqueUrl(id);
  }

  @Override
  public Collection<Balise> getBalises()
  {
    return provider.getBalises();
  }

  @Override
  public long getBalisesUpdateDate() throws IOException
  {
    return provider.getBalisesUpdateDate();
  }

  @Override
  public Releve getReleveById(final String id)
  {
    return provider.getReleveById(id);
  }

  @Override
  public Collection<Releve> getReleves()
  {
    return provider.getReleves();
  }

  @Override
  public long getRelevesUpdateDate() throws IOException
  {
    return provider.getRelevesUpdateDate();
  }

  @Override
  public boolean isAvailable()
  {
    return available;
  }

  @Override
  public boolean updateBalises() throws IOException
  {
    try
    {
      final boolean retour = provider.updateBalises();
      available = true;
      return retour;
    }
    catch (final IOException ioe)
    {
      available = false;
      throw ioe;
    }
  }

  @Override
  public boolean updateBalisesUpdateDate() throws IOException
  {
    try
    {
      final boolean retour = provider.updateBalisesUpdateDate();
      available = true;
      return retour;
    }
    catch (final IOException ioe)
    {
      available = false;
      throw ioe;
    }
  }

  @Override
  public boolean updateReleves() throws IOException
  {
    try
    {
      final boolean retour = provider.updateReleves();
      available = true;
      return retour;
    }
    catch (final IOException ioe)
    {
      available = false;
      throw ioe;
    }
  }

  @Override
  public boolean updateRelevesUpdateDate() throws IOException
  {
    try
    {
      final boolean retour = provider.updateRelevesUpdateDate();
      available = true;
      return retour;
    }
    catch (final IOException ioe)
    {
      available = false;
      throw ioe;
    }
  }

  /**
   * @return the balisesKey
   */
  public String getBalisesCacheKey()
  {
    return balisesKey;
  }

  /**
   * @return the relevesKey
   */
  public String getRelevesCacheKey()
  {
    return relevesKey;
  }

  @Override
  public Balise newBalise()
  {
    return provider.newBalise();
  }

  @Override
  public Releve newReleve()
  {
    return provider.newReleve();
  }

  @Override
  public String filterExceptionMessage(final String message)
  {
    return provider.filterExceptionMessage(message);
  }

  @Override
  public String getCountry()
  {
    return provider.getCountry();
  }

  @Override
  public boolean isMultiCountries()
  {
    return provider.isMultiCountries();
  }

  @Override
  public String getName()
  {
    return provider.getName();
  }

  @Override
  public Collection<Releve> getUpdatedReleves()
  {
    return provider.getUpdatedReleves();
  }

  @Override
  public Class<? extends BaliseProvider> getBaliseProviderClass()
  {
    return provider.getBaliseProviderClass();
  }

  /**
   * 
   * @return
   */
  public BaliseProvider getOriginalBaliseProvider()
  {
    return provider;
  }

  @Override
  public int getDefaultDeltaReleves()
  {
    return provider.getDefaultDeltaReleves();
  }
}
