package org.pedro.android.mobibalises_common.webcam;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.Strings;
import org.pedro.android.mobibalises_common.search.SearchDatabaseHelper;
import org.pedro.android.mobibalises_common.search.SearchDatabaseHelper.SearchItem;
import org.pedro.webcams.Webcam;

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
public class WebcamDatabaseHelper extends SQLiteOpenHelper
{
  private static final String BASE_DATABASE_NAME                           = "MobiBalisesWebcams.db";
  public static final String  DATABASE_NAME                                = ActivityCommons.MOBIBALISES_EXTERNAL_STORAGE_PATH.getAbsolutePath() + "/webcams/" + BASE_DATABASE_NAME;

  private static final String TABLE_PROVIDER                               = "provider";
  private static final String COL_PROVIDER_KEY                             = "key";
  private static final String COL_PROVIDER_UPDATE_TS                       = "update_ts";
  private static final String COL_PROVIDER_CHECK_TS                        = "check_ts";
  private static final String SQL_TABLE_PROVIDER_CREATE                    = "CREATE TABLE " + TABLE_PROVIDER + " (" + COL_PROVIDER_KEY + " TEXT NOT NULL, " + COL_PROVIDER_UPDATE_TS + " INTEGER, " + COL_PROVIDER_CHECK_TS
                                                                               + " INTEGER NOT NULL, PRIMARY KEY (" + COL_PROVIDER_KEY + "))";
  private static final String SQL_TABLE_PROVIDER_DROP                      = "DROP TABLE IF EXISTS " + TABLE_PROVIDER;
  private static final String SQL_TABLE_PROVIDER_SELECT                    = "SELECT * FROM " + TABLE_PROVIDER + " WHERE " + COL_PROVIDER_KEY + " = ?";
  private static final String SQL_TABLE_PROVIDER_COUNT                     = "SELECT count(*) FROM " + TABLE_PROVIDER;

