package org.pedro.map.tileprovider;

/**
 * 
 * @author pedro.m
 */
public final class MRIReliefTileProvider extends MRITileProvider
{
  public static final String  KEY   = "mri_relief";

  private static final String LAYER = "relief";

  /**
   * 
   */
  public MRIReliefTileProvider()
  {
    super(KEY);
  }

  @Override
  public int getMaxZoomLevel()
  {
    return 15;
  }

  @Override
  protected String getLayer()
  {
    return LAYER;
  }
}
