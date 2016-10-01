package org.pedro.android.mobibalises_common.map;

import org.pedro.map.MapDrawable;
import org.pedro.map.Point;
import org.pedro.map.Rect;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

/**
 * 
 * @author pedro.m
 *
 */
public final class LocationDrawable implements MapDrawable<Canvas>
{
  private final Drawable drawable;
  private final Rect     bounds = new Rect();

  /**
   * 
   * @param resources
   * @param resourceId
   */
  public LocationDrawable(final Resources resources, final int resourceId)
  {
    drawable = resources.getDrawable(resourceId);
    final int width = drawable.getIntrinsicWidth();
    final int height = drawable.getIntrinsicHeight();
    bounds.left = -(width / 2);
    bounds.top = -(height / 2);
    bounds.right = (width / 2);
    bounds.bottom = (height / 2);
  }

  @Override
  public void draw(final Canvas canvas, final Point point)
  {
    drawable.setBounds(point.x + bounds.left, point.y + bounds.top, point.x + bounds.right, point.y + bounds.bottom);
    drawable.draw(canvas);
  }

  @Override
  public Rect getDisplayBounds()
  {
    return bounds;
  }

  @Override
  public Rect getInteractiveBounds()
  {
    return bounds;
  }
}
