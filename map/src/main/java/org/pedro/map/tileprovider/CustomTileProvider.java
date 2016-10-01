package org.pedro.map.tileprovider;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;

import org.pedro.map.MercatorProjection;
import org.pedro.map.Tile;

/**
 * Fournisseur local de tuiles.
 * tileMask : format du nom du fichier, avec "{zoom}" pour le niveau de zoom, "{x}" pour l'indice latitude,
 * "{y}" pour l'indice longitude, "{center_lat}" pour la latitude du centre de la tuile, "{center_lon}" pour
 * la longitude du centre de la tuile
 * 
 * @author pedro.m
 */
public class CustomTileProvider extends AbstractURLTileProvider
{
  private static final double TILE_SIZE_SUR_2        = TILE_SIZE / 2;

  private static final String REGEXP_ZOOM            = "\\{zoom\\}";
  private static final String REGEXP_X               = "\\{x\\}";
  private static final String REGEXP_Y               = "\\{y\\}";
  private static final String FIELD_CENTER_LAT       = "center_lat";
  private static final String FIELD_CENTER_LON       = "center_lon";
  private static final String ZONE_CENTER_LAT        = "{" + FIELD_CENTER_LAT + "}";
  private static final String ZONE_CENTER_LON        = "{" + FIELD_CENTER_LON + "}";
  private static final String REGEXP_CENTER_LAT      = "\\{" + FIELD_CENTER_LAT + "\\}";
  private static final String REGEXP_CENTER_LON      = "\\{" + FIELD_CENTER_LON + "\\}";
  private static final String REPLACEMENT_ZOOM       = "{0}";
  private static final String REPLACEMENT_X          = "{1}";
  private static final String REPLACEMENT_Y          = "{2}";
  private static final String REPLACEMENT_CENTER_LAT = "{3}";
  private static final String REPLACEMENT_CENTER_LON = "{4}";

  private final int           minZoomLevel;
  private final int           maxZoomLevel;
  private final String        tileMask;
  private final boolean       calculateCenterLatLon;
  private final boolean       needsCache;

  /**
   * 
   * @param key
   * @param minZoomLevel
   * @param maxZoomLevel
   * @param directory
   * @param tileMask
   */
  public CustomTileProvider(final String key, final int minZoomLevel, final int maxZoomLevel, final String tileMask, final boolean needsCache)
  {
    super(key);
    this.minZoomLevel = minZoomLevel;
    this.maxZoomLevel = maxZoomLevel;
    this.tileMask = tileMask.replaceAll(REGEXP_ZOOM, REPLACEMENT_ZOOM).replaceAll(REGEXP_X, REPLACEMENT_X).replaceAll(REGEXP_Y, REPLACEMENT_Y).replaceAll(REGEXP_CENTER_LAT, REPLACEMENT_CENTER_LAT)
        .replaceAll(REGEXP_CENTER_LON, REPLACEMENT_CENTER_LON);
    this.calculateCenterLatLon = tileMask.contains(ZONE_CENTER_LAT) || tileMask.contains(ZONE_CENTER_LON);
    this.needsCache = needsCache;
  }

  @Override
  public int getMinZoomLevel()
  {
    return minZoomLevel;
  }

  @Override
  public int getMaxZoomLevel()
  {
    return maxZoomLevel;
  }

  @Override
  public URL getTileURL(final Tile tile) throws IOException
  {
    final String path;
    if (calculateCenterLatLon)
    {
      // Calcul de la longitude et latitude du centre de la tuile necessaire
      final double longitude = MercatorProjection.pixelXToLongitude(tile.x * TILE_SIZE + TILE_SIZE_SUR_2, tile.zoom);
      final double latitude = MercatorProjection.pixelYToLatitude(tile.y * TILE_SIZE + TILE_SIZE_SUR_2, tile.zoom);

      // Elaboration du path
      path = MessageFormat.format(tileMask, Integer.valueOf(tile.zoom), Long.valueOf(tile.x), Long.valueOf(tile.y), Double.toString(latitude), Double.toString(longitude));
    }
    else
    {
      // Elaboration du path directe
      path = MessageFormat.format(tileMask, Integer.valueOf(tile.zoom), Long.valueOf(tile.x), Long.valueOf(tile.y));
    }

    return new URL(path);
  }

  @Override
  public boolean needsCache(final Tile tile)
  {
    return needsCache;
  }

  @Override
  public boolean hasTile(final Tile tile) throws IOException
  {
    return (tile.zoom >= minZoomLevel) && (tile.zoom <= maxZoomLevel);
  }

  /**
   * 
   * @param args
   */
  /*
  public static void main(final String[] args)
  {
    try
    {
      final CustomTileProvider test = new CustomTileProvider("test", 0, 15, "file:/sdcard/.maps/test/test_{zoom}_{x}_{y}.png", false);
      final Tile tile = new Tile(10, 10, 5);
      final URL url = test.getTileURL(tile);
      System.out.println("URL = " + url);
    }
    catch (final IOException e)
    {
      e.printStackTrace(System.err);
    }
  }
  */
}
