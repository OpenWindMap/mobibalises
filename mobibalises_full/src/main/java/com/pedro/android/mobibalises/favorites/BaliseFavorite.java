package com.pedro.android.mobibalises.favorites;

import java.lang.ref.WeakReference;

import org.pedro.android.mobibalises_common.Strings;
import org.pedro.balises.Balise;
import org.pedro.balises.Releve;
import org.pedro.balises.Utils;
import org.pedro.map.GeoPoint;

/**
 * 
 * @author pedro.m
 */
public class BaliseFavorite extends Favorite implements Comparable<BaliseFavorite>
{
  private final WeakReference<FavoritesService> favoritesService;
  private final String                          providerId;
  private final String                          baliseId;
  private final Double                          distance;
  private final Double                          bearing;
  private final int                             hashcode;

  /**
   * 
   * @param chaine
   * @return
   */
  public static BaliseFavorite parseBalise(final String chaine, final FavoritesService inFavoritesService)
  {
    if (Utils.isStringVide(chaine))
    {
      return null;
    }

    final int posPoint = chaine.indexOf(Strings.CHAR_POINT);
    if (posPoint < 0)
    {
      return null;
    }

    // Favori
    return new BaliseFavorite(chaine.substring(0, posPoint), chaine.substring(posPoint + 1), inFavoritesService);
  }

  /**
   * 
   * @param providerId
   * @param baliseId
   * @param favoritesService
   */
  public BaliseFavorite(final String providerId, final String baliseId, final FavoritesService favoritesService)
  {
    this(providerId, baliseId, null, null, favoritesService);
  }

  /**
   * 
   * @param providerId
   * @param baliseId
   * @param distance
   * @param bearing
   * @param favoritesService
   */
  public BaliseFavorite(final String providerId, final String baliseId, final Double distance, final Double bearing, final FavoritesService favoritesService)
  {
    super(new StringBuilder().append(providerId).append(Strings.CHAR_POINT).append(baliseId).toString());
    this.providerId = providerId;
    this.baliseId = baliseId;
    this.distance = distance;
    this.bearing = bearing;
    this.favoritesService = new WeakReference<FavoritesService>(favoritesService);

    this.hashcode = getId().hashCode();
  }

  @Override
  public String toString()
  {
    return getId();
  }

  @Override
  public int hashCode()
  {
    return hashcode;
  }

  @Override
  public boolean equals(final Object object)
  {
    if (object == null)
    {
      return false;
    }

    if (!BaliseFavorite.class.isAssignableFrom(object.getClass()))
    {
      return false;
    }

    final BaliseFavorite favorite = (BaliseFavorite)object;

    return favorite.getId().equals(getId());
  }

  @Override
  public int compareTo(final BaliseFavorite other)
  {
    if (other == null)
    {
      return -1;
    }

    if ((distance == null) && (other.distance == null))
    {
      return 0;
    }

    if (distance == null)
    {
      return -1;
    }

    if (other.distance == null)
    {
      return 1;
    }

    if (distance.doubleValue() < other.distance.doubleValue())
    {
      return -1;
    }
    else if (distance.doubleValue() > other.distance.doubleValue())
    {
      return 1;
    }

    return 0;
  }

  @Override
  public GeoPoint getCenter()
  {
    final Balise balise = favoritesService.get().getBalise(providerId, baliseId);

    if (balise == null)
    {
      return null;
    }

    if (Double.isNaN(balise.latitude) || Double.isNaN(balise.longitude))
    {
      return null;
    }

    return new GeoPoint(balise.latitude, balise.longitude);
  }

  @Override
  public String getName()
  {
    final Balise balise = favoritesService.get().getBalise(providerId, baliseId);

    return (balise == null ? null : balise.nom);
  }

  /**
   * 
   * @return
   */
  public boolean isDrawable()
  {
    final Balise balise = favoritesService.get().getBalise(providerId, baliseId);
    final Releve releve = favoritesService.get().getReleve(providerId, baliseId);

    final boolean baliseDrawable = (balise != null) && !Utils.isBooleanNull(balise.active) && Utils.getBooleanValue(balise.active);

    return baliseDrawable && (releve != null) && (releve.date != null);
  }

  /**
   * 
   * @return
   */
  public String getProviderId()
  {
    return providerId;
  }

  /**
   * 
   * @return
   */
  public String getBaliseId()
  {
    return baliseId;
  }

  /**
   * @return the distance
   */
  public Double getDistance()
  {
    return distance;
  }

  /**
   * 
   * @return
   */
  public Double getBearing()
  {
    return bearing;
  }
}
