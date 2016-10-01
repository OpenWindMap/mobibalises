package org.pedro.map.tileprovider;

/**
 * 
 * @author pedro.m
 */
public final class BingAerialTileProvider extends BingTileProvider
{
  public static final String  KEY    = "bing_aerial";
  private static final String AERIAL = "Aerial";

  /**
   * 
   * @param apiKey
   */
  public BingAerialTileProvider(final String apiKey)
  {
    super(KEY, apiKey);
  }

  @Override
  public String getMapType()
  {
    return AERIAL;
  }
}
