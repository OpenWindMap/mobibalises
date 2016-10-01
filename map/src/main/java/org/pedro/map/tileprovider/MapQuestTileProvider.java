package org.pedro.map.tileprovider;

import java.io.IOException;
import java.net.URL;

import org.pedro.map.Tile;

/**
 * 
 * @author pedro.m
 */
public abstract class MapQuestTileProvider extends NetworkTileProvider
{
  private static final String PARAM_MAP_TYPE  = "maptype";
  private static final String PARAM_TILE_X    = "tileX";
  private static final String PARAM_TILE_Y    = "tileY";
  private static final String PARAM_ZOOM      = "zoom";

  private static final String URL             = " http://otile1.mqcdn.com/tiles/1.0.0/{" + PARAM_MAP_TYPE + "}/{" + PARAM_ZOOM + "}/{" + PARAM_TILE_X + "}/{" + PARAM_TILE_Y + "}.png";

  private static final String REGEXP_MAP_TYPE = "\\{" + PARAM_MAP_TYPE + "\\}";
  private static final String REGEXP_TILE_X   = "\\{" + PARAM_TILE_X + "\\}";
  private static final String REGEXP_TILE_Y   = "\\{" + PARAM_TILE_Y + "\\}";
  private static final String REGEXP_ZOOM     = "\\{" + PARAM_ZOOM + "\\}";

  private final String        mapType;

  /**
   * 
   * @param key
   * @param mapType
   */
  public MapQuestTileProvider(final String key, final String mapType)
  {
    super(key);
    this.mapType = mapType;
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
    urlString = urlString.replaceAll(REGEXP_MAP_TYPE, mapType);
    urlString = urlString.replaceAll(REGEXP_ZOOM, Integer.toString(tile.zoom));
    urlString = urlString.replaceAll(REGEXP_TILE_X, Long.toString(tile.x));
    urlString = urlString.replaceAll(REGEXP_TILE_Y, Long.toString(tile.y));

    return new URL(urlString);
  }
}
