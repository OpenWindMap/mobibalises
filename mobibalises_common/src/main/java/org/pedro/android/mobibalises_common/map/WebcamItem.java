package org.pedro.android.mobibalises_common.map;

import org.pedro.android.mobibalises_common.Strings;
import org.pedro.android.mobibalises_common.map.InfosDrawable.DrawableInfo;
import org.pedro.android.mobibalises_common.webcam.WebcamDatabaseHelper.WebcamRow;
import org.pedro.map.GeoPoint;
import org.pedro.map.MapDrawable;
import org.pedro.map.OverlayItem;

import android.graphics.Canvas;

/**
 * 
 * @author pedro.m
 */
public final class WebcamItem extends OverlayItem<Canvas>
{
  protected final WebcamRow row;

  /**
   * 
   * @param row
   * @param point
   * @param drawable
   */
  public WebcamItem(final WebcamRow row, final GeoPoint point, final MapDrawable<Canvas> drawable)
  {
    super(new StringBuilder().append(row.provider).append(Strings.CHAR_POINT).append(row.id).toString(), point, drawable);
    this.row = row;
  }

  @Override
  public String toString()
  {
    return row.provider + "." + row.id + "@" + getPoint();
  }

  @Override
  public void recycle()
  {
    final InfosDrawable infoDrawable = (InfosDrawable)getDrawable();
    if (infoDrawable.infos.size() < 2)
    {
      return;
    }
    final DrawableInfo imageInfo = infoDrawable.infos.get(1);
    if (imageInfo.image == null)
    {
      return;
    }
    imageInfo.image.recycle();
    imageInfo.image = null;
  }
}
