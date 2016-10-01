package org.pedro.map.tileprovider;

/**
 * 
 * @author pedro.m
 */
public class IGNSatelliteTileProvider extends IGNTileProvider
{
  public static final String  KEY   = "ignsat";

  private static final String LAYER = "ORTHOIMAGERY.ORTHOPHOTOS";

  /**
   * 
   * @param apiKey
   * @param debug
   */
  public IGNSatelliteTileProvider(final String apiKey, final boolean debug)
  {
    super(KEY, apiKey, debug);
  }

  @Override
  public int getMaxZoomLevel()
  {
    return 18;
  }

  @Override
  public String getLayer()
  {
    return LAYER;
  }
}
