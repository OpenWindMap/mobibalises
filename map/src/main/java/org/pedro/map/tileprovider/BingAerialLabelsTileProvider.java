package org.pedro.map.tileprovider;

/**
 * 
 * @author pedro.m
 */
public final class BingAerialLabelsTileProvider extends BingTileProvider
{
  public static final String  KEY           = "bing_aerial_labels";
  private static final String AERIAL_LABELS = "AerialWithLabels";

  /**
   * 
   * @param apiKey
   */
  public BingAerialLabelsTileProvider(final String apiKey)
  {
    super(KEY, apiKey);
  }

  @Override
  public String getMapType()
  {
    return AERIAL_LABELS;
  }
}