  private static final String TABLE_WEBCAM                                 = "webcam";
  public static final String  COL_WEBCAM_PROVIDER                          = "provider";
  public static final String  COL_WEBCAM_ID                                = "id";
  public static final String  COL_WEBCAM_NOM                               = "nom";
  public static final String  COL_WEBCAM_NOM_NORM                          = "nom_norm";
  public static final String  COL_WEBCAM_PAYS                              = "pays";
  public static final String  COL_WEBCAM_PRATIQUES                         = "pratiques";
  public static final String  COL_WEBCAM_LATITUDE                          = "latitude";
  public static final String  COL_WEBCAM_LONGITUDE                         = "longitude";
  public static final String  COL_WEBCAM_ALTITUDE                          = "altitude";
  public static final String  COL_WEBCAM_DIRECTION                         = "direction";
  public static final String  COL_WEBCAM_CHAMP                             = "champ";
  public static final String  COL_WEBCAM_URL_IMAGE                         = "url_image";
  public static final String  COL_WEBCAM_PERIODICITE                       = "periodicite";
  public static final String  COL_WEBCAM_DECALAGE_PERIODICITE              = "decalage_periodicite";
  public static final String  COL_WEBCAM_DECALAGE_HORLOGE                  = "decalage_horloge";
  public static final String  COL_WEBCAM_FUSEAU_HORAIRE                    = "fuseau_horaire";
  public static final String  COL_WEBCAM_CODE_LOCALE                       = "code_locale";
  public static final String  COL_WEBCAM_LARGEUR                           = "largeur";
  public static final String  COL_WEBCAM_HAUTEUR                           = "hauteur";
  public static final String  COL_WEBCAM_URL_PAGE                          = "url_page";
  public static final String  COL_WEBCAM_DESCRIPTION                       = "description";
  public static final String  COL_WEBCAM_STATUT_ENLIGNE                    = "statut_enligne";
  public static final String  COL_WEBCAM_STATUT_VARIABLE                   = "statut_variable";
  private static final String SQL_TABLE_WEBCAM_CREATE                      = "CREATE TABLE " + TABLE_WEBCAM + " (" + COL_WEBCAM_PROVIDER + " TEXT NOT NULL, " + COL_WEBCAM_ID + " TEXT NOT NULL, " + COL_WEBCAM_NOM + " TEXT NOT NULL, "
                                                                               + COL_WEBCAM_NOM_NORM + " TEXT NOT NULL, " + COL_WEBCAM_PAYS + " TEXT NOT NULL, " + COL_WEBCAM_PRATIQUES + " INTEGER NOT NULL, " + COL_WEBCAM_LATITUDE
                                                                               + " REAL NOT NULL, " + COL_WEBCAM_LONGITUDE + " REAL NOT NULL, " + COL_WEBCAM_ALTITUDE + " INTEGER, " + COL_WEBCAM_DIRECTION + " INTEGER NOT NULL, "
                                                                               + COL_WEBCAM_CHAMP + " INTEGER NOT NULL, " + COL_WEBCAM_URL_IMAGE + " TEXT, " + COL_WEBCAM_PERIODICITE + " INTEGER, " + COL_WEBCAM_DECALAGE_PERIODICITE
                                                                               + " INTEGER, " + COL_WEBCAM_DECALAGE_HORLOGE + " INTEGER, " + COL_WEBCAM_FUSEAU_HORAIRE + " TEXT, " + COL_WEBCAM_CODE_LOCALE + " TEXT, " + COL_WEBCAM_LARGEUR
                                                                               + " INTEGER, " + COL_WEBCAM_HAUTEUR + " INTEGER, " + COL_WEBCAM_URL_PAGE + " TEXT, " + COL_WEBCAM_DESCRIPTION + " TEXT, " + COL_WEBCAM_STATUT_ENLIGNE
                                                                               + " TEXT NOT NULL, " + COL_WEBCAM_STATUT_VARIABLE + " TEXT NOT NULL, PRIMARY KEY (" + COL_WEBCAM_PROVIDER + ", " + COL_WEBCAM_ID + "))";
  private static final String SQL_TABLE_WEBCAM_DROP                        = "DROP TABLE IF EXISTS " + TABLE_WEBCAM;
  private static final String SQL_INDEX_WEBCAM_PROVIDER_DROP               = "DROP INDEX IF EXISTS webcam_provider_idx";
  private static final String SQL_INDEX_WEBCAM_PROVIDER_CREATE             = "CREATE INDEX webcam_provider_idx ON " + TABLE_WEBCAM + "(" + COL_WEBCAM_PROVIDER + ")";
  private static final String SQL_INDEX_WEBCAM_PAYS_DROP                   = "DROP INDEX IF EXISTS webcam_pays_idx";
  private static final String SQL_INDEX_WEBCAM_PAYS_CREATE                 = "CREATE INDEX webcam_pays_idx ON " + TABLE_WEBCAM + "(" + COL_WEBCAM_PAYS + ")";
  private static final String SQL_INDEX_WEBCAM_LATITUDE_DROP               = "DROP INDEX IF EXISTS webcam_latitude_idx";
  private static final String SQL_INDEX_WEBCAM_LATITUDE_CREATE             = "CREATE INDEX webcam_latitude_idx ON " + TABLE_WEBCAM + "(" + COL_WEBCAM_LATITUDE + ")";
  private static final String SQL_INDEX_WEBCAM_LONGITUDE_DROP              = "DROP INDEX IF EXISTS webcam_longitude_idx";
  private static final String SQL_INDEX_WEBCAM_LONGITUDE_CREATE            = "CREATE INDEX webcam_longitude_idx ON " + TABLE_WEBCAM + "(" + COL_WEBCAM_LONGITUDE + ")";

