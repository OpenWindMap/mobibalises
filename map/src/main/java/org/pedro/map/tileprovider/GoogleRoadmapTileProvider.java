package org.pedro.map.tileprovider;

/**
 * 
 * @author pedro.m
 */
public final class GoogleRoadmapTileProvider extends GoogleTileProvider
{
  public static final String  KEY     = "google_roadmap";
  private static final String ROADMAP = "roadmap";

  /**
   * 
   * @param apiKey
   */
  public GoogleRoadmapTileProvider(final String apiKey)
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
    return ROADMAP;
  }
}
