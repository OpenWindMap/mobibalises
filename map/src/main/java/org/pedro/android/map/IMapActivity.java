package org.pedro.android.map;


/**
 * 
 * @author pedro.m
 */
public interface IMapActivity
{
  /**
   * 
   */
  public void setGraphicsMode();

  /**
   * 
   * @return
   */
  public boolean isGraphicsLightMode();

  /**
   * 
   * @return
   */
  public MapView getMapView();
  
  /**
   * 
   * @param consumedByOverlays
   */
  public void onSingleTap(final boolean consumedByOverlays);
}