  private static final String SQL_TABLE_WEBCAM_SELECT_LAT_LNG_BOX          = "SELECT * FROM " + TABLE_WEBCAM + " WHERE {0}" + COL_WEBCAM_LATITUDE + " >= ? AND " + COL_WEBCAM_LATITUDE + " <= ? AND " + COL_WEBCAM_LONGITUDE + " >= ? AND "
                                                                               + COL_WEBCAM_LONGITUDE + " <= ?";
  private static final String SQL_TABLE_WEBCAM_SELECT_ID_FROM_PROVIDER_ID  = "SELECT " + COL_WEBCAM_ID + " FROM " + TABLE_WEBCAM + " WHERE " + COL_WEBCAM_PROVIDER + " = ? AND " + COL_WEBCAM_ID + " = ?";
  private static final String SQL_TABLE_WEBCAM_SEARCH                      = "SELECT " + COL_WEBCAM_NOM + ", " + COL_WEBCAM_PROVIDER + ", " + COL_WEBCAM_ID + ", " + COL_WEBCAM_LATITUDE + ", " + COL_WEBCAM_LONGITUDE + " FROM "
                                                                               + TABLE_WEBCAM + " WHERE " + COL_WEBCAM_NOM_NORM + " LIKE ? ORDER BY " + COL_WEBCAM_PROVIDER + ", " + COL_WEBCAM_NOM;
  private static final String SQL_TABLE_WEBCAM_SELECT_ALL_FROM_PROVIDER_ID = "SELECT * FROM " + TABLE_WEBCAM + " WHERE " + COL_WEBCAM_PROVIDER + " = ? AND " + COL_WEBCAM_ID + " = ?";

  /**
   * 
   * @author pedro.m
   */
  public static class WebcamRow
  {
    public String  provider;
    public String  id;
    public String  nom;
    public String  nomNorm;
    public String  pays;
    public int     pratiques;
    public float   latitude;
    public float   longitude;
    public int     altitude = Integer.MIN_VALUE;
    public int     direction;
    public int     champ;
    public String  urlImage;
    public Integer periodicite;
    public Integer decalagePeriodicite;
    public Integer decalageHorloge;
    public String  fuseauHoraire;
    public String  codeLocale;
    public int     largeur  = Integer.MIN_VALUE;
    public int     hauteur  = Integer.MIN_VALUE;
    public String  urlPage;
    public String  description;
    public String  statutEnLigne;
    public String  statutVariable;

    @Override
    public String toString()
    {
      return provider + "/" + id + "/" + nom;
    }
  }

  /**
   * 
   * @param context
   * @throws NameNotFoundException
   */
  private WebcamDatabaseHelper(final Context context) throws NameNotFoundException
  {
    super(context, BASE_DATABASE_NAME, null, context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode);
  }

