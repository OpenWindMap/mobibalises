package org.pedro.android.mobibalises_common.view;

import org.pedro.android.mobibalises_common.provider.BaliseProviderUtils.TendanceVent;

import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * 
 * @author pedro.m
 */
public class WindIconInfos
{
  protected final Path   pathFleche       = new Path();
  protected final Matrix matrix           = new Matrix();
  protected String       texteValeur;
  protected boolean      baliseActive;
  protected boolean      directionValide;
  protected boolean      releveValide;
  protected Paint        paintCercleExterieurRemplissage;
  protected Paint        paintInterieur;
  protected Paint        paintValeur;
  protected boolean      windLimitOk;
  protected TendanceVent tendanceVent;
  protected Paint        paintTendanceVent;
  protected final Path   pathTendanceVent = new Path();
  protected float        deltaYValeur;
  protected boolean      drawPeremption   = true;

  /**
   * @return the baliseActive
   */
  public boolean isBaliseActive()
  {
    return baliseActive;
  }

  /**
   * @return the directionValide
   */
  public boolean isDirectionValide()
  {
    return directionValide;
  }

  /**
   * @return the releveValide
   */
  public boolean isReleveValide()
  {
    return releveValide;
  }

  /**
   * @return the windLimitOk
   */
  public boolean isWindLimitOk()
  {
    return windLimitOk;
  }

  /**
   * 
   * @param drawPeremption
   */
  public void setDrawPeremption(final boolean drawPeremption)
  {
    this.drawPeremption = drawPeremption;
  }
}
