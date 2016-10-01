package org.pedro.android.mobibalises_common.map;

import org.pedro.android.mobibalises_common.Strings;
import org.pedro.map.GeoPoint;
import org.pedro.map.MapDrawable;
import org.pedro.map.OverlayItem;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Path;

/**
 * 
 * @author pedro.m
 */
public final class BaliseItem extends OverlayItem<Canvas>
{
  public final String providerKey;
  public final String baliseId;

  public boolean      collide              = false;
  public float        deltaAngle           = 0;
  public int          deltaX               = 0;
  public int          deltaY               = 0;
  public Path         collisionPath;
  public Matrix       collisionMatrix;
  public boolean      collisionDisplayable = true;

  /**
   * 
   * @param providerKey
   * @param baliseId
   * @param point
   * @param drawable
   */
  public BaliseItem(final String providerKey, final String baliseId, final GeoPoint point, final MapDrawable<Canvas> drawable)
  {
    super(new StringBuilder().append(providerKey).append(Strings.CHAR_POINT).append(baliseId).toString(), point, drawable);
    this.providerKey = providerKey;
    this.baliseId = baliseId;
  }

  @Override
  public String toString()
  {
    return providerKey + "." + baliseId + "@" + getPoint();
  }
}
