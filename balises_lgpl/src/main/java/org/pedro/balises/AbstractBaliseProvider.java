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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author pedro.m
 */
public abstract class AbstractBaliseProvider implements BaliseProvider, ReleveParserListener
{
  protected static final String       REQUEST_PROPERTY_ACCEPT_ENCODING          = "Accept-Encoding";
  protected static final String       REQUEST_PROPERTY_ACCEPT_ENCODING_IDENTITY = "identity";

  private final String                name;
  protected final String              country;
  protected final String              region;
  private Map<String, Balise>         balises;
  private Map<String, Releve>         releves;
  protected final Map<String, Releve> updatedReleves                            = new HashMap<String, Releve>();

  /**
   * 
   * @param name
   * @param country
   * @param region
   * @param balisesSize
   */
  public AbstractBaliseProvider(final String name, final String country, final String region, final int balisesSize)
  {
    this.name = name;
    this.country = country.toUpperCase();
    this.region = (region == null ? null : region.toUpperCase());
    balises = new HashMap<String, Balise>(balisesSize);
    releves = new HashMap<String, Releve>(balisesSize);
  }

  @Override
  public final Collection<Balise> getBalises()
  {
    return balises.values();
  }

  @Override
  public final Balise getBaliseById(final String id)
  {
    return balises.get(id);
  }

  /**
   * @return the balises
   */
  protected final Map<String, Balise> getBalisesMap()
  {
    return balises;
  }

  /**
   * @param balises the balises to set
   */
  protected final void setBalisesMap(final Map<String, Balise> balises)
  {
    this.balises = balises;
  }

  @Override
  public final Collection<Releve> getReleves()
  {
    return releves.values();
  }

  @Override
  public final Releve getReleveById(final String id)
  {
    return releves.get(id);
  }

  /**
   * @return the releves
   */
  protected final Map<String, Releve> getRelevesMap()
  {
    return releves;
  }

  /**
   * @param releves the releves to set
   */
  protected final void setRelevesMap(final Map<String, Releve> releves)
  {
    this.releves = releves;
  }

  @Override
  public void onReleveParsed(final Releve nouveau)
  {
    // Recherche dans les anciens
    final Releve ancien = releves.get(nouveau.id);
    final boolean updated;

    // Nouveau releve
    if (ancien == null)
    {
      updated = true;
    }
    // Ancien releve existant, comparaison de la date et calcul des tendances
    else
    {
      // Comparaison date
      final long newStamp = (nouveau.date == null ? -1 : nouveau.date.getTime());
      final long oldStamp = (ancien.date == null ? -1 : ancien.date.getTime());
      updated = (newStamp > oldStamp);

      // Calcul des tendances
      if (updated && (nouveau.date != null) && (ancien.date != null))
      {
        // Date releve precedent fournie par le serveur
        final long prevStamp = (nouveau.dateRelevePrecedent == null ? -1 : nouveau.dateRelevePrecedent.getTime());

        // Si les anciennes donnees locales sont plus fraiches que les anciennes donnees du serveur => calcul des tendances
        // Sinon on ne touche pas aux tendances fournies par le serveur
        if (oldStamp > prevStamp)
        {
          // Anciennete
          nouveau.dateRelevePrecedent = ancien.date;

          // Vent mini
          if (!Double.isNaN(nouveau.ventMini) && !Double.isNaN(ancien.ventMini))
          {
            nouveau.ventMiniTendance = nouveau.ventMini - ancien.ventMini;
          }

          // Vent moyen
          if (!Double.isNaN(nouveau.ventMoyen) && !Double.isNaN(ancien.ventMoyen))
          {
            nouveau.ventMoyenTendance = nouveau.ventMoyen - ancien.ventMoyen;
          }

          // Vent maxi
          if (!Double.isNaN(nouveau.ventMaxi) && !Double.isNaN(ancien.ventMaxi))
          {
            nouveau.ventMaxiTendance = nouveau.ventMaxi - ancien.ventMaxi;
          }
        }
      }
    }

    // Sauvegarde
    if (updated)
    {
      // Allocation si besoin (le parser ne travaille que sur une seule instance de Releve !),
      // si non on reprend l'ancienne instance
      final Releve toSave = (ancien == null) ? newReleve() : ancien;

      // Copie des valeurs
      toSave.copyFrom(nouveau);

      // Sauvegarde
      updatedReleves.put(toSave.id, toSave);
      if (ancien == null)
      {
        // Dans la table des releves (necessaire seulement si l'ancien releve n'existait pas)
        releves.put(toSave.id, toSave);
      }
    }
  }

  @Override
  public String filterExceptionMessage(final String message)
  {
    return message;
  }

  @Override
  public String getCountry()
  {
    return country;
  }

  @Override
  public String getName()
  {
    return name;
  }

  @Override
  public Collection<Releve> getUpdatedReleves()
  {
    return updatedReleves.values();
  }

  @Override
  public Class<? extends BaliseProvider> getBaliseProviderClass()
  {
    return getClass();
  }
}
