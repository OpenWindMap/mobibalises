package com.pedro.android.mobibalises.view;

import android.graphics.Path;

/**
 * 
 * @author pedro.m
 */
public class AdditionalWindIconInfos
{
  protected boolean    baliseActive;
  protected boolean    releveValide;
  protected boolean    variationVentValide;
  protected final Path pathVariationVent = new Path();

  /**
   * @return the baliseActive
   */
  public boolean isBaliseActive()
  {
    return baliseActive;
  }

  /**
   * @return the releveValide
   */
  public boolean isReleveValide()
  {
    return releveValide;
  }

  /**
   * @return the variationVentValide
   */
  public boolean isVariationVentValide()
  {
    return variationVentValide;
  }
}
