package org.pedro.android.mobibalises_common.map;

import java.util.HashMap;
import java.util.Map;

import org.pedro.balises.Balise;
import org.pedro.spots.Spot;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * 
 * @author pedro.m
 */
public class ItemDatabaseHelper extends SQLiteOpenHelper
{
  public static final int     TYPE_BALISE                            = 1;
  public static final Integer TYPE_BALISE_INTEGER                    = Integer.valueOf(TYPE_BALISE);
  public static final String  TYPE_BALISE_STRING                     = Integer.toString(TYPE_BALISE, 10);
  public static final int     TYPE_SPOT                              = 2;
  public static final Integer TYPE_SPOT_INTEGER                      = Integer.valueOf(TYPE_SPOT);
  public static final String  TYPE_SPOT_STRING                       = Integer.toString(TYPE_SPOT, 10);

  private static final String TABLE_ITEM                             = "item";
  private static final String COL_ITEM_TYPE                          = "type";
  private static final String COL_ITEM_PROVIDER                      = "provider";
  private static final String COL_ITEM_ID                            = "id";
  private static final String COL_ITEM_LATITUDE                      = "lat";
  private static final String COL_ITEM_LONGITUDE                     = "lng";
  private static final String SQL_TABLE_ITEM_CREATE                  = "CREATE TABLE " + TABLE_ITEM + " (" + COL_ITEM_TYPE + " INTEGER NOT NULL, " + COL_ITEM_PROVIDER + " TEXT NOT NULL, " + COL_ITEM_ID + " TEXT NOT NULL, "
                                                                         + COL_ITEM_LATITUDE + " REAL NOT NULL, " + COL_ITEM_LONGITUDE + " REAL NOT NULL, PRIMARY KEY (" + COL_ITEM_PROVIDER + ", " + COL_ITEM_ID + "))";
  private static final String SQL_TABLE_ITEM_DROP                    = "DROP TABLE IF EXISTS " + TABLE_ITEM;
  private static final String SQL_INDEX_ITEM_TYPE_DROP               = "DROP INDEX IF EXISTS item_type_idx";
  private static final String SQL_INDEX_ITEM_TYPE_CREATE             = "CREATE INDEX item_type_idx ON " + TABLE_ITEM + "(" + COL_ITEM_TYPE + ")";
  private static final String SQL_INDEX_ITEM_LATITUDE_DROP           = "DROP INDEX IF EXISTS item_latitude_idx";
  private static final String SQL_INDEX_ITEM_LATITUDE_CREATE         = "CREATE INDEX item_latitude_idx ON " + TABLE_ITEM + "(" + COL_ITEM_LATITUDE + ")";
  private static final String SQL_INDEX_ITEM_LONGITUDE_DROP          = "DROP INDEX IF EXISTS item_longitude_idx";
  private static final String SQL_INDEX_ITEM_LONGITUDE_CREATE        = "CREATE INDEX item_longitude_idx ON " + TABLE_ITEM + "(" + COL_ITEM_LONGITUDE + ")";
  private static final String SQL_TABLE_ITEM_SELECT_TYPE_LAT_LNG_BOX = "SELECT * FROM " + TABLE_ITEM + " WHERE " + COL_ITEM_TYPE + " = ? AND " + COL_ITEM_LATITUDE + " >= ? AND " + COL_ITEM_LATITUDE + " <= ? AND " + COL_ITEM_LONGITUDE
                                                                         + " >= ? AND " + COL_ITEM_LONGITUDE + " <= ?";
  private static final String SQL_TABLE_ITEM_DELETE_TYPE_PROVIDER    = "DELETE FROM " + TABLE_ITEM + " WHERE " + COL_ITEM_TYPE + " = ? AND " + COL_ITEM_PROVIDER + " = ?";

  /**
   * 
   * @author pedro.m
   */
  public static class ItemRow
  {
    public int    type;
    public String provider;
    public String id;
    public float  latitude;
    public float  longitude;

    @Override
    public String toString()
    {
      return type + "/" + provider + "/" + id;
    }
  }

  /**
   * 
   * @param context
   * @throws NameNotFoundException
   */
  private ItemDatabaseHelper(final Context context) throws NameNotFoundException
  {
    super(context, null, null, context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode);
  }

