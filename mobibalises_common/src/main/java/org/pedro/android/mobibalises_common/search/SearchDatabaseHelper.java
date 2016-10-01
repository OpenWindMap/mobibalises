package org.pedro.android.mobibalises_common.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.pedro.android.mobibalises_common.Strings;
import org.pedro.balises.Balise;
import org.pedro.spots.Spot;
import org.pedro.spots.dhv.DhvSpot;
import org.pedro.spots.ffvl.FfvlSpot;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * 
 * @author pedro.m
 */
public class SearchDatabaseHelper extends SQLiteOpenHelper
{
  private static final String DATABASE_NAME                   = "MobiBalisesSearch";
  private static final int    DATABASE_VERSION                = 1;

  public static final String  ITEM_TYPE_BALISE                = "1";
  public static final String  ITEM_TYPE_SPOT                  = "2";
  public static final String  ITEM_TYPE_WEBCAM                = "3";

  public static final String  COL_TYPE                        = "type";
  public static final String  COL_PROVIDER                    = "provider";

  private static final String TABLE_ITEM                      = "item";
  public static final String  COL_ITEM_SUBTYPE                = "subtype";
  public static final String  COL_ITEM_ID                     = "id";
  public static final String  COL_ITEM_SUBID                  = "subid";
  public static final String  COL_ITEM_NAME                   = "name";
  public static final String  COL_ITEM_NAME_NORM              = "name_norm";
  public static final String  COL_ITEM_LATITUDE               = "lat";
  public static final String  COL_ITEM_LONGITUDE              = "lon";
  private static final String SQL_TABLE_ITEM_CREATE           = "CREATE TABLE " + TABLE_ITEM + " (" + COL_TYPE + ", " + COL_ITEM_SUBTYPE + ", " + COL_PROVIDER + ", " + COL_ITEM_ID + ", " + COL_ITEM_SUBID + ", " + COL_ITEM_NAME + ", "
                                                                  + COL_ITEM_NAME_NORM + ", " + COL_ITEM_LATITUDE + ", " + COL_ITEM_LONGITUDE + ")";
  private static final String SQL_TABLE_ITEM_DROP             = "DROP TABLE IF EXISTS " + TABLE_ITEM;
  private static final String SQL_TABLE_ITEM_DELETE           = "DELETE FROM " + TABLE_ITEM + " WHERE " + COL_TYPE + " = ? AND " + COL_PROVIDER + " = ?";
  private static final String SQL_TABLE_ITEM_SEARCH           = "SELECT oid AS _id, " + COL_TYPE + ", " + COL_ITEM_SUBTYPE + ", " + COL_ITEM_NAME + ", " + COL_PROVIDER + ", " + COL_ITEM_ID + ", " + COL_ITEM_SUBID + ", " + COL_ITEM_LATITUDE
                                                                  + ", " + COL_ITEM_LONGITUDE + " FROM " + TABLE_ITEM + " WHERE " + COL_ITEM_NAME_NORM + " LIKE ? ORDER BY " + COL_TYPE + ", " + COL_ITEM_SUBTYPE + ", " + COL_PROVIDER + ", "
                                                                  + COL_ITEM_NAME;
  private static final String SQL_TABLE_ITEM_SEARCH_PROVIDERS = "SELECT DISTINCT " + COL_TYPE + ", " + COL_PROVIDER + " FROM " + TABLE_ITEM + " ORDER BY " + COL_TYPE + ", " + COL_PROVIDER;

  private static final String TABLE_MAJ                       = "maj";
  private static final String COL_MAJ_TS                      = "ts";
  private static final String SQL_TABLE_MAJ_CREATE            = "CREATE TABLE " + TABLE_MAJ + " (" + COL_TYPE + ", " + COL_PROVIDER + ", " + COL_MAJ_TS + ")";
  private static final String SQL_TABLE_MAJ_DROP              = "DROP TABLE IF EXISTS " + TABLE_MAJ;
  private static final String SQL_TABLE_MAJ_DELETE            = "DELETE FROM " + TABLE_MAJ + " WHERE " + COL_TYPE + " = ? AND " + COL_PROVIDER + " = ?";
  private static final String SQL_TABLE_MAJ_SELECT            = "SELECT " + COL_MAJ_TS + " FROM " + TABLE_MAJ + " WHERE " + COL_TYPE + " = ? AND " + COL_PROVIDER + " = ?";

  /**
   * 
   * @author pedro.m
   */
  public static class SearchItem
  {
    public String type;
    public String subtype;
    public String provider;
    public String id;
    public String subId;
    public String name;
    public double latitude;
    public double longitude;

    @Override
    public String toString()
    {
      return type + "/" + subtype + "/" + provider + "/" + id + "/" + subId + "/" + name + "/" + latitude + "/" + longitude;
    }
  }

