package org.pedro.map.tileprovider;

/**
 * 
 * @author pedro.m
 */
@Deprecated
public final class CloudMadeOriginalTileProvider extends CloudMadeTileProvider
{
  public static final String  KEY      = "cloudmade_original";
  private static final String ORIGINAL = "1";

  /**
   * 
   * @param apiKey
   * @param token
   */
  public CloudMadeOriginalTileProvider(final String apiKey, final String token)
  {
    super(KEY, apiKey, token);
  }

  @Override
  public String getStyleId()
  {
    return ORIGINAL;
  }
}
