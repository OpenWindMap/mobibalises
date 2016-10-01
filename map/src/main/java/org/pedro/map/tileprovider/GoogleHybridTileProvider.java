package org.pedro.map.tileprovider;

/**
 * 
 * @author pedro.m
 */
public final class GoogleHybridTileProvider extends GoogleTileProvider
{
  public static final String  KEY    = "google_hybrid";
  private static final String HYBRID = "hybrid";

  /**
   * 
   * @param apiKey
   */
  public GoogleHybridTileProvider(final String apiKey)
  {
    super(KEY, apiKey);
  }

  @Override
  public int getMaxZoomLevel()
  {
    return 17;
  }

  @Override
  public String getMapType()
  {
    return HYBRID;
  }
}
