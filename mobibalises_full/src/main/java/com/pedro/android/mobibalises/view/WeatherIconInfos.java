package com.pedro.android.mobibalises.view;

import com.pedro.android.mobibalises.R;

/**
 * 
 * @author pedro.m
 */
public class WeatherIconInfos
{
  /**
   * 
   * @author pedro.m
   */
  public enum Nuage
  {
    CLAIR(-1, true), UN_DEUX(R.drawable.ic_nuage_1_2, true), UN_DEUX_CBCU(R.drawable.ic_nuage_1_2_cbcu, true), TROIS_QUATRE(R.drawable.ic_nuage_3_4, true), TROIS_QUATRE_CBCU(R.drawable.ic_nuage_3_4_cbcu, true), CINQ_SIX(
        R.drawable.ic_nuage_5_6, true), CINQ_SIX_CBCU(R.drawable.ic_nuage_5_6_cbcu, true), SEPT_HUIT(R.drawable.ic_nuage_7_8, false), SEPT_HUIT_CBCU(R.drawable.ic_nuage_7_8_cbcu, false);

    private final int     resourceId;
    private final boolean drawSunMoon;

    /**
     * 
     * @param resourceId
     * @param drawSunMoon
     */
    private Nuage(final int resourceId, final boolean drawSunMoon)
    {
      this.resourceId = resourceId;
      this.drawSunMoon = drawSunMoon;
    }

    /**
     * 
     * @return
     */
    public int getResourceId()
    {
      return resourceId;
    }

    /**
     * 
     * @return
     */
    public boolean drawSunMoon()
    {
      return drawSunMoon;
    }
  }

  /**
   * 
   * @author pedro.m
   */
  public enum Precipitation
  {
    DRY(-1), LITTLE_RAIN(R.drawable.ic_pluie_1), RAIN(R.drawable.ic_pluie_2), HEAVY_RAIN(R.drawable.ic_pluie_3), STORMY_RAIN(R.drawable.ic_pluie_4);

    private final int resourceId;

    /**
     * 
     * @param resourceId
     */
    private Precipitation(final int resourceId)
    {
      this.resourceId = resourceId;
    }

    /**
     * 
     * @return
     */
    public int getResourceId()
    {
      return resourceId;
    }
  }

  protected Nuage         nuage         = null;
  protected Precipitation precipitation = null;
  protected boolean       night         = false;
}
