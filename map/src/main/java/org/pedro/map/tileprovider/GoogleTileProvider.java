package org.pedro.map.tileprovider;

import java.io.IOException;
import java.net.URL;

import org.pedro.map.MercatorProjection;
import org.pedro.map.Tile;

/**
 * 
 * @author pedro.m
 */
public abstract class GoogleTileProvider extends NetworkTileProvider
{
  private static final double TILE_SIZE_SUR_2  = TILE_SIZE / 2;

  private static final String PARAM_LATITUDE   = "latitude";
  private static final String PARAM_LONGITUDE  = "longitude";
  private static final String PARAM_ZOOM       = "zoom";
  private static final String PARAM_MAPTYPE    = "maptype";
  private static final String PARAM_API_KEY    = "key";

  private static final String URL              = "http://maps.googleapis.com/maps/api/staticmap?center={" + PARAM_LATITUDE + "},{" + PARAM_LONGITUDE + "}&zoom={" + PARAM_ZOOM + "}&size=" + TILE_SIZE + "x" + TILE_SIZE
                                                   + "&format=jpg&maptype={" + PARAM_MAPTYPE + "}&sensor=false";

  private static final String REGEXP_LATITUDE  = "\\{" + PARAM_LATITUDE + "\\}";
  private static final String REGEXP_LONGITUDE = "\\{" + PARAM_LONGITUDE + "\\}";
  private static final String REGEXP_ZOOM      = "\\{" + PARAM_ZOOM + "\\}";
  private static final String REGEXP_MAPTYPE   = "\\{" + PARAM_MAPTYPE + "\\}";

  private final String        apiParamUrl;

  /**
   * 
   * @param key
   * @param apiKey
   */
  public GoogleTileProvider(final String key, final String apiKey)
  {
    super(key);
    if ((apiKey != null) && (apiKey.trim().length() > 0))
    {
      apiParamUrl = '&' + PARAM_API_KEY + '=' + apiKey;
    }
    else
    {
      apiParamUrl = null;
    }
  }

  @Override
  public final int getMinZoomLevel()
  {
    return 0;
  }

  @Override
  public final URL getTileURL(final Tile tile) throws IOException
  {
    // Longitude et latitude du centre de la tuile
    final double longitude = MercatorProjection.pixelXToLongitude(tile.x * TILE_SIZE + TILE_SIZE_SUR_2, tile.zoom);
    final double latitude = MercatorProjection.pixelYToLatitude(tile.y * TILE_SIZE + TILE_SIZE_SUR_2, tile.zoom);

    // URL de base
    String urlString = URL;
    urlString = urlString.replaceAll(REGEXP_ZOOM, Integer.toString(tile.zoom));
    urlString = urlString.replaceAll(REGEXP_LONGITUDE, Double.toString(longitude));
    urlString = urlString.replaceAll(REGEXP_LATITUDE, Double.toString(latitude));
    urlString = urlString.replaceAll(REGEXP_MAPTYPE, getMapType());

    // Utilisation d'une clef d'API ?
    StringBuffer urlBuffer = new StringBuffer(urlString);
    if (apiParamUrl != null)
    {
      urlBuffer.append(apiParamUrl);
    }

    return new URL(urlBuffer.toString());
  }

  /**
   * 
   * @return
   */
  public abstract String getMapType();
}