  /**
   * 
   * @param context
   * @return
   */
  public static WebcamDatabaseHelper newInstance(final Context context)
  {
    try
    {
      return new WebcamDatabaseHelper(context);
    }
    catch (final NameNotFoundException nnfe)
    {
      Log.e(WebcamDatabaseHelper.class.getSimpleName(), nnfe.getMessage(), nnfe);
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
    Log.d(WebcamDatabaseHelper.class.getSimpleName(), ">>> clean()");

    // Tables
    database.execSQL(SQL_TABLE_PROVIDER_DROP);
    database.execSQL(SQL_TABLE_PROVIDER_CREATE);
    database.execSQL(SQL_TABLE_WEBCAM_DROP);
    database.execSQL(SQL_TABLE_WEBCAM_CREATE);

    // Index
    database.execSQL(SQL_INDEX_WEBCAM_PROVIDER_DROP);
    database.execSQL(SQL_INDEX_WEBCAM_PROVIDER_CREATE);
    database.execSQL(SQL_INDEX_WEBCAM_PAYS_DROP);
    database.execSQL(SQL_INDEX_WEBCAM_PAYS_CREATE);
    database.execSQL(SQL_INDEX_WEBCAM_LATITUDE_DROP);
    database.execSQL(SQL_INDEX_WEBCAM_LATITUDE_CREATE);
    database.execSQL(SQL_INDEX_WEBCAM_LONGITUDE_DROP);
    database.execSQL(SQL_INDEX_WEBCAM_LONGITUDE_CREATE);

    Log.d(WebcamDatabaseHelper.class.getSimpleName(), "<<< clean()");
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
  public static Map<String, WebcamRow> selectWebcams(final SQLiteDatabase database, final List<String> providers, final float latMin, final float latMax, final float lngMin, final float lngMax)
  {
    // Initialisations
    Log.d(WebcamDatabaseHelper.class.getSimpleName(), "selectWebcams(" + providers + ", " + latMin + ", " + latMax + ", " + lngMin + ", " + lngMax + ")");
    final Map<String, WebcamRow> webcams = new HashMap<String, WebcamRow>();
    final int nbProviders = (providers == null ? 0 : providers.size());
    final String[] args = new String[nbProviders + 4];

    // Providers
    final String providersClause;
    if ((providers != null) && (nbProviders == 1))
    {
      providersClause = COL_WEBCAM_PROVIDER + " = ? AND ";
      args[0] = providers.get(0);
    }
    else if ((providers != null) && (nbProviders > 1))
    {
      String theClause = COL_WEBCAM_PROVIDER + " IN (";
      String sep = "";
      int i = 0;
      for (final String provider : providers)
      {
        theClause += sep + "?";
        args[i] = provider;
        sep = ", ";

        // Next
        i++;
      }
      theClause += ") AND ";
      providersClause = theClause;
    }
    else
    {
      providersClause = Strings.VIDE;
    }
    final String req = MessageFormat.format(SQL_TABLE_WEBCAM_SELECT_LAT_LNG_BOX, providersClause);
    Log.d(WebcamDatabaseHelper.class.getSimpleName(), "req : " + req);

    // Lat/Lng
    args[nbProviders] = Float.toString(latMin);
    args[nbProviders + 1] = Float.toString(latMax);
    args[nbProviders + 2] = Float.toString(lngMin);
    args[nbProviders + 3] = Float.toString(lngMax);

    final Cursor cursor = database.rawQuery(req, args);
    if (cursor.moveToFirst())
    {
      do
      {
        final WebcamRow row = new WebcamRow();
        parseWebcamRow(cursor, row);
        webcams.put(row.provider + "." + row.id, row);
      } while (cursor.moveToNext());
      Log.d(WebcamDatabaseHelper.class.getSimpleName(), "webcams selected : " + webcams.size());
    }
    else
    {
      Log.d(WebcamDatabaseHelper.class.getSimpleName(), "no webcam selected");
    }
    cursor.close();

    return webcams;
  }

  /**
   * 
   * @param cursor
   * @param row
   */
  private static void parseWebcamRow(final Cursor cursor, final WebcamRow row)
  {
    int col = -1;
    row.provider = cursor.getString(++col);
    row.id = cursor.getString(++col);
    row.nom = cursor.getString(++col);
    row.nomNorm = cursor.getString(++col);
    row.pays = cursor.getString(++col);
    row.pratiques = cursor.getInt(++col);
    row.latitude = cursor.getFloat(++col);
    row.longitude = cursor.getFloat(++col);
    if (!cursor.isNull(++col))
    {
      row.altitude = cursor.getInt(col);
    }
    row.direction = cursor.getInt(++col);
    row.champ = cursor.getInt(++col);
    if (!cursor.isNull(++col))
    {
      row.urlImage = cursor.getString(col);
    }
    if (!cursor.isNull(++col))
    {
      row.periodicite = Integer.valueOf(cursor.getInt(col));
    }
    if (!cursor.isNull(++col))
    {
      row.decalagePeriodicite = Integer.valueOf(cursor.getInt(col));
    }
    if (!cursor.isNull(++col))
    {
      row.decalageHorloge = Integer.valueOf(cursor.getInt(col));
    }
    if (!cursor.isNull(++col))
    {
      row.fuseauHoraire = cursor.getString(col);
    }
    if (!cursor.isNull(++col))
    {
      row.codeLocale = cursor.getString(col);
    }
    if (!cursor.isNull(++col))
    {
      row.largeur = cursor.getInt(col);
    }
    if (!cursor.isNull(++col))
    {
      row.hauteur = cursor.getInt(col);
    }
    if (!cursor.isNull(++col))
    {
      row.urlPage = cursor.getString(col);
    }
    if (!cursor.isNull(++col))
    {
      row.description = cursor.getString(col);
    }
    row.statutEnLigne = cursor.getString(++col);
    row.statutVariable = cursor.getString(++col);
  }

  /**
   * 
   * @param database
   * @param key
   * @return
   */
  public static long[] getProviderLastCheckAndUpdate(final SQLiteDatabase database, final String key)
  {
    final String[] args = { key };
    final Cursor cursor = database.rawQuery(SQL_TABLE_PROVIDER_SELECT, args);
    try
    {
      if (cursor.moveToFirst())
      {
        return new long[] { cursor.getLong(2), cursor.getLong(1) };
      }

      return null;
    }
    finally
    {
      cursor.close();
    }
  }

  /**
   * 
   * @param database
   * @return
   */
  public static int getNbProvidersUpdated(final SQLiteDatabase database)
  {
    final String[] args = {};
    final Cursor cursor = database.rawQuery(SQL_TABLE_PROVIDER_COUNT, args);
    try
    {
      if (cursor.moveToFirst())
      {
        return cursor.getInt(0);
      }

      return -1;
    }
    finally
    {
      cursor.close();
    }
  }

  /**
   * 
   * @param database
   * @param key
   * @param ts
   */
  public static void updateProviderLastUpdate(final SQLiteDatabase database, final String key, final long ts)
  {
    final String[] args = new String[] { key };
    final Cursor cursor = database.rawQuery(SQL_TABLE_PROVIDER_SELECT, args);
    final boolean exists = cursor.moveToFirst();
    cursor.close();

    final ContentValues itemValues = new ContentValues();
    itemValues.put(COL_PROVIDER_UPDATE_TS, Long.valueOf(ts));

    // Update
    if (exists)
    {
      database.update(TABLE_PROVIDER, itemValues, COL_PROVIDER_KEY + " = ?", args);
    }
    // Insert
    else
    {
      itemValues.put(COL_PROVIDER_KEY, key);
      database.insert(TABLE_PROVIDER, null, itemValues);
    }
  }

  /**
   * 
   * @param database
   * @param key
   * @param ts
   */
  public static void updateProviderLastCheck(final SQLiteDatabase database, final String key, final long ts)
  {
    final String[] args = new String[] { key };
    final Cursor cursor = database.rawQuery(SQL_TABLE_PROVIDER_SELECT, args);
    final boolean exists = cursor.moveToFirst();
    cursor.close();

    final ContentValues itemValues = new ContentValues();
    itemValues.put(COL_PROVIDER_CHECK_TS, Long.valueOf(ts));

    // Update
    if (exists)
    {
      database.update(TABLE_PROVIDER, itemValues, COL_PROVIDER_KEY + " = ?", args);
    }
    // Insert
    else
    {
      itemValues.put(COL_PROVIDER_KEY, key);
      database.insert(TABLE_PROVIDER, null, itemValues);
    }
  }

  /**
   * 
   * @param database
   * @param key
   * @param webcam
   */
  public static void deleteWebcam(final SQLiteDatabase database, final String key, final Webcam webcam)
  {
    Log.d(WebcamDatabaseHelper.class.getSimpleName(), "deleting webcam " + key + "/" + webcam.id + "/" + webcam.etat);
    database.delete(TABLE_WEBCAM, COL_WEBCAM_PROVIDER + " = ? AND " + COL_WEBCAM_ID + " = ?", new String[] { key, Integer.toString(webcam.id, 10) });
  }

  /**
   * 
   * @param database
   * @param key
   * @param webcam
   */
  public static void updateWebcam(final SQLiteDatabase database, final String key, final Webcam webcam)
  {
    final String[] args = new String[] { key, Integer.toString(webcam.id, 10) };
    final Cursor cursor = database.rawQuery(SQL_TABLE_WEBCAM_SELECT_ID_FROM_PROVIDER_ID, args);
    final boolean exists = cursor.moveToFirst();
    cursor.close();

    final ContentValues itemValues = new ContentValues();
    itemValues.put(COL_WEBCAM_NOM, webcam.nom);
    itemValues.put(COL_WEBCAM_NOM_NORM, Strings.removeAccents(webcam.nom));
    itemValues.put(COL_WEBCAM_PAYS, webcam.pays);
    itemValues.put(COL_WEBCAM_PRATIQUES, Integer.valueOf(webcam.pratiques));
    itemValues.put(COL_WEBCAM_LATITUDE, Float.valueOf(webcam.latitude));
    itemValues.put(COL_WEBCAM_LONGITUDE, Float.valueOf(webcam.longitude));
    itemValues.put(COL_WEBCAM_ALTITUDE, webcam.altitude);
    itemValues.put(COL_WEBCAM_DIRECTION, Integer.valueOf(webcam.direction));
    itemValues.put(COL_WEBCAM_CHAMP, Integer.valueOf(webcam.champ));
    itemValues.put(COL_WEBCAM_URL_IMAGE, webcam.urlImage);
    itemValues.put(COL_WEBCAM_PERIODICITE, webcam.periodicite);
    itemValues.put(COL_WEBCAM_DECALAGE_PERIODICITE, webcam.decalagePeriodicite);
    itemValues.put(COL_WEBCAM_DECALAGE_HORLOGE, webcam.decalageHorloge);
    itemValues.put(COL_WEBCAM_FUSEAU_HORAIRE, webcam.fuseauHoraire);
    itemValues.put(COL_WEBCAM_CODE_LOCALE, webcam.codeLocale);
    itemValues.put(COL_WEBCAM_LARGEUR, webcam.largeur);
    itemValues.put(COL_WEBCAM_HAUTEUR, webcam.hauteur);
    itemValues.put(COL_WEBCAM_URL_PAGE, webcam.urlPage);
    itemValues.put(COL_WEBCAM_DESCRIPTION, webcam.description);
    itemValues.put(COL_WEBCAM_STATUT_ENLIGNE, webcam.statutEnLigne);
    itemValues.put(COL_WEBCAM_STATUT_VARIABLE, webcam.statutVariable);

    // Update
    if (exists)
    {
      final int retour = database.update(TABLE_WEBCAM, itemValues, COL_WEBCAM_PROVIDER + " = ? AND " + COL_WEBCAM_ID + " = ?", args);
      Log.d(WebcamDatabaseHelper.class.getSimpleName(), "updating webcam " + key + "/" + webcam.id + "/" + webcam.etat + " => " + retour);
    }
    // Insert
    else
    {
      itemValues.put(COL_WEBCAM_PROVIDER, key);
      itemValues.put(COL_WEBCAM_ID, Integer.valueOf(webcam.id));
      final long retour = database.insert(TABLE_WEBCAM, null, itemValues);
      Log.d(WebcamDatabaseHelper.class.getSimpleName(), "inserting webcam " + key + "/" + webcam.id + "/" + webcam.etat + " => " + retour);
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
    Log.d(getClass().getSimpleName(), "searchItems(" + search + ":" + args[0] + ") : " + SQL_TABLE_WEBCAM_SEARCH);

    final Cursor cursor = database.rawQuery(SQL_TABLE_WEBCAM_SEARCH, args);

    if (cursor.moveToFirst())
    {
      do
      {
        final SearchItem item = new SearchItem();
        items.add(item);

        int col = 0;
        item.type = SearchDatabaseHelper.ITEM_TYPE_WEBCAM;
        item.subtype = null;
        item.name = cursor.getString(col++);
        item.provider = cursor.getString(col++);
        item.id = cursor.getString(col++);
        item.subId = null;
        item.latitude = cursor.getDouble(col++);
        item.longitude = cursor.getDouble(col++);
      } while (cursor.moveToNext());
      Log.d(getClass().getSimpleName(), "webcams items searched " + items.size());
    }
    cursor.close();

    return items;
  }

  /**
   * 
   * @param database
   * @param provider
   * @param id
   * @return
   */
  public WebcamRow selectWebcam(final SQLiteDatabase database, final String provider, final String id)
  {
    // Initialisations
    WebcamRow row = null;
    final String[] args = new String[] { provider, id };
    Log.d(getClass().getSimpleName(), "selectWebcam(" + provider + ", " + id + ") : " + SQL_TABLE_WEBCAM_SELECT_ALL_FROM_PROVIDER_ID);

    final Cursor cursor = database.rawQuery(SQL_TABLE_WEBCAM_SELECT_ALL_FROM_PROVIDER_ID, args);

    if (cursor.moveToFirst())
    {
      row = new WebcamRow();
      parseWebcamRow(cursor, row);
      Log.d(getClass().getSimpleName(), "webcam item selected " + row);
    }
    cursor.close();

    return row;
  }
}
