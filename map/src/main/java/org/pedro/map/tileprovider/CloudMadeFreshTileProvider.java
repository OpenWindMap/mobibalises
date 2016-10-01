package org.pedro.map.tileprovider;

/**
 * 
 * @author pedro.m
 */
@Deprecated
public final class CloudMadeFreshTileProvider extends CloudMadeTileProvider
{
  public static final String  KEY   = "cloudmade_fresh";
  private static final String FRESH = "997";

  /**
   * 
   * @param apiKey
   * @param token
   */
  public CloudMadeFreshTileProvider(final String apiKey, final String token)
  {
    super(KEY, apiKey, token);
  }

  @Override
  public String getStyleId()
  {
    return FRESH;
  }
}
