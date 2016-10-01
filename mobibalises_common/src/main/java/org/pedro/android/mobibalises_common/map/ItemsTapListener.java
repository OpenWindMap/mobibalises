package org.pedro.android.mobibalises_common.map;

import java.util.List;

import org.pedro.map.OverlayItem;

import android.graphics.Canvas;

/**
 * 
 * @author pedro.m
 */
public interface ItemsTapListener
{
  /**
   * 
   * @param tapItems
   */
  public void onItemsTap(final List<OverlayItem<Canvas>> tapItems);

  /**
   * 
   * @param tapItems
   */
  public void onItemsLongTap(final List<OverlayItem<Canvas>> tapItems);
}
