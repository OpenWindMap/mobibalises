package org.pedro.map.tileprovider;

import java.io.IOException;
import java.net.URL;

import org.pedro.map.MercatorProjection;
import org.pedro.map.Tile;

/**
 * 
 * @author pedro.m
 */
public abstract class BingTileProvider extends NetworkTileProvider
{
  private static final String PARAM_LATITUDE   = "latitude";
  private static final String PARAM_LONGITUDE  = "longitude";
  private static final String PARAM_ZOOM       = "zoom";
  private static final String PARAM_MAPTYPE    = "maptype";
  private static final String PARAM_APIKEY     = "key";

  private static final String URL              = "http://dev.virtualearth.net/REST/v1/Imagery/Map/{" + PARAM_MAPTYPE + "}/{" + PARAM_LATITUDE + "},{" + PARAM_LONGITUDE + "}/{" + PARAM_ZOOM + "}?mapSize=" + TILE_SIZE + "," + TILE_SIZE
                                                   + "&key={" + PARAM_APIKEY + "}";

  private static final String REGEXP_LATITUDE  = "\\{" + PARAM_LATITUDE + "\\}";
  private static final String REGEXP_LONGITUDE = "\\{" + PARAM_LONGITUDE + "\\}";
  private static final String REGEXP_ZOOM      = "\\{" + PARAM_ZOOM + "\\}";
  private static final String REGEXP_MAPTYPE   = "\\{" + PARAM_MAPTYPE + "\\}";
  private static final String REGEXP_APIKEY    = "\\{" + PARAM_APIKEY + "\\}";

  protected final String      apiKey;

  /**
   * 
   * @param key
   * @param apiKey
   */
  public BingTileProvider(final String key, final String apiKey)
  {
    super(key);
    this.apiKey = apiKey;
  }

  @Override
  public final int getMinZoomLevel()
  {
    return 1;
  }

  @Override
  public final int getMaxZoomLevel()
  {
    return 15;
  }

  @Override
  public final URL getTileURL(final Tile tile) throws IOException
  {
    // Longitude et latitude du centre de la tuile
    final double longitude = MercatorProjection.pixelXToLongitude(tile.x * TILE_SIZE + TILE_SIZE / 2, tile.zoom);
    final double latitude = MercatorProjection.pixelYToLatitude(tile.y * TILE_SIZE + TILE_SIZE / 2, tile.zoom);

    String urlString = URL;
    urlString = urlString.replaceAll(REGEXP_ZOOM, Integer.toString(tile.zoom));
    urlString = urlString.replaceAll(REGEXP_LONGITUDE, Double.toString(longitude));
    urlString = urlString.replaceAll(REGEXP_LATITUDE, Double.toString(latitude));
    urlString = urlString.replaceAll(REGEXP_MAPTYPE, getMapType());
    urlString = urlString.replaceAll(REGEXP_APIKEY, apiKey);

    return new URL(urlString);
  }

  /**
   * 
   * @return
   */
  public abstract String getMapType();
}
