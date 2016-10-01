package org.pedro.map.tileprovider;

/**
 * 
 * @author pedro.m
 */
public class IGNMapTileProvider extends IGNTileProvider
{
  public static final String  KEY   = "ignmap";

  private static final String LAYER = "GEOGRAPHICALGRIDSYSTEMS.MAPS";

  /**
   * 
   * @param apiKey
   * @param debug
   */
  public IGNMapTileProvider(final String apiKey, final boolean debug)
  {
    super(KEY, apiKey, debug);
  }

  @Override
  public int getMaxZoomLevel()
  {
    return 16;
  }

  @Override
  public String getLayer()
  {
    return LAYER;
  }
}