  /**
   * 
   * @param context
   * @return
   */
  public static ItemDatabaseHelper newInstance(final Context context)
  {
    try
    {
      return new ItemDatabaseHelper(context);
    }
    catch (final NameNotFoundException nnfe)
    {
      Log.e(ItemDatabaseHelper.class.getSimpleName(), nnfe.getMessage(), nnfe);
      throw new RuntimeException(nnfe);
    }
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
  public static void clean(final SQLiteDatabase database)
  {
    Log.d(ItemDatabaseHelper.class.getSimpleName(), ">>> clean()");

    // Tables
    database.execSQL(SQL_TABLE_ITEM_DROP);
    database.execSQL(SQL_TABLE_ITEM_CREATE);

    // Index
    database.execSQL(SQL_INDEX_ITEM_TYPE_DROP);
    database.execSQL(SQL_INDEX_ITEM_TYPE_CREATE);
    database.execSQL(SQL_INDEX_ITEM_LATITUDE_DROP);
    database.execSQL(SQL_INDEX_ITEM_LATITUDE_CREATE);
    database.execSQL(SQL_INDEX_ITEM_LONGITUDE_DROP);
    database.execSQL(SQL_INDEX_ITEM_LONGITUDE_CREATE);

    Log.d(ItemDatabaseHelper.class.getSimpleName(), "<<< clean()");
  }

  /**
   * 
   * @param database
   * @param providers
   * @param latMin
   * @param latMax
   * @param lngMin
   * @param lngMax
   * @return
   */
  public static Map<String, ItemRow> selectItems(final SQLiteDatabase database, final int type, final float latMin, final float latMax, final float lngMin, final float lngMax)
  {
    // Initialisations
    Log.d(ItemDatabaseHelper.class.getSimpleName(), "selectItems(" + type + ", " + latMin + ", " + latMax + ", " + lngMin + ", " + lngMax + ")");
    final Map<String, ItemRow> items = new HashMap<String, ItemRow>();

    final String req = SQL_TABLE_ITEM_SELECT_TYPE_LAT_LNG_BOX;
    Log.d(ItemDatabaseHelper.class.getSimpleName(), "req : " + req);

    // Type et Lat/Lng
    final String[] args = new String[5];
    args[0] = Integer.toString(type, 10);
    args[1] = Float.toString(latMin);
    args[2] = Float.toString(latMax);
    args[3] = Float.toString(lngMin);
    args[4] = Float.toString(lngMax);

    final Cursor cursor = database.rawQuery(req, args);
    if (cursor.moveToFirst())
    {
      do
      {
        final ItemRow row = new ItemRow();
        parseItemRow(cursor, row);
        items.put(row.provider + "." + row.id, row);
      } while (cursor.moveToNext());
      Log.d(ItemDatabaseHelper.class.getSimpleName(), "items selected : " + items.size());
    }
    else
    {
      Log.d(ItemDatabaseHelper.class.getSimpleName(), "no item selected");
    }
    cursor.close();

    return items;
  }

  /**
   * 
   * @param cursor
   * @param row
   */
  private static void parseItemRow(final Cursor cursor, final ItemRow row)
  {
    int col = -1;
    row.type = cursor.getInt(++col);
    row.provider = cursor.getString(++col);
    row.id = cursor.getString(++col);
    row.latitude = cursor.getFloat(++col);
    row.longitude = cursor.getFloat(++col);
  }

  /**
   * 
   * @param database
   * @param providerKey
   * @param balise
   */
  public static void replaceBalise(final SQLiteDatabase database, final String providerKey, final Balise balise)
  {
    final ContentValues itemValues = new ContentValues();
    itemValues.put(COL_ITEM_TYPE, TYPE_BALISE_INTEGER);
    itemValues.put(COL_ITEM_PROVIDER, providerKey);
    itemValues.put(COL_ITEM_ID, balise.id);
    itemValues.put(COL_ITEM_LATITUDE, Double.valueOf(balise.latitude));
    itemValues.put(COL_ITEM_LONGITUDE, Double.valueOf(balise.longitude));

    database.replace(TABLE_ITEM, null, itemValues);
  }

  /**
   * 
   * @param database
   * @param providerKey
   */
  public static void deleteBalises(final SQLiteDatabase database, final String providerKey)
  {
    database.execSQL(SQL_TABLE_ITEM_DELETE_TYPE_PROVIDER, new Object[] { TYPE_BALISE_INTEGER, providerKey });
  }

  /**
   * 
   * @param database
   * @param providerKey
   * @param spot
   */
  public static void replaceSpot(final SQLiteDatabase database, final String providerKey, final Spot spot)
  {
    final ContentValues itemValues = new ContentValues();
    itemValues.put(COL_ITEM_TYPE, TYPE_SPOT_INTEGER);
    itemValues.put(COL_ITEM_PROVIDER, providerKey);
    itemValues.put(COL_ITEM_ID, spot.id);
    itemValues.put(COL_ITEM_LATITUDE, spot.latitude);
    itemValues.put(COL_ITEM_LONGITUDE, spot.longitude);

    database.replace(TABLE_ITEM, null, itemValues);
  }

  /**
   * 
   * @param database
   * @param providerKey
   */
  public static void deleteSpots(final SQLiteDatabase database, final String providerKey)
  {
    database.execSQL(SQL_TABLE_ITEM_DELETE_TYPE_PROVIDER, new Object[] { TYPE_SPOT_INTEGER, providerKey });
  }
}
