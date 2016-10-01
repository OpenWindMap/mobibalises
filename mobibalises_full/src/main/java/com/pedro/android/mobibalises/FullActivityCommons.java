package com.pedro.android.mobibalises;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.Strings;
import org.pedro.android.mobibalises_common.map.AbstractBalisesMapActivity;
import org.pedro.android.mobibalises_common.provider.BaliseProviderUtils;
import org.pedro.android.mobibalises_common.provider.BaliseProviderUtils.TendanceVent;
import org.pedro.android.mobibalises_common.service.AbstractProvidersService;
import org.pedro.android.mobibalises_lgpl.alarm.BaliseAlarm;
import org.pedro.balises.Balise;
import org.pedro.balises.BaliseProvider;
import org.pedro.balises.CachedProvider;
import org.pedro.balises.HistoryBaliseProvider;
import org.pedro.balises.Releve;
import org.pedro.balises.Utils;
import org.pedro.utils.AESTool;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings.Secure;
import android.support.v4.app.NotificationCompat;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.ServerManagedPolicy;
import com.pedro.android.mobibalises.alarm.AlarmUtils;
import com.pedro.android.mobibalises.favorites.AbstractBalisesFavoritesAdapter;
import com.pedro.android.mobibalises.favorites.AbstractFavoritesLocationListener;
import com.pedro.android.mobibalises.favorites.BaliseFavorite;
import com.pedro.android.mobibalises.favorites.FavoritesActivity;
import com.pedro.android.mobibalises.history.HistoryActivity;
import com.pedro.android.mobibalises.service.IFullProvidersService;
import com.pedro.android.mobibalises.service.MobibalisesLicenseChecker;
import com.pedro.android.mobibalises.service.ProvidersService;
import com.pedro.android.mobibalises.start.BalisesStartActivity;
import com.pedro.android.mobibalises.widget.BalisesWidgets;

/**
 * 
 * @author pedro.m
 */
public abstract class FullActivityCommons
{
  private static final String  NEW_LABEL_SUFFIX                   = ".1";
  private static final String  PREFERENCE_LICENSE_CHECK           = "mobibalises_config_111_";
  private static final long    DELTA_LICENSE_CHECK                = 7 * 24 * 3600 * 1000;

  private static final String  BASE64_PUBLIC_KEY                  = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5vR/B2ofqt7GQWpC4VFS56iI0FYKo2TWL2rqDdWEuAEBBa6GM6cp4SJGSyg8dKHfD5VMkQqZbrfs6pQW2dJSfxQmFABrvjaffUkPl6Bk11koovIx1mUyICWNpqpK9AMXOIgO8vIbM0VwK88ar6XayPNqL2XarSxm4LImVfrrbsW+E+qhmC2Yg8l+5tMuA5j6/YbQzetz5BrLkbbg+n2P3hPUrdYYIxPLOWjh2KN68e4OrBeEw7kz9sQGVCcIkPuyfC4S+CHQ4pwSEetVMb4awsSjbvMK7Nvab+T1MauyFVOf6RRLiyrEVzqxzFEmzPAeZkIAKbKbyTFaBU6N0TkvbQIDAQAB";
  private static final byte[]  SALT                               = new byte[] { 67, 34, -87, 123, 90, -2, 38, -40, 13, 7, 20, 12, -20, -48, 22, 10, -71, 3, 4, 74 };

  private static final String  STRING_PREFIXE_FAVORITE_LIST_NAME  = " (";
  private static final String  STRING_SUFFIXE_FAVORITE_LIST_NAME  = ")";

  private static final String  STRING_TENDANCE_VENT_FAIBLE_HAUSSE = "<big><font color=\"green\">\u25B2</font></big>";
  private static final String  STRING_TENDANCE_VENT_FORTE_HAUSSE  = "<big><font color=\"red\">\u25B2</font></big>";
  private static final String  STRING_TENDANCE_VENT_FAIBLE_BAISSE = "<big><font color=\"green\">\u25BC</font></big>";
  private static final String  STRING_TENDANCE_VENT_FORTE_BAISSE  = "<big><font color=\"red\">\u25BC</font></big>";

  private static final int     MOBIBALISES_NOTIFICATION_ID        = 1;

  // Resources
  static Resources             resources;
  static SharedPreferences     sharedPreferences;
  static boolean               debugMode;

  // Initialisation
  private static final Object  initLock                           = new Object();
  private static boolean       initialized                        = false;

  // Gestion du dialogue de choix des balises favorites
  static final Object          favoritesChooserDialogLock         = new Object();
  static boolean               favoritesChooserDialogShowing      = false;

  // Gestion du dialogue de license
  private static Handler       unlicensedDialogHandler;

  // Notifications
  private static List<Integer> notifications                      = new ArrayList<Integer>();

  /**
   * 
   * @author pedro.m
   */
  public interface FavoritesLabelsChooserListener
  {
    /**
     * 
     * @param labels
     */
    public void onFavoritesLabelsChoosed(List<String> labels);
  }

  /**
   * 
   * @author pedro.m
   */
  public interface FavoritesChooserListener
  {
    /**
     * 
     */
    public void onFavoritesChoosed();
  }

  /**
   * 
   * @author pedro.m
   */
  public interface FavoriteLabelChooserListener
  {
    /**
     * 
     * @param label
     */
    public void onFavoriteLabelChoosed(final String label);

    /**
     * 
     */
    public void onProximityModeChoosed();
  }

  /**
   * 
   * @author pedro.m
   */
  private static class HistoryDownloadThread extends Thread
  {
    private final String                  activityName;
    private final String                  idBalise;
    private final HistoryBaliseProvider   provider;
    private final String                  providerKey;
    private final HistoryDownloadListener listener;

    /**
     * 
     * @author pedro.m
     */
    private interface HistoryDownloadListener
    {
      /**
       * 
       * @param releves
       */
      public void downloadComplete(final Collection<Releve> releves);

      /**
       * 
       * @param releves
       */
      public void downloadFailed(final IOException ioe);
    }

    /**
     * 
     * @param activityName
     * @param idBalise
     * @param providerKey
     * @param provider
     * @param listener
     */
    HistoryDownloadThread(final String activityName, final String idBalise, final String providerKey, final HistoryBaliseProvider provider, final HistoryDownloadListener listener)
    {
      super(HistoryDownloadThread.class.getName());
      this.activityName = activityName;
      this.idBalise = idBalise;
      this.providerKey = providerKey;
      this.provider = provider;
      this.listener = listener;
    }

