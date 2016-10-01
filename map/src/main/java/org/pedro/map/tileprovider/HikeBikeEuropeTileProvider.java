package org.pedro.map.tileprovider;

import java.io.IOException;
import java.net.URL;

import org.pedro.map.Tile;

/**
 * 
 * @author pedro.m
 */
public final class HikeBikeEuropeTileProvider extends NetworkTileProvider
{
  public static final String  KEY           = "hike_bike_europe";

  private static final String PARAM_TILE_X  = "tileX";
  private static final String PARAM_TILE_Y  = "tileY";
  private static final String PARAM_ZOOM    = "zoom";

  private static final String URL           = "http://toolserver.org/tiles/hikebike/{" + PARAM_ZOOM + "}/{" + PARAM_TILE_X + "}/{" + PARAM_TILE_Y + "}.png";

  private static final String REGEXP_TILE_X = "\\{" + PARAM_TILE_X + "\\}";
  private static final String REGEXP_TILE_Y = "\\{" + PARAM_TILE_Y + "\\}";
  private static final String REGEXP_ZOOM   = "\\{" + PARAM_ZOOM + "\\}";

  /**
   * 
   */
  public HikeBikeEuropeTileProvider()
  {
    super(KEY);
  }

  @Override
  public int getMaxZoomLevel()
  {
    return 18;
  }

  @Override
  public int getMinZoomLevel()
  {
    return 0;
  }

  @Override
  public java.net.URL getTileURL(final Tile tile) throws IOException
  {
    String urlString = URL;
    urlString = urlString.replaceAll(REGEXP_ZOOM, Integer.toString(tile.zoom));
    urlString = urlString.replaceAll(REGEXP_TILE_X, Long.toString(tile.x));
    urlString = urlString.replaceAll(REGEXP_TILE_Y, Long.toString(tile.y));

    return new URL(urlString);
  }
}
