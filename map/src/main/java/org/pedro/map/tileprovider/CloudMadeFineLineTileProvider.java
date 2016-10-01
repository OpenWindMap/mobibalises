package org.pedro.map.tileprovider;

/**
 * 
 * @author pedro.m
 */
@Deprecated
public final class CloudMadeFineLineTileProvider extends CloudMadeTileProvider
{
  public static final String  KEY       = "cloudmade_fineline";
  private static final String FINE_LINE = "2";

  /**
   * 
   * @param apiKey
   * @param token
   */
  public CloudMadeFineLineTileProvider(final String apiKey, final String token)
  {
    super(KEY, apiKey, token);
  }

  @Override
  public String getStyleId()
  {
    return FINE_LINE;
  }
}
