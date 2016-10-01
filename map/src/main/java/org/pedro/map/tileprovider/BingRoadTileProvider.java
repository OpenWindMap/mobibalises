package org.pedro.map.tileprovider;

/**
 * 
 * @author pedro.m
 */
public final class BingRoadTileProvider extends BingTileProvider
{
  public static final String  KEY  = "bing_road";
  private static final String ROAD = "Road";

  /**
   * 
   * @param apiKey
   */
  public BingRoadTileProvider(final String apiKey)
  {
    super(KEY, apiKey);
  }

  @Override
  public String getMapType()
  {
    return ROAD;
  }
}
