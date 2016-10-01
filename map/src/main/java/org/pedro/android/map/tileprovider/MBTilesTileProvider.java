package org.pedro.android.map.tileprovider;

import java.io.IOException;
import java.text.MessageFormat;

import org.pedro.map.Tile;
import org.pedro.map.tileprovider.AbstractTileProvider;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Fournisseur local de tuiles, au format MBTiles.
 * 
 * @author pedro.m
 */
public class MBTilesTileProvider extends AbstractTileProvider
{
  private static final String REQ_MIN_MAX   = "SELECT min(zoom_level), max(zoom_level) FROM tiles";
  private static final String REQ_TEST_TILE = "SELECT count(*) FROM tiles WHERE tile_column = {0} AND tile_row = {1} AND zoom_level = {2}";
  private static final String REQ_TILE      = "SELECT tile_data FROM tiles WHERE tile_column = {0} AND tile_row = {1} AND zoom_level = {2}";

  private final String        filename;
  private int                 minZoomLevel  = Integer.MAX_VALUE;
  private int                 maxZoomLevel  = Integer.MIN_VALUE;

  private SQLiteDatabase      database;

  /**
   * 
   * @param key
   * @param filename
   */
  public MBTilesTileProvider(final String key, final String filename)
  {
    super(key);
    this.filename = filename;
    init();
  }

  /**
   * 
   */
  private void init()
  {
    // Initialisations
    Cursor cursor = null;

    try
    {
      // Ouverture
      database = SQLiteDatabase.openDatabase(filename, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READONLY);

      // Requete min-max
      cursor = database.rawQuery(REQ_MIN_MAX, null);

      // Analyse
      final boolean next = cursor.moveToFirst();
      if (next)
      {
        minZoomLevel = cursor.getInt(0);
        maxZoomLevel = cursor.getInt(1);
      }
    }
    catch (final Throwable th)
    {
      // Erreur
      th.printStackTrace(System.err);
    }
    finally
    {
      if (cursor != null)
      {
        cursor.close();
      }
    }
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
  public boolean needsCache(final Tile tile)
  {
    return false;
  }

  /**
   * 
   * @param tile
   * @return
   */
  private static long getTileRow(final Tile tile)
  {
    return (long)Math.pow(2, tile.zoom) - 1 - tile.y;
  }

  @Override
  public boolean hasTile(final Tile tile) throws IOException
  {
    // Initialisations
    Cursor cursor = null;

    try
    {
      // Initialisations
      boolean hasTile = false;

      // Requete
      final String req = MessageFormat.format(REQ_TEST_TILE, Long.toString(tile.x), Long.toString(getTileRow(tile)), Integer.toString(tile.zoom));
      cursor = database.rawQuery(req, null);

      // Analyse
      final boolean next = cursor.moveToFirst();
      if (next)
      {
        final int count = cursor.getInt(0);
        hasTile = (count > 0);
      }

      // Fin
      return hasTile;
    }
    catch (final Throwable th)
    {
      // Erreur
      th.printStackTrace(System.err);
      return false;
    }
    finally
    {
      // Fermeture
      if (cursor != null)
      {
        cursor.close();
      }
    }
  }

  @Override
  public int readData(final Tile tile, final byte[] buffer) throws IOException
  {
    // Initialisations
    Cursor cursor = null;
    try
    {
      // Initialisations
      int length = 0;

      // Requete
      final String req = MessageFormat.format(REQ_TILE, Long.toString(tile.x), Long.toString(getTileRow(tile)), Integer.toString(tile.zoom));
      cursor = database.rawQuery(req, null);

      // Analyse
      final boolean next = cursor.moveToFirst();
      if (next)
      {
        final byte[] blob = cursor.getBlob(0);
        length = blob.length;
        System.arraycopy(blob, 0, buffer, 0, length);
      }

      // Fin
      return length;
    }
    catch (final Throwable th)
    {
      // Erreur
      th.printStackTrace(System.err);
      return 0;
    }
    finally
    {
      // Fermeture
      if (cursor != null)
      {
        cursor.close();
      }
    }
  }

  @Override
  public void shutdown()
  {
    if (database != null)
    {
      database.close();
    }
  }
}
