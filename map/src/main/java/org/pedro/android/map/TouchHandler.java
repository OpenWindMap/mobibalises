package org.pedro.android.map;

import android.content.Context;
import android.view.MotionEvent;

/**
 * 
 * @author pedro.m
 */
public interface TouchHandler
{
  /**
   * 
   * @param event
   * @return
   */
  public boolean handleTouchEvent(MotionEvent event);

  /**
   * 
   * @param context
   * @param mapView
   * @return
   */
  public boolean initHandler(Context context, MapView mapView);

  /**
   * 
   */
  public void shutdownHandler();
}
