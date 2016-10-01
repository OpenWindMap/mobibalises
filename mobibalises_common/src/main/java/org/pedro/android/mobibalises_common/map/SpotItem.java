package org.pedro.android.mobibalises_common.map;

import org.pedro.android.mobibalises_common.Strings;
import org.pedro.map.GeoPoint;
import org.pedro.map.MapDrawable;
import org.pedro.map.OverlayItem;
import org.pedro.spots.Spot;
import org.pedro.spots.dhv.DhvSpot;
import org.pedro.spots.ffvl.FfvlSpot;

import android.graphics.Canvas;

/**
 * 
 * @author pedro.m
 */
public final class SpotItem extends OverlayItem<Canvas>
{
  protected final String   providerKey;
  protected final String   spotId;
  protected final String   spotName;
  protected final boolean  spotHasInfos;
  protected final Object[] spotOtherInfos;

  /**
   * 
   * @param providerKey
   * @param spot
   * @param point
   * @param drawable
   */
  public SpotItem(final String providerKey, final Spot spot, final GeoPoint point, final MapDrawable<Canvas> drawable)
  {
    super(new StringBuilder().append(providerKey).append(Strings.CHAR_POINT).append(spot.id).toString(), point, drawable);
    this.providerKey = providerKey;
    this.spotId = spot.id;
    this.spotName = spot.nom;
    this.spotHasInfos = ((spot.infos != null) && (spot.infos.size() > 0));
    if (FfvlSpot.class.isAssignableFrom(spot.getClass()))
    {
      spotOtherInfos = new Object[] { ((FfvlSpot)spot).idStructure };
    }
    else if (DhvSpot.class.isAssignableFrom(spot.getClass()))
    {
      spotOtherInfos = new Object[] { ((DhvSpot)spot).idSite };
    }
    else
    {
      spotOtherInfos = null;
    }
  }

  @Override
  public String toString()
  {
    return providerKey + "." + spotId + "@" + getPoint();
  }
}