  /**
   * 
   * @param context
   */
  public SearchDatabaseHelper(final Context context)
  {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(final SQLiteDatabase database)
  {
    Log.d(getClass().getSimpleName(), ">>> onCreate() : " + database);
    clean(database);
    Log.d(getClass().getSimpleName(), "<<< onCreate()");
  }

  @Override
  public void onUpgrade(final SQLiteDatabase database, int oldVersion, int newVersion)
  {
    Log.d(getClass().getSimpleName(), ">>> onUpgrade(" + oldVersion + ", " + newVersion + ") : " + database);
    Log.d(getClass().getSimpleName(), "<<< onUpgrade(" + oldVersion + ", " + newVersion + ") : " + database);
  }

  /**
   * 
   * @param database
   */
  public void clean(final SQLiteDatabase database)
  {
    Log.d(getClass().getSimpleName(), ">>> clean()");
    database.execSQL(SQL_TABLE_ITEM_DROP);
    database.execSQL(SQL_TABLE_ITEM_CREATE);

    database.execSQL(SQL_TABLE_MAJ_DROP);
    database.execSQL(SQL_TABLE_MAJ_CREATE);
    Log.d(getClass().getSimpleName(), "<<< clean()");
  }

  /**
   * 
   * @param database
   * @param providerKey
   */
  public void deleteBalisesForProvider(final SQLiteDatabase database, final String providerKey)
  {
    deleteForProvider(database, ITEM_TYPE_BALISE, providerKey);
  }

  /**
   * 
   * @param database
   * @param providerKey
   */
  public void deleteSpotsForProvider(final SQLiteDatabase database, final String providerKey)
  {
    deleteForProvider(database, ITEM_TYPE_SPOT, providerKey);
  }

  /**
   * 
   * @param type
   * @param database
   * @param providerKey
   */
  public void deleteForProvider(final SQLiteDatabase database, final String type, final String providerKey)
  {
    // Initialisations
    final Object[] args = new Object[] { type, providerKey };

    // Items
    Log.d(getClass().getSimpleName(), "deleteForProvider(" + type + ", " + providerKey + ") : " + SQL_TABLE_ITEM_DELETE);
    database.execSQL(SQL_TABLE_ITEM_DELETE, args);

    // MAJ
    Log.d(getClass().getSimpleName(), "deleteForProvider(" + type + ", " + providerKey + ") : " + SQL_TABLE_MAJ_DELETE);
    database.execSQL(SQL_TABLE_MAJ_DELETE, args);
  }

  /**
   * 
   * @param database
   * @param providerKey
   * @param balises
   * @param timestamp
   */
  public void insertBalisesForProvider(final SQLiteDatabase database, final String providerKey, final Collection<Balise> balises, final long timestamp)
  {
    // Item
    final ContentValues itemValues = new ContentValues();
    itemValues.put(COL_TYPE, ITEM_TYPE_BALISE);
    itemValues.put(COL_PROVIDER, providerKey);

    // Items
    for (final Balise balise : balises)
    {
      itemValues.put(COL_ITEM_ID, balise.id);
      itemValues.put(COL_ITEM_NAME, balise.nom);
      itemValues.put(COL_ITEM_NAME_NORM, Strings.removeAccents(balise.nom));
      itemValues.put(COL_ITEM_LATITUDE, Double.valueOf(balise.latitude));
      itemValues.put(COL_ITEM_LONGITUDE, Double.valueOf(balise.longitude));
      database.insert(TABLE_ITEM, null, itemValues);
    }
    Log.d(getClass().getSimpleName(), "insertBalisesForProvider(" + providerKey + ", ...) : " + balises.size());

    // MAJ
    final ContentValues majValues = new ContentValues();
    majValues.put(COL_TYPE, ITEM_TYPE_BALISE);
    majValues.put(COL_PROVIDER, providerKey);
    majValues.put(COL_MAJ_TS, Long.valueOf(timestamp));
    database.insert(TABLE_MAJ, null, majValues);
    Log.d(getClass().getSimpleName(), "insertBalisesForProvider(" + providerKey + ", ...) : " + majValues);
  }

  /**
   * 
   * @param database
   * @param providerKey
   * @param balises
   * @param timestamp
   */
  public void insertSpotsForProvider(final SQLiteDatabase database, final String providerKey, final Collection<Spot> spots, final long timestamp)
  {
    // Item
    final ContentValues itemValues = new ContentValues();
    itemValues.put(COL_TYPE, ITEM_TYPE_SPOT);
    itemValues.put(COL_PROVIDER, providerKey);

    // Items
    for (final Spot spot : spots)
    {
      itemValues.put(COL_ITEM_SUBTYPE, spot.type.getKey());
      itemValues.put(COL_ITEM_ID, spot.id);
      // Spot FFVL
      if (FfvlSpot.class.isAssignableFrom(spot.getClass()))
      {
        itemValues.put(COL_ITEM_SUBID, ((FfvlSpot)spot).idSite);
      }
      // Spot DHV
      else if (DhvSpot.class.isAssignableFrom(spot.getClass()))
      {
        itemValues.put(COL_ITEM_SUBID, ((DhvSpot)spot).idSite);
      }
      itemValues.put(COL_ITEM_NAME, spot.nom);
      itemValues.put(COL_ITEM_NAME_NORM, Strings.removeAccents(spot.nom));
      itemValues.put(COL_ITEM_LATITUDE, spot.latitude);
      itemValues.put(COL_ITEM_LONGITUDE, spot.longitude);
      database.insert(TABLE_ITEM, null, itemValues);
    }
    Log.d(getClass().getSimpleName(), "insertSpotsForProvider(" + providerKey + ", ...) : " + spots.size());

    // MAJ
    final ContentValues majValues = new ContentValues();
    majValues.put(COL_TYPE, ITEM_TYPE_SPOT);
    majValues.put(COL_PROVIDER, providerKey);
    majValues.put(COL_MAJ_TS, Long.valueOf(timestamp));
    database.insert(TABLE_MAJ, null, majValues);
    Log.d(getClass().getSimpleName(), "insertSpotsForProvider(" + providerKey + ", ...) : " + majValues);
  }

  /**
   * 
   * @param database
   * @param providerKey
   * @return
   */
  public long getBaliseProviderTimestamp(final SQLiteDatabase database, final String providerKey)
  {
    Log.d(getClass().getSimpleName(), "getBaliseProviderTimestamp(" + providerKey + ") : " + SQL_TABLE_MAJ_SELECT);
    final String[] args = new String[] { ITEM_TYPE_BALISE, providerKey };
    final Cursor cursor = database.rawQuery(SQL_TABLE_MAJ_SELECT, args);

    try
    {
      if (cursor.moveToFirst())
      {
        return cursor.getLong(0);
      }

      return Long.MIN_VALUE;
    }
    finally
    {
      cursor.close();
    }
  }

  /**
   * 
   * @param database
   * @param providerKey
   * @return
   */
  public long getSpotProviderTimestamp(final SQLiteDatabase database, final String providerKey)
  {
    Log.d(getClass().getSimpleName(), "getSpotProviderTimestamp(" + providerKey + ") : " + SQL_TABLE_MAJ_SELECT);
    final String[] args = new String[] { ITEM_TYPE_SPOT, providerKey };
    final Cursor cursor = database.rawQuery(SQL_TABLE_MAJ_SELECT, args);

    try
    {
      if (cursor.moveToFirst())
      {
        return cursor.getLong(0);
      }

      return Long.MIN_VALUE;
    }
    finally
    {
      cursor.close();
    }
  }

  /**
   * 
   * @param database
   * @param search
   * @return
   */
  public List<SearchItem> searchItems(final SQLiteDatabase database, final String search)
  {
    // Initialisations
    final List<SearchItem> items = new ArrayList<SearchItem>();
    final String[] args = new String[] { "%" + Strings.removeAccents(search) + "%" };
    Log.d(getClass().getSimpleName(), "searchItems(" + search + ":" + args[0] + ") : " + SQL_TABLE_ITEM_SEARCH);

    final Cursor cursor = database.rawQuery(SQL_TABLE_ITEM_SEARCH, args);

    if (cursor.moveToFirst())
    {
      do
      {
        final SearchItem item = new SearchItem();
        items.add(item);

        int col = 1; // OID ininteressant
        item.type = cursor.getString(col++);
        item.subtype = cursor.getString(col++);
        item.name = cursor.getString(col++);
        item.provider = cursor.getString(col++);
        item.id = cursor.getString(col++);
        item.subId = cursor.getString(col++);
        item.latitude = cursor.getDouble(col++);
        item.longitude = cursor.getDouble(col++);
      } while (cursor.moveToNext());
      Log.d(getClass().getSimpleName(), "items searched " + items.size());
    }
    cursor.close();

    return items;
  }

  /**
   * 
   * @param database
   * @return
   */
  public List<SearchItem> searchProviders(final SQLiteDatabase database)
  {
    // Initialisations
    final List<SearchItem> items = new ArrayList<SearchItem>();
    Log.d(getClass().getSimpleName(), "searchProviders() : " + SQL_TABLE_ITEM_SEARCH_PROVIDERS);

    final Cursor cursor = database.rawQuery(SQL_TABLE_ITEM_SEARCH_PROVIDERS, null);

    if (cursor.moveToFirst())
    {
      do
      {
        final SearchItem item = new SearchItem();
        items.add(item);

        int col = 0; // OID ininteressant
        item.type = cursor.getString(col++);
        item.provider = cursor.getString(col++);
      } while (cursor.moveToNext());
      Log.d(getClass().getSimpleName(), "items searched " + items.size());
    }
    cursor.close();

    return items;
  }
}