    @Override
    public void run()
    {
      try
      {
        // Telechargement
        final int duree = sharedPreferences.getInt(resources.getString(R.string.config_history_mode_duration_key), Integer.parseInt(resources.getString(R.string.config_history_mode_duration_default), 10));
        final int peremption = sharedPreferences.getInt(resources.getString(R.string.config_map_outofdate_key), Integer.parseInt(resources.getString(R.string.config_map_outofdate_default), 10));
        final Collection<Releve> releves = provider.getHistoriqueBalise(idBalise, duree, peremption);

        // Verification donnees
        if ((releves == null) || (releves.size() == 0))
        {
          if (!isInterrupted())
          {
            listener.downloadFailed(null);
          }
          return;
        }

        // Fin
        if (!isInterrupted())
        {
          listener.downloadComplete(releves);
        }
      }
      catch (final IOException ioe)
      {
        Log.e(activityName, "Erreur au telechargement de l'historique pour la balise " + providerKey + "." + idBalise, ioe);
        if (!isInterrupted())
        {
          listener.downloadFailed(ioe);
        }
      }
    }
  }

  /**
   * 
   * @param context
   */
  public static void init(final Context context)
  {
    // Une seule fois
    synchronized (initLock)
    {
      if (initialized)
      {
        return;
      }

      // Initialisations
      resources = context.getResources();
      sharedPreferences = ActivityCommons.getSharedPreferences(context);

      debugMode = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) > 0;

      // Gestion du dialog de license
      initUnlicensedDialogHandler();

      // Fin
      initialized = true;
    }
  }

  /**
   * 
   * @param context
   * @return
   */
  public static boolean needsLicenseCheck(final Context context)
  {
    // Mode debug
    if (debugMode)
    {
      return false;
    }

    // Derniere verification
    final long lastCheck = sharedPreferences.getLong(PREFERENCE_LICENSE_CHECK + context.getResources().getString(R.string.app_version), -1);
    if (System.currentTimeMillis() <= lastCheck + DELTA_LICENSE_CHECK)
    {
      return false;
    }

    // Ami ?
    if (isFriend(context))
    {
      return false;
    }

    return true;
  }

  /**
   * 
   * @param context
   * @return
   */
  public static final boolean isFriend(final Context context)
  {
    // Verification de l'existence
    final File file = new File(ActivityCommons.MOBIBALISES_EXTERNAL_STORAGE_PATH, "mobibalises.license");
    if (!file.exists() || !file.isFile() || !file.canRead())
    {
      return false;
    }

    // Le fichier existe, lecture du contenu
    final StringBuilder buffer = new StringBuilder();
    FileReader fr = null;
    try
    {
      fr = new FileReader(file);
      int car = fr.read();
      while (car != -1)
      {
        // Ajout au buffer
        buffer.append((char)car);

        // Next
        car = fr.read();
      }
    }
    catch (final IOException ioe)
    {
      ioe.printStackTrace(System.err);
    }
    finally
    {
      try
      {
        if (fr != null)
        {
          fr.close();
        }
      }
      catch (final IOException ioe)
      {
        ioe.printStackTrace(System.err);
      }
    }

    // Fichier non correct ?
    final String line = buffer.toString();
    if (Utils.isStringVide(line))
    {
      return false;
    }

    // Recuperation id du telephone
    final String devId = ActivityCommons.getDeviceId(context);
    if (Utils.isStringVide(devId))
    {
      return false;
    }

    // Verification clef
    try
    {
      final String reference = "ma_reference_pour_mobibalises";
      final AESTool tool = new AESTool(reference);
      final String encrypted = tool.encrypt(devId);

      return encrypted.toString().equals(line);
    }
    catch (final Exception e)
    {
      Log.w(context.getClass().getSimpleName(), e);
      return false;
    }
  }

  /**
   * 
   * @param context
   * @param contextCallback
   * @return
   */
  public static MobibalisesLicenseChecker initLicenseChecker(final Context context, final LicenseCheckerCallback contextCallback)
  {
    // Initialisations
    final String deviceId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);

    // Le callback principal
    final LicenseCheckerCallback licenseCallback = new LicenseCheckerCallback()
    {
      @Override
      public void allow(final int reason)
      {
        // Log
        Log.i(context.getClass().getSimpleName(), "Licensing ok (allow) for " + context + " : " + reason);

        // Widgets
        BalisesWidgets.setLicensed(true);

        // Client
        if (contextCallback != null)
        {
          contextCallback.allow(reason);
        }

        // En cache
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(PREFERENCE_LICENSE_CHECK + context.getResources().getString(R.string.app_version), System.currentTimeMillis());
        ActivityCommons.commitPreferences(editor);
      }

      @Override
      public void dontAllow(final int reason)
      {
        // Log
        Log.i(context.getClass().getSimpleName(), "Licensing KO (dontAllow) for " + context + " : " + reason);

        // Widgets
        BalisesWidgets.setLicensed(false);

        // Client
        if (contextCallback != null)
        {
          contextCallback.dontAllow(reason);
        }
      }

      @Override
      public void applicationError(final int errorCode)
      {
        // Log
        Log.e(context.getClass().getSimpleName(), "Licensing error for " + context + " : " + errorCode);

        // Widgets
        BalisesWidgets.setLicensed(false);

        // Client
        if (contextCallback != null)
        {
          contextCallback.applicationError(errorCode);
        }
      }
    };

    // Check
    final MobibalisesLicenseChecker checker = new MobibalisesLicenseChecker(context, new ServerManagedPolicy(context, new AESObfuscator(SALT, context.getPackageName(), deviceId)), BASE64_PUBLIC_KEY, licenseCallback);

    return checker;
  }

  /**
   * 
   */
  private static void initUnlicensedDialogHandler()
  {
    unlicensedDialogHandler = new Handler(Looper.getMainLooper())
    {
      @Override
      public void handleMessage(final Message msg)
      {
        // Initialisations
        final Activity activity = (Activity)((Object[])msg.obj)[0];
        final Context context = activity.getApplicationContext();
        final MobibalisesLicenseChecker licenseChecker = (MobibalisesLicenseChecker)((Object[])msg.obj)[1];
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.app_name);
        builder.setIcon(R.drawable.icon);
        builder.setCancelable(true);

        // TextView personnalise
        final View view = LayoutInflater.from(context).inflate(R.layout.alert_dialog, null);
        final TextView textView = (TextView)view.findViewById(R.id.alert_dialog_text);
        final String deviceId = ActivityCommons.getDeviceId(context);
        textView.setAutoLinkMask(Linkify.WEB_URLS + Linkify.EMAIL_ADDRESSES);
        textView.setText(Html.fromHtml(Strings.toHtmlString(resources.getString(R.string.message_unlicensed, deviceId))));
        builder.setView(view);

        // Bouton Market
        builder.setPositiveButton(resources.getString(R.string.full_version_market_button), new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(final DialogInterface dialog, final int id)
          {
            ActivityCommons.goToMarket(activity);
            dialog.dismiss();
            activity.finish();
          }
        });

        // Bouton Retry, seulement si nb maxi d'essais non atteint
        if (licenseChecker.getTries() < 3)
        {
          builder.setNeutralButton(resources.getString(R.string.button_retry), new DialogInterface.OnClickListener()
          {
            @Override
            public void onClick(final DialogInterface dialog, final int id)
            {
              licenseChecker.checkAccess();
              dialog.dismiss();
            }
          });
        }

        // Cancel
        builder.setOnCancelListener(new OnCancelListener()
        {
          @Override
          public void onCancel(final DialogInterface arg0)
          {
            activity.finish();
          }
        });

        // Dialog
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        ActivityCommons.registerAlertDialog(ActivityCommons.ALERT_DIALOG_UNLICENSED, alertDialog, null);
      }
    };
  }

  /**
   * 
   * @param activity
   */
  public static final void unlicensedDialog(final Activity activity, final MobibalisesLicenseChecker checker)
  {
    final Message msg = new Message();
    msg.obj = new Object[] { activity, checker };
    unlicensedDialogHandler.sendMessage(msg);
  }

  /**
   * 
   * @param favorite
   * @return
   */
  static String getFavoriteListName(final BaliseFavorite favorite)
  {
    return favorite.getName().concat(STRING_PREFIXE_FAVORITE_LIST_NAME).concat(favorite.getProviderId().replace(Strings.CHAR_UNDERSCORE, Strings.CHAR_POINT)).concat(STRING_SUFFIXE_FAVORITE_LIST_NAME);
  }

  /**
   * 
   * @param providersService
   * @param activity
   * @param listener
   * @param label
   * @param locationProgressDialogId
   * @param waitProgressDialogId
   */
  public static void chooseFavoritesForLabel(final IFullProvidersService providersService, final FavoritesActivity activity, final FavoritesChooserListener listener, final String label, final int locationProgressDialogId,
      final int waitProgressDialogId)
  {
    synchronized (favoritesChooserDialogLock)
    {
      if (favoritesChooserDialogShowing)
      {
        return;
      }
      favoritesChooserDialogShowing = true;

      // Initialisations
      final LayoutInflater inflater = LayoutInflater.from(activity.getApplicationContext());
      final List<BaliseFavorite> currentFavorites = new ArrayList<BaliseFavorite>();
      final List<BaliseFavorite> removedFavorites = new ArrayList<BaliseFavorite>();

      // Elaboration dialogue
      final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
      builder.setTitle(resources.getString(R.string.label_dialog_balises_title, label));
      builder.setIcon(R.drawable.icon);
      builder.setCancelable(true);
      final OnClickListener positiveListener = new OnClickListener()
      {
        @Override
        public void onClick(final DialogInterface dialog, final int wich)
        {
          // Ajout et retrait des balises favorites
          for (final BaliseFavorite favorite : removedFavorites)
          {
            providersService.getFavoritesService().removeBaliseFavorite(favorite, label);
          }
          for (final BaliseFavorite favorite : currentFavorites)
          {
            providersService.getFavoritesService().addBaliseFavorite(favorite, label);
          }
          providersService.getFavoritesService().saveBalisesFavorites();

          // Signalement
          listener.onFavoritesChoosed();

          // Fermeture dialog
          dialog.dismiss();
          synchronized (favoritesChooserDialogLock)
          {
            favoritesChooserDialogShowing = false;
          }
        }
      };
      builder.setPositiveButton(resources.getString(R.string.button_update), positiveListener);
      builder.setNegativeButton(resources.getString(R.string.button_cancel), new OnClickListener()
      {
        @Override
        public void onClick(final DialogInterface dialog, final int wich)
        {
          // Fermeture dialog
          dialog.dismiss();
          synchronized (favoritesChooserDialogLock)
          {
            favoritesChooserDialogShowing = false;
          }
        }
      });
      builder.setOnCancelListener(new OnCancelListener()
      {
        @Override
        public void onCancel(final DialogInterface dialog)
        {
          synchronized (favoritesChooserDialogLock)
          {
            favoritesChooserDialogShowing = false;
          }
        }
      });

      // La vue
      final View view = inflater.inflate(R.layout.balises_list, null);

      // La liste
      final ListView listView = (ListView)view.findViewById(android.R.id.list);
      listView.setOnItemClickListener(new OnItemClickListener()
      {
        @Override
        public void onItemClick(final AdapterView<?> adapterView, final View inView, final int arg2, final long arg3)
        {
          // Initialisations
          final CheckedTextView checkedTextView = (CheckedTextView)inView;
          final BaliseFavorite finalFavorite = (BaliseFavorite)inView.getTag();

          // Cochage/decochage
          checkedTextView.setChecked(!checkedTextView.isChecked());
          listView.invalidate();

          // Ajouter ou retirer des favoris "courants"
          if (checkedTextView.isChecked())
          {
            currentFavorites.add(finalFavorite);
            removedFavorites.remove(finalFavorite);
          }
          else
          {
            currentFavorites.remove(finalFavorite);
            removedFavorites.add(finalFavorite);
          }
        }
      });
      final List<View> balisesFavoritesViews = new ArrayList<View>();

      // L'adapteur
      final AbstractBalisesFavoritesAdapter adapter = new AbstractBalisesFavoritesAdapter(activity, R.layout.balise_item)
      {
        @Override
        public void publishResults(final List<BaliseFavorite> inFavorites)
        {
          // Initialisations
          clear();

          // Copie
          final List<BaliseFavorite> favorites = new ArrayList<BaliseFavorite>();
          favorites.addAll(inFavorites);

          // Filtrage et ajout
          int balisesCount = 0;
          int baliseIndex = 0;
          synchronized (favorites)
          {
            for (final BaliseFavorite favorite : favorites)
            {
              // Adapter
              add(favorite);

              // View
              balisesCount++;
              final CheckedTextView itemView;
              if (balisesCount > balisesFavoritesViews.size())
              {
                // Recuperation de la vue
                itemView = (CheckedTextView)inflater.inflate(itemId, listView, false);
                balisesFavoritesViews.add(itemView);
              }
              else
              {
                itemView = (CheckedTextView)balisesFavoritesViews.get(baliseIndex);
              }
              baliseIndex++;

              // Synchro
              synchronize(itemView, favorite);
            }
          }

          // Suppression des elements en trop
          for (int i = getCount() - 1; i >= balisesCount; i--)
          {
            // Adapter
            final BaliseFavorite favori = getItem(i);
            remove(favori);

            // View
            balisesFavoritesViews.remove(i);
          }
        }

        /**
         * 
         * @param itemView
         * @param favorite
         */
        private void synchronize(final CheckedTextView itemView, final BaliseFavorite favorite)
        {
          // Texte
          itemView.setText(getFavoriteListName(favorite));

          // Checkbox
          itemView.setChecked(providersService.getFavoritesService().isBaliseFavoriteForLabel(favorite.getProviderId(), favorite.getBaliseId(), label));

          // Le favori
          itemView.setTag(favorite);
        }

        @Override
        public View getView(final int position, final View inView, final ViewGroup parent)
        {
          return balisesFavoritesViews.get(position);
        }
      };

      // Reglage vue
      listView.setAdapter(adapter);

      // Le filtre
      final EditText editText = (EditText)view.findViewById(R.id.balises_list_filter);
      editText.addTextChangedListener(new TextWatcher()
      {
        @Override
        public void afterTextChanged(final Editable editable)
        {
          // Nothing
        }

        @Override
        public void beforeTextChanged(final CharSequence text, final int start, final int count, final int after)
        {
          // Nothing
        }

        @Override
        public void onTextChanged(final CharSequence text, final int start, final int before, final int count)
        {
          final String textString = (text == null ? null : text.toString());
          if (Utils.isStringVide(textString))
          {
            listView.clearTextFilter();
          }
          else
          {
            listView.setFilterText(textString);
          }
        }
      });
      builder.setView(view);

      // Remplissage des favoris (tries par ordre alphabetique)
      final String[] keys = resources.getStringArray(R.array.providers_keys);
      final String[] forDebugs = resources.getStringArray(R.array.providers_forDebugs);
      final String[] hiddens = resources.getStringArray(R.array.providers_hidden);
      final SortedSet<BaliseFavorite> currentNonFilteredBalises = new TreeSet<BaliseFavorite>(new Comparator<BaliseFavorite>()
      {
        @Override
        public int compare(final BaliseFavorite fav1, final BaliseFavorite fav2)
        {
          return getFavoriteListName(fav1).compareToIgnoreCase(getFavoriteListName(fav2));
        }
      });
      for (int i = 0; i < keys.length; i++)
      {
        final boolean forDebug = Boolean.parseBoolean(forDebugs[i]);
        final boolean hidden = ActivityCommons.isBaliseProviderHidden(keys[i], hiddens[i]);
        if ((debugMode || !forDebug) && !hidden)
        {
          final List<String> countries = BaliseProviderUtils.getBaliseProviderActiveCountries(activity.getApplicationContext(), sharedPreferences, keys[i], i);
          for (final String country : countries)
          {
            final String fullKey = BaliseProviderUtils.getBaliseProviderFullKey(keys[i], country);
            final BaliseProvider provider = providersService.getBaliseProvider(fullKey);
            for (final Balise balise : provider.getBalises())
            {
              final BaliseFavorite favorite = new BaliseFavorite(fullKey, balise.id, providersService.getFavoritesService());
              currentNonFilteredBalises.add(favorite);
              if (providersService.getFavoritesService().isBaliseFavoriteForLabel(fullKey, balise.id, label))
              {
                currentFavorites.add(favorite);
              }
            }
          }
        }
      }

      // Conversion
      final BaliseFavorite[] arrayNonFilteredBalises = currentNonFilteredBalises.toArray(new BaliseFavorite[currentNonFilteredBalises.size()]);
      final List<BaliseFavorite> sortedCurrentNonFilteredBalises = Arrays.asList(arrayNonFilteredBalises);

      // Bouton du milieu : a proximite
      if (providersService.getFullLocationService().isLocationEnabled())
      {
        builder.setNeutralButton(resources.getString(R.string.button_proximity), new OnClickListener()
        {
          /**
           * 
           * @author pedro.m
           */
          class FavoritesChooserLocationListener extends AbstractFavoritesLocationListener
          {
            private final DialogInterface dialog;
            private final int             wich;

            /**
             * 
             * @param dialog
             * @param wich
             */
            private FavoritesChooserLocationListener(final DialogInterface dialog, final int wich)
            {
              this.dialog = dialog;
              this.wich = wich;
            }

            @Override
            public void onLocationChanged(final Location location)
            {
              // Recuperation des balises proches
              final List<BaliseFavorite> proches = getProximityBalises(activity.getApplicationContext(), location, providersService);

              // Cochage des balises
              for (int i = 0; i < adapter.getCount(); i++)
              {
                for (final BaliseFavorite proche : proches)
                {
                  if (adapter.getItem(i).equals(proche))
                  {
                    if (!currentFavorites.contains(proche))
                    {
                      currentFavorites.add(proche);
                      removedFavorites.remove(proche);
                    }
                  }
                }
              }

              // MAJ de la liste
              adapter.publishResults(sortedCurrentNonFilteredBalises);

              // Validation
              positiveListener.onClick(dialog, wich);
            }

            @Override
            public void onProviderDisabled(final String provider)
            {
              // Nothing
            }

            @Override
            public void onProviderEnabled(final String provider)
            {
              // Nothing
            }

            @Override
            public void onStatusChanged(final String provider, final int status, final Bundle extras)
            {
              // Nothing
            }
          }

          @Override
          public void onClick(final DialogInterface dialog, final int wich)
          {
            // Appel de la localisation
            final LocationListener locationListener = new FavoritesChooserLocationListener(dialog, wich);
            if (!ActivityCommons.startLocation(activity, providersService, locationListener, false, false, true, locationProgressDialogId, true))
            {
              // GPS et GSM non actifs, question pour redirection vers les parametres de localisation
              ActivityCommons.locationSettingsDialog(activity, activity);
            }

            // Deverrouillage
            synchronized (favoritesChooserDialogLock)
            {
              favoritesChooserDialogShowing = false;
            }
          }
        });
      }

      // Publication
      adapter.setCurrentNonFilteredBalisesFavorites(sortedCurrentNonFilteredBalises);
      adapter.publishResults(sortedCurrentNonFilteredBalises);

      // Fin de la boite de progression
      if (waitProgressDialogId > 0)
      {
        ActivityCommons.cancelProgressDialog(waitProgressDialogId);
      }

      // Affichage
      if (!activity.isFinishing())
      {
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        ActivityCommons.registerAlertDialog(ActivityCommons.ALERT_DIALOG_CHOOSE_FAVS_FOR_LABEL, alertDialog, null);
      }
    }
  }

  /**
   * 
   * @param activity
   * @param providersService
   * @param listener
   * @param choosenLabels
   */
  public static void chooseFavoritesLabels(final Activity activity, final IFullProvidersService providersService, final FavoritesLabelsChooserListener listener, final List<String> choosenLabels)
  {
    // Initialisations
    final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    final String[] items = providersService.getFavoritesService().getLabels().toArray(new String[] {});
    final boolean[] checkeds = new boolean[items.length];
    final List<String> checkedLabels = new ArrayList<String>();

    // Si mode modif : precochage
    if (choosenLabels != null)
    {
      for (int i = 0; i < items.length; i++)
      {
        if (choosenLabels.contains(items[i]))
        {
          checkeds[i] = true;
          checkedLabels.add(items[i]);
        }
      }
    }

    // Elaboration
    builder.setTitle(resources.getString(R.string.label_title_select_labels));
    builder.setCancelable(true);
    builder.setMultiChoiceItems(items, checkeds, new DialogInterface.OnMultiChoiceClickListener()
    {
      @Override
      public void onClick(final DialogInterface dialog, final int index, final boolean checked)
      {
        if (checked)
        {
          checkedLabels.add(items[index]);
        }
        else
        {
          checkedLabels.remove(items[index]);
        }
      }
    });
    final int okId = (choosenLabels == null ? R.string.button_add : R.string.button_update);
    builder.setPositiveButton(resources.getString(okId), new OnClickListener()
    {
      @Override
      public void onClick(final DialogInterface dialog, final int wich)
      {
        listener.onFavoritesLabelsChoosed(checkedLabels);
        dialog.dismiss();
      }
    });
    builder.setNegativeButton(resources.getString(R.string.button_cancel), new OnClickListener()
    {
      @Override
      public void onClick(final DialogInterface dialog, final int wich)
      {
        dialog.dismiss();
      }
    });

    // Affichage
    final AlertDialog alertDialog = builder.create();
    alertDialog.show();
    ActivityCommons.registerAlertDialog(ActivityCommons.ALERT_DIALOG_CHOOSE_FAVS_LABEL, alertDialog, null);
  }

  /**
   * 
   * @param activity
   * @param providersService
   * @param listener
   * @param choosenLabel
   * @param showNewListItem
   * @param showProximityListItem
   * @param proximityMode
   * @param flightMode
   */
  public static void chooseFavoriteLabel(final Activity activity, final IFullProvidersService providersService, final FavoriteLabelChooserListener listener, final String choosenLabel, final boolean showNewListItem,
      final boolean showProximityListItem, final boolean proximityMode, final boolean flightMode)
  {
    // Initialisations
    final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    final List<String> existingLabels = providersService.getFavoritesService().getLabels();
    final int existingLabelsSize = existingLabels.size();
    final int nbItems = existingLabelsSize + (showNewListItem ? 1 : 0) + (showProximityListItem ? 1 : 0);
    final String[] items = new String[nbItems];

    // Remplissage avec les labels existants
    {
      int i = 0;
      for (final String label : existingLabels)
      {
        items[i] = label;
        i++;
      }
    }

    // Item a proximite
    final int proximityItemIndex = existingLabelsSize;
    if (showProximityListItem)
    {
      items[proximityItemIndex] = resources.getString(R.string.label_proximity);
    }

    // Item nouvelle liste
    final int newListItemIndex = existingLabelsSize + (showProximityListItem ? 1 : 0);
    if (showNewListItem)
    {
      items[newListItemIndex] = resources.getString(R.string.label_new_list_item);
    }

    // Si mode modif : precochage
    int selected = -1;
    if (showProximityListItem && proximityMode)
    {
      selected = proximityItemIndex;
    }
    else if ((choosenLabel != null) && !flightMode)
    {
      for (int i = 0; i < items.length; i++)
      {
        if (choosenLabel.equals(items[i]))
        {
          selected = i;
        }
      }
    }

    // Elaboration
    builder.setTitle(resources.getString(R.string.label_title_choose_label));
    builder.setCancelable(true);
    builder.setSingleChoiceItems(items, selected, new DialogInterface.OnClickListener()
    {
      @Override
      public void onClick(final DialogInterface dialog, final int inSelected)
      {
        // Label existant
        if (inSelected < existingLabelsSize)
        {
          listener.onFavoriteLabelChoosed(items[inSelected]);
        }
        // A proximite
        else if (showProximityListItem && (inSelected == proximityItemIndex))
        {
          listener.onProximityModeChoosed();
        }
        // Nouvelle liste
        else if (showNewListItem && (inSelected == newListItemIndex))
        {
          String newLabel = resources.getString(R.string.label_default_new_label, Integer.valueOf(existingLabelsSize + 1));
          while (providersService.getFavoritesService().getLabels().contains(newLabel))
          {
            newLabel = newLabel + NEW_LABEL_SUFFIX;
          }
          providersService.getFavoritesService().addLabel(newLabel);
          providersService.getFavoritesService().saveLabels();
          providersService.getFavoritesService().saveBalisesFavorites();
          listener.onFavoriteLabelChoosed(newLabel);
        }

        // Fermeture dialogue
        dialog.dismiss();
      }
    });

    // Affichage
    final AlertDialog alertDialog = builder.create();
    alertDialog.show();
    ActivityCommons.registerAlertDialog(ActivityCommons.ALERT_DIALOG_CHOOSE_FAV_LABEL, alertDialog, null);
  }

  /**
   * 
   * @param providersService
   * @param providerKey
   * @param baliseId
   * @param labels
   */
  public static void updateAndSaveBaliseFavoriteLabels(final IFullProvidersService providersService, final String providerKey, final String baliseId, final List<String> labels)
  {
    if (providersService.getFavoritesService().isBaliseFavorite(providerKey, baliseId))
    {
      providersService.getFavoritesService().updateBaliseFavorite(providerKey, baliseId, labels);
    }
    else
    {
      providersService.getFavoritesService().addBaliseFavorite(providerKey, baliseId, labels);
    }

    // Sauve
    providersService.getFavoritesService().saveBalisesFavorites();
  }

  /**
   * 
   * @param activity
   */
  public static void widgetPreferences(final Activity activity)
  {
    // Initialisations
    final Intent intent = new Intent(activity.getString(R.string.intent_widget_preferences_action));

    // Demarrage
    activity.startActivity(intent);
  }

  /**
   * 
   * @param providersService
   * @return
   */
  public static boolean isFlightMode(final IFullProvidersService providersService)
  {
    return (providersService == null ? false : providersService.isFlightMode());
  }

  /**
   * 
   * @param providersService
   * @return
   */
  public static boolean isHistoryMode(final IFullProvidersService providersService)
  {
    return (providersService == null ? false : providersService.isHistoryMode());
  }

  /**
   * 
   * @param providersService
   * @return
   */
  public static boolean isAlarmMode(final IFullProvidersService providersService)
  {
    return (providersService == null ? false : providersService.isAlarmMode());
  }

  /**
   * 
   * @param item
   * @param providersService
   * @return
   */
  public static boolean manageFlightModeMenuItem(final MenuItem item, final IFullProvidersService providersService)
  {
    final boolean flightMode = isFlightMode(providersService);
    item.setChecked(flightMode);
    item.setIcon(flightMode ? R.drawable.ic_menu_mode_vol_on : R.drawable.ic_menu_mode_vol_off);

    return flightMode;
  }

  /**
   * 
   * @param item
   * @param providersService
   * @return
   */
  public static boolean manageHistoryModeMenuItem(final MenuItem item, final IFullProvidersService providersService)
  {
    final boolean historyMode = isHistoryMode(providersService);
    item.setChecked(historyMode);

    return historyMode;
  }

  /**
   * 
   * @param item
   * @param providersService
   * @return
   */
  public static boolean manageAlarmModeMenuItem(final MenuItem item, final IFullProvidersService providersService)
  {
    final boolean alarmMode = isAlarmMode(providersService);
    item.setChecked(alarmMode);

    return alarmMode;
  }

  /**
   * 
   * @param context
   * @param id
   */
  private static void addNotification(final Context context, final int id)
  {
    synchronized (notifications)
    {
      final Integer theId = Integer.valueOf(id);
      if (!notifications.contains(theId))
      {
        notifications.add(theId);
        manageNotification(context);
      }
    }
  }

  /**
   * 
   * @param context
   * @param id
   */
  private static void removeNotification(final Context context, final int id)
  {
    synchronized (notifications)
    {
      final Integer theId = Integer.valueOf(id);
      if (notifications.contains(theId))
      {
        notifications.remove(theId);
        manageNotification(context);
      }
    }
  }

  /**
   * 
   * @param context
   */
  private static void manageNotification(final Context context)
  {
    synchronized (notifications)
    {
      // Initialisations
      final NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

      // Suppression notification
      notificationManager.cancel(MOBIBALISES_NOTIFICATION_ID);

      // Ajout notification si besoin
      if (notifications.size() > 0)
      {
        // Action sur click
        final Intent notificationIntent = new Intent(context, BalisesStartActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        // Modes actifs
        final StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (final Integer id : notifications)
        {
          // Separateur
          if (!first)
          {
            builder.append(Strings.CHAR_VIRGULE);
            builder.append(Strings.CHAR_SPACE);
          }

          // Texte
          builder.append(resources.getString(id.intValue()));

          // Next
          first = false;
        }

        // Notification
        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);
        notificationBuilder.setSmallIcon(R.drawable.icon_small);
        notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.icon));
        notificationBuilder.setContentTitle(resources.getString(R.string.label_notification_title));
        notificationBuilder.setContentText(resources.getString(R.string.label_notification_text, builder.toString()));
        notificationBuilder.setContentIntent(contentIntent);
        final Notification notification = notificationBuilder.build();
        notification.flags = Notification.FLAG_NO_CLEAR;
        notificationManager.notify(MOBIBALISES_NOTIFICATION_ID, notification);
      }
    }
  }

  /**
   * 
   * @param context
   * @param providersService
   * @param force
   */
  public static void addFlightModeNotification(final Context context, final IFullProvidersService providersService, final boolean force)
  {
    // Mode vol ?
    final boolean flightMode = force || isFlightMode(providersService);

    // Ajout de la notification si mode vol actif
    if (flightMode)
    {
      addNotification(context, R.string.label_notification_flight_mode);
    }
  }

  /**
   * 
   * @param context
   */
  public static void removeFlightModeNotification(final Context context)
  {
    removeNotification(context, R.string.label_notification_flight_mode);
  }

  /**
   * 
   * @param context
   */
  public static void addRecordModeNotification(final Context context)
  {
    addNotification(context, R.string.label_notification_record_mode);
  }

  /**
   * 
   * @param context
   */
  public static void removeRecordModeNotification(final Context context)
  {
    removeNotification(context, R.string.label_notification_record_mode);
  }

  /**
   * 
   * @param fullProvidersService
   */
  public static void toggleHistoryMode(final Context context, final IFullProvidersService fullProvidersService)
  {
    // Bascule
    if (fullProvidersService != null)
    {
      // Flag
      final boolean wasHistoryMode = fullProvidersService.isHistoryMode();
      fullProvidersService.setHistoryMode(!wasHistoryMode);

      // Message
      Toast.makeText(context, wasHistoryMode ? R.string.message_history_mode_off : R.string.message_history_mode_on, Toast.LENGTH_SHORT).show();
    }
  }

  /**
   * 
   * @param context
   */
  public static void addHistoryModeNotification(final Context context)
  {
    // Mode historique ?
    final boolean historyMode = ProvidersService.isHistoryModeOn(context);

    // Ajout de la notification si mode historique actif
    if (historyMode)
    {
      addNotification(context, R.string.label_notification_history_mode);
    }
  }

  /**
   * 
   * @param context
   */
  public static void removeHistoryModeNotification(final Context context)
  {
    removeNotification(context, R.string.label_notification_history_mode);
  }

  /**
   * 
   * @param fullProvidersService
   */
  public static void toggleAlarmMode(final Context context, final IFullProvidersService fullProvidersService)
  {
    // Bascule
    if (fullProvidersService != null)
    {
      // Flag
      final boolean wasAlarmMode = fullProvidersService.isAlarmMode();
      fullProvidersService.setAlarmMode(!wasAlarmMode);

      // Message
      Toast.makeText(context, wasAlarmMode ? R.string.message_alarm_mode_off : R.string.message_alarm_mode_on, Toast.LENGTH_SHORT).show();
    }
  }

  /**
   * 
   * @param context
   */
  public static void addAlarmModeNotification(final Context context)
  {
    // Mode alarme ?
    final boolean alarmMode = ProvidersService.isAlarmModeOn(context);

    // Ajout de la notification si mode historique actif
    if (alarmMode && AlarmUtils.almostOneActiveAlarm(context, null))
    {
      addNotification(context, R.string.label_notification_alarm_mode);
    }
  }

  /**
   * 
   * @param context
   */
  public static void removeAlarmModeNotification(final Context context)
  {
    removeNotification(context, R.string.label_notification_alarm_mode);
  }

  /**
   * 
   * @param context
   */
  public static void addSpeakBlackWidgetNotification(final Context context)
  {
    // Presence d'un widget parlant meme si ecran eteint ?
    final boolean speakBlack = BalisesWidgets.existsSpeakBlackWidget(context);

    // Ajout de la notification si oui
    if (speakBlack)
    {
      addNotification(context, R.string.label_notification_speak_black_widget);
    }
  }

  /**
   * 
   * @param context
   */
  public static void removeSpeakBlackWidgetNotification(final Context context)
  {
    removeNotification(context, R.string.label_notification_speak_black_widget);
  }

  /**
   * 
   * @param activity
   * @param inSharedPreferences
   * @param providersService
   * @param provider
   * @param providerKey
   * @param idBalise
   */
  public static void historiqueBalise(final Activity activity, final SharedPreferences inSharedPreferences, final IFullProvidersService providersService, final BaliseProvider provider, final String providerKey, final String idBalise)
  {
    // Verification donnees historique
    boolean ok = false;
    boolean complete = false;
    try
    {
      final Collection<Releve> releves = providersService.getHistory(providerKey, idBalise);
      final Releve lastReleve = providersService.getBaliseProvider(providerKey).getReleveById(idBalise);
      ok = (releves != null) && (releves.size() > 0);
      complete = ok && isHistoryComplete(releves, lastReleve);
    }
    catch (final IOException ioe)
    {
      Log.w(activity.getClass().getSimpleName(), "Impossible de retrouver les donnees d'historique", ioe);
    }

    // Analyse des donnees presentes en local
    final String baliseName = provider.getBaliseById(idBalise).nom;
    final boolean downloadable = HistoryBaliseProvider.class.isAssignableFrom(providersService.getBaliseProvider(providerKey).getBaliseProviderClass());

    // Selon le cas
    if (downloadable && (!ok || !complete))
    {
      // Initialisations
      final boolean finalOk = ok;

      // Test de la presence du reseau
      final ConnectivityManager connectivityManager = (ConnectivityManager)activity.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
      final boolean networkOff = AbstractProvidersService.isNetworkOff(connectivityManager);
      if (networkOff)
      {
        // Telechargement impossible : pas de reseau
        ActivityCommons.alertDialog(activity, ActivityCommons.ALERT_DIALOG_HISTORY_NO_NETWORK, R.drawable.icon, resources.getString(R.string.app_name),
            resources.getString((ok ? R.string.message_history_no_network_incomplete : R.string.message_history_no_network), baliseName), new OnClickListener()
            {
              @Override
              public void onClick(final DialogInterface dialog, final int which)
              {
                // Historique quand meme si presence d'anciennes donnees
                if (finalOk)
                {
                  startHistoriqueBalise(activity, provider, providerKey, idBalise);
                }
              }
            }, true, null, 0);

        return;
      }

      // Telechargement possible voire necessaire
      final OnClickListener okClickListener = new OnClickListener()
      {
        @Override
        public void onClick(final DialogInterface dialog, final int which)
        {
          // Thread
          final CachedProvider cachedProvider = (CachedProvider)provider;
          final HistoryBaliseProvider historyProvider = (HistoryBaliseProvider)cachedProvider.getOriginalBaliseProvider();
          final HistoryDownloadThread downloadThread = new HistoryDownloadThread(activity.getClass().getSimpleName(), idBalise, providerKey, historyProvider, new HistoryDownloadThread.HistoryDownloadListener()
          {
            @Override
            public void downloadComplete(final Collection<Releve> releves)
            {
              try
              {
                // Fermeture progression
                ActivityCommons.cancelProgressDialog(ActivityCommons.PROGRESS_DIALOG_HISTORY_DOWNLOAD);

                // Enregistrement
                providersService.recordHistory(providerKey, idBalise, releves);

                // Historique
                startHistoriqueBalise(activity, provider, providerKey, idBalise);
              }
              catch (final IOException ioe)
              {
                Log.e(activity.getClass().getSimpleName(), "Erreur a la sauvegarde de l'historique pour la balise " + providerKey + "." + idBalise, ioe);
                ActivityCommons.alertDialog(activity, ActivityCommons.ALERT_DIALOG_HISTORY_SAVE_ERROR, R.drawable.icon, resources.getString(R.string.app_name),
                    resources.getString(R.string.message_history_save_error, baliseName, provider.filterExceptionMessage(ioe.getMessage())), null, true, null, 0);
              }
            }

            @Override
            public void downloadFailed(final IOException ioe)
            {
              // Fermeture progression
              ActivityCommons.cancelProgressDialog(ActivityCommons.PROGRESS_DIALOG_HISTORY_DOWNLOAD);

              // Message d'erreur
              ActivityCommons.alertDialog(activity, ActivityCommons.ALERT_DIALOG_HISTORY_DOWNLOAD_ERROR, R.drawable.icon, resources.getString(R.string.app_name),
                  resources.getString(R.string.message_history_download_error, baliseName, provider.filterExceptionMessage(ioe == null ? null : ioe.getMessage())), null, true, null, 0);
            }
          });

          // Message de progression
          ActivityCommons.progressDialog(activity, ActivityCommons.PROGRESS_DIALOG_HISTORY_DOWNLOAD, resources.getString(R.string.app_name), resources.getString(R.string.message_history_download_progress), true, true,
              new OnCancelListener()
              {
                @Override
                public void onCancel(final DialogInterface inDialog)
                {
                  downloadThread.interrupt();
                }
              });

          // Lancement thread
          downloadThread.start();
        }
      };

      // Message de confirmation si necessaire
      final boolean askBeforeOverwrite = inSharedPreferences.getBoolean(resources.getString(R.string.config_history_mode_ask_key), Boolean.parseBoolean(resources.getString(R.string.config_history_mode_ask_default)));
      if (askBeforeOverwrite)
      {
        final int messageId = (ok ? R.string.message_history_download_incomplete_data : R.string.message_history_download_no_data);
        ActivityCommons.choiceDialog(activity, ActivityCommons.CONFIRM_DIALOG_HISTORY_OVERWRITE, resources.getString(R.string.app_name), resources.getString(messageId, baliseName), okClickListener, new OnClickListener()
        {
          @Override
          public void onClick(final DialogInterface dialog, final int which)
          {
            final SharedPreferences.Editor editor = inSharedPreferences.edit();
            editor.putBoolean(resources.getString(R.string.config_history_mode_ask_key), false);
            ActivityCommons.commitPreferences(editor);
            okClickListener.onClick(dialog, which);
          }
        }, new OnClickListener()
        {
          @Override
          public void onClick(final DialogInterface dialog, final int which)
          {
            // Historique quand meme si donnees ok seulement incompletes (et pas de telechargement)
            if (finalOk)
            {
              startHistoriqueBalise(activity, provider, providerKey, idBalise);
            }
          }
        }, true, null, R.string.button_yes, R.string.button_always, ok ? R.string.button_no : R.string.button_cancel);
      }

      // Sinon telechargement en ecrasant les donnees locales
      else
      {
        okClickListener.onClick(null, 0);
      }

      // Fin
      return;
    }
    else if (!ok)
    {
      // Telechargement impossible
      ActivityCommons.alertDialog(activity, ActivityCommons.ALERT_DIALOG_HISTORY_NO_DATA, R.drawable.icon, resources.getString(R.string.app_name), resources.getString(R.string.message_history_no_data, baliseName), null, true, null, 0);
      return;
    }

    // Historique
    startHistoriqueBalise(activity, provider, providerKey, idBalise);
  }

  /**
   * 
   * @param releves
   * @param lastProviderReleve
   * @return
   */
  private static boolean isHistoryComplete(final Collection<Releve> releves, final Releve lastProviderReleve)
  {
    // Aucun releve ?
    if (releves == null)
    {
      return false;
    }

    // Premier et dernier releve
    Releve first = null;
    Releve last = null;
    for (final Releve releve : releves)
    {
      if (first == null)
      {
        first = releve;
      }
      last = releve;
    }

    // Probleme ?
    if ((first == null) || (first.date == null) || (last == null) || (last.date == null))
    {
      return false;
    }

    // Dernier releve d'historique identique au dernier releve du provider ?
    if ((lastProviderReleve != null) && (lastProviderReleve.date.getTime() != last.date.getTime()))
    {
      // Non, donc l'historique n'est pas complet
      return false;
    }

    // Valeurs limites
    final long delaiPeremption = 60 * 1000 * (long)sharedPreferences.getInt(resources.getString(R.string.config_map_outofdate_key), Integer.parseInt(resources.getString(R.string.config_map_outofdate_default), 10));
    final long historyDuration = 24 * 3600 * 1000 * (long)sharedPreferences.getInt(resources.getString(R.string.config_history_mode_duration_key), Integer.parseInt(resources.getString(R.string.config_history_mode_duration_default), 10));
    final long utcNow = Utils.toUTC(System.currentTimeMillis());
    final boolean completeBegin = first.date.getTime() <= (utcNow - historyDuration + delaiPeremption);
    final boolean completeEnd = last.date.getTime() >= (utcNow - delaiPeremption);

    // Fin
    return completeBegin && completeEnd;
  }

  /**
   * 
   * @param activity
   * @param provider
   * @param providerKey
   * @param idBalise
   */
  static void startHistoriqueBalise(final Activity activity, final BaliseProvider provider, final String providerKey, final String idBalise)
  {
    // Activite historique
    final Intent intent = new Intent(activity.getString(R.string.intent_history_action));
    intent.putExtra(HistoryActivity.INTENT_EXTRA_PROVIDER_KEY, providerKey);
    intent.putExtra(HistoryActivity.INTENT_EXTRA_BALISE_ID, idBalise);
    intent.putExtra(HistoryActivity.INTENT_EXTRA_BALISE_NAME, provider.getBaliseById(idBalise).nom);
    intent.putExtra(HistoryActivity.INTENT_EXTRA_PROVIDER_DELTA_RELEVES, provider.getDefaultDeltaReleves());
    final SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putInt(HistoryActivity.PREFERENCES_ACTIVE_INDEX, -1);
    ActivityCommons.commitPreferences(editor);
    activity.startActivity(intent);
  }

  /**
   * 
   * @param tendance
   * @return
   */
  public static String getStringTendance(final TendanceVent tendance)
  {
    switch (tendance)
    {
      case FAIBLE_HAUSSE:
        return STRING_TENDANCE_VENT_FAIBLE_HAUSSE;
      case FORTE_HAUSSE:
        return STRING_TENDANCE_VENT_FORTE_HAUSSE;
      case FAIBLE_BAISSE:
        return STRING_TENDANCE_VENT_FAIBLE_BAISSE;
      case FORTE_BAISSE:
        return STRING_TENDANCE_VENT_FORTE_BAISSE;
      case INCONNUE:
      case STABLE:
      default:
        return Strings.VIDE;
    }
  }

  /**
   * 
   * @param context
   * @param alarm
   * @param levee
   * @param verifiee
   * @param releve
   */
  public static void manageAlarmNotification(final Context context, final BaliseAlarm alarm, final boolean levee, final boolean verifiee, final Releve releve)
  {
    // Initialisations
    final NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
    final int notificationId = alarm.hashCode();
    final String notificationTag = Boolean.toString(verifiee);

    // Suppression notification
    if (!levee)
    {
      notificationManager.cancel(notificationTag, notificationId);
      return;
    }

    // ...Sinon ajout notification

    // Action principale : mode carte
    final Intent notificationIntent = new Intent(resources.getString(R.string.intent_map_action));
    notificationIntent.putExtra(AbstractBalisesMapActivity.EXTRA_BALISE_PROVIDER_ID, alarm.provider);
    notificationIntent.putExtra(AbstractBalisesMapActivity.EXTRA_BALISE_ID, alarm.idBalise);
    final PendingIntent contentIntent = PendingIntent.getActivity(context, alarm.hashCode() + 1, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

    // Action 2 : acquittement de la notification
    final Intent acqIntent = new Intent(ProvidersService.ACTION_ALARM_TAP);
    acqIntent.putExtra(ProvidersService.INTENT_ALARM_ACTION, ProvidersService.INTENT_ALARM_ACTION_CANCEL_NOTIFICATION);
    acqIntent.putExtra(ProvidersService.INTENT_ALARM_ID, alarm.getId());
    acqIntent.putExtra(ProvidersService.INTENT_ALARM_NOTIFICATION_TAG, notificationTag);
    final PendingIntent alarmAcqIntent = PendingIntent.getBroadcast(context, alarm.hashCode() + 2, acqIntent, PendingIntent.FLAG_CANCEL_CURRENT);

    // Action 3 : desactivation de l'alarme
    final Intent alarmIntent = new Intent(ProvidersService.ACTION_ALARM_TAP);
    alarmIntent.putExtra(ProvidersService.INTENT_ALARM_ACTION, ProvidersService.INTENT_ALARM_ACTION_CANCEL_ALARM);
    alarmIntent.putExtra(ProvidersService.INTENT_ALARM_ID, alarm.getId());
    alarmIntent.putExtra(ProvidersService.INTENT_ALARM_NOTIFICATION_TAG, notificationTag);
    final PendingIntent alarmActionIntent = PendingIntent.getBroadcast(context, alarm.hashCode() + 3, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);

    // Action 4 : desactivation du mode alarme
    final Intent alarmModeIntent = new Intent(ProvidersService.ACTION_ALARM_TAP);
    alarmModeIntent.putExtra(ProvidersService.INTENT_ALARM_ACTION, ProvidersService.INTENT_ALARM_ACTION_CANCEL_ALARM_MODE);
    alarmModeIntent.putExtra(ProvidersService.INTENT_ALARM_ID, alarm.getId());
    alarmModeIntent.putExtra(ProvidersService.INTENT_ALARM_NOTIFICATION_TAG, notificationTag);
    final PendingIntent alarmModeActionIntent = PendingIntent.getBroadcast(context, alarm.hashCode() + 4, alarmModeIntent, PendingIntent.FLAG_CANCEL_CURRENT);

    // Titre
    final String titlePattern = resources.getString(R.string.alarm_notification_android_title, alarm.nomProvider, alarm.nomBalise);
    final String title = AlarmUtils.formatAlarmText(context, titlePattern, alarm, releve);

    // Texte principal
    final String textPattern;
    final String textPerso = (verifiee ? alarm.texteVerifieeNotificationAndroidPerso : alarm.texteNonVerifieeNotificationAndroidPerso);
    if (alarm.checkNotificationAndroidPerso && !Utils.isStringVide(textPerso))
    {
      textPattern = textPerso;
    }
    else
    {
      textPattern = resources.getString(verifiee ? R.string.alarm_notification_android_text_verifiee : R.string.alarm_notification_android_text_non_verifiee);
    }
    final String text = AlarmUtils.formatAlarmText(context, textPattern, alarm, releve);

    // Construction
    final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
    builder.setSmallIcon(R.drawable.ic_mobibalises_alarm_small);
    builder.setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_mobibalises_alarm));
    builder.setTicker(title);
    builder.setContentTitle(title);
    builder.setContentText(text);
    builder.setContentText(Html.fromHtml(text));
    builder.setContentIntent(contentIntent);
    builder.addAction(R.drawable.ic_cab_done_holo_dark, resources.getString(R.string.alarm_notification_android_action_notification), alarmAcqIntent);
    builder.addAction(R.drawable.ic_mobibalises_alarm_mute, resources.getString(R.string.alarm_notification_android_action_alarme), alarmActionIntent);
    builder.addAction(R.drawable.ic_mobibalises_alarm_mute_all, resources.getString(R.string.alarm_notification_android_action_mode_alarme), alarmModeActionIntent);

    // Son
    if (alarm.checkNotificationAndroidAudio && (alarm.uriNotificationAndroidAudio != null))
    {
      builder.setSound(alarm.uriNotificationAndroidAudio, AudioManager.STREAM_NOTIFICATION);
    }

    // Vibration
    if (alarm.checkNotificationAndroidVibration)
    {
      builder.setVibrate(new long[] { 0, 100, 250, 100, 250, 100 });
    }

    // Notification
    final Notification notification = builder.build();
    notificationManager.notify(notificationTag, notificationId, notification);
  }
}
