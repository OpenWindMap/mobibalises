package org.pedro.android.mobibalises_common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.pedro.android.mobibalises_common.analytics.AnalyticsService;
import org.pedro.android.mobibalises_common.location.LocationService;
import org.pedro.android.mobibalises_common.map.AbstractBalisesMapActivity;
import org.pedro.android.mobibalises_common.preferences.AbstractBalisesPreferencesActivity;
import org.pedro.android.mobibalises_common.provider.BaliseProviderUtils;
import org.pedro.android.mobibalises_common.service.BaliseProviderInfos;
import org.pedro.android.mobibalises_common.service.IProvidersService;
import org.pedro.android.mobibalises_common.service.WebcamProviderInfos;
import org.pedro.android.mobibalises_common.start.AbstractBalisesStartActivity;
import org.pedro.balises.BaliseProvider;
import org.pedro.balises.Utils;
import org.pedro.balises.ffvl.FfvlProvider;
import org.pedro.balises.ffvl.LastUpdateFfvlContentHandler;
import org.pedro.utils.ThreadUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.text.Html;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 
 * @author pedro.m
 */
public abstract class ActivityCommons
{
  public static final int           PROGRESS_DIALOG_SPOTS                     = 1000;
  public static final int           PROGRESS_DIALOG_SPOT_INFOS                = 1001;
  public static final int           PROGRESS_DIALOG_SPOTS_DOWNLOAD            = 1002;
  public static final int           PROGRESS_DIALOG_MAP_LAYERS                = 1010;
  public static final int           PROGRESS_DIALOG_LOCATION                  = 1020;
  public static final int           PROGRESS_DIALOG_FFVL                      = 1030;
  public static final int           PROGRESS_DIALOG_HISTORY_DOWNLOAD          = 1040;

  public static final int           ALERT_DIALOG_DATA_INFOS                   = 2000;
  public static final int           ALERT_DIALOG_WHATS_NEW                    = 2010;
  public static final int           ALERT_DIALOG_STARTUP                      = 2020;
  public static final int           ALERT_DIALOG_FFVL_MESSAGE                 = 2030;
  public static final int           ALERT_DIALOG_SPOT_ERROR                   = 2040;
  public static final int           ALERT_DIALOG_SPOT_DOWNLOAD_ERROR          = 2041;
  public static final int           ALERT_DIALOG_HISTORY_NO_NETWORK           = 2050;
  public static final int           ALERT_DIALOG_HISTORY_SAVE_ERROR           = 2051;
  public static final int           ALERT_DIALOG_HISTORY_DOWNLOAD_ERROR       = 2052;
  public static final int           ALERT_DIALOG_HISTORY_NO_DATA              = 2053;
  public static final int           ALERT_DIALOG_HISTORY_READ_ERROR           = 2054;
  public static final int           ALERT_DIALOG_VOICE_SDCARD_UNAVAILABLE     = 2060;
  public static final int           ALERT_DIALOG_MAP_LAYERS                   = 2070;
  public static final int           ALERT_DIALOG_MAP_TYPE                     = 2080;
  public static final int           ALERT_DIALOG_PREFS_SPOTS_COUNTRY_CHOICE   = 2090;
  public static final int           ALERT_DIALOG_PREFS_BALISE_PROVIDER_CHOICE = 2100;
  public static final int           ALERT_DIALOG_FAVS_DISPLAY_MODE            = 2110;
  public static final int           ALERT_DIALOG_FAV_LABEL_EDIT               = 2120;
  public static final int           ALERT_DIALOG_UNLICENSED                   = 2130;
  public static final int           ALERT_DIALOG_CHOOSE_FAVS_LABEL            = 2140;
  public static final int           ALERT_DIALOG_CHOOSE_FAVS_FOR_LABEL        = 2150;
  public static final int           ALERT_DIALOG_CHOOSE_FAV_LABEL             = 2160;
  public static final int           ALERT_DIALOG_FULL_VERSION_PROMOTE         = 2170;
  public static final int           ALERT_DIALOG_IGN                          = 2180;

  public static final int           CONFIRM_DIALOG_HISTORY_OVERWRITE          = 3000;
  public static final int           CONFIRM_DIALOG_VOICE_INSTALL              = 3010;
  public static final int           CONFIRM_DIALOG_FAV_LABEL_DELETE           = 3020;
  public static final int           CONFIRM_DIALOG_LOCATION_SETTINGS          = 3030;
  public static final int           CONFIRM_DIALOG_SEARCH_RESET               = 3040;

  private static final String       HTML_TITRE_FFVL_PREFIX                    = "<b><u>";
  private static final String       HTML_TITRE_FFVL_SUFFIX                    = "</u></b>";

  public static final Uri           MARKET_URI                                = Uri.parse("market://details?id=com.pedro.android.mobibalises");

  //TODO 201609 public static final File          MOBIBALISES_EXTERNAL_STORAGE_PATH         = new File(Environment.getExternalStorageDirectory(), ".mobibalises");
  public static File                MOBIBALISES_EXTERNAL_STORAGE_PATH;

  private static final String       HTML_WHATS_NEW_PREFIX                     = "<big><b>";
  private static final String       HTML_WHATS_NEW_SUFFIX                     = "</b></big>";
  private static final String       HTML_WHATS_NEW_ITEM_PREFIX                = "&#8226; ";

  private static final String       HTML_FIRST_LAUNCH_PREFIX                  = "<b>";
  private static final String       HTML_FIRST_LAUNCH_SUFFIX                  = "</b>";

  private static final String       HTML_FONT_COLOR_PREFIX                    = "<font color=\"{0}\">";
  private static final String       HTML_FONT_COLOR_SUFFIX                    = "</font>";
  private static final String       HTML_ERROR_PREFIX                         = "<b>";
  private static final String       HTML_ERROR_SUFFIX                         = "</b>";
  private static final String       HTML_NETWORK_ERROR_PREFIX                 = "<b><big>";
  private static final String       HTML_NETWORK_ERROR_SUFFIX                 = "</big></b>\n\n";
  private static final String       HTML_LINE_TYPE_SUFFIX                     = ")</small>";
  private static final String       HTML_LINE_TYPE_PREFIX                     = "<small>&nbsp;&nbsp;(";
  private static final String       HTML_INFO_LINE_PREFIX                     = "\n&nbsp;&nbsp;&nbsp;&nbsp;";
  private static final String       HTML_RELEVES_BALISES_PREFIX               = "\n<b>&#8226; ";
  private static final String       HTML_RELEVES_BALISES_SUFFIX               = " :</b>";
  private static final String       HTML_PROVIDER_STATUS_PREFIX               = " <i><small>";
  private static final String       HTML_PROVIDER_STATUS_SUFFIX               = "</small></i>";
  private static final String       HTML_BIG_BOLD_UNDERLINE                   = "<big><b><u>";
  private static final String       HTML_BIG_BOLD_UNDERLINE_END               = "</u></b></big>";
  private static final String       STRING_TROIS_POINTS_INTERROGATION         = "???";

  private static final String       MOBIBALISES_WEB_URL                       = "http://www.mobibalises.net";
  public static final String        HELP_URL                                  = MOBIBALISES_WEB_URL + "/aide.php";

  private static final Object       initLock                                  = new Object();
  private static boolean            initialized                               = false;

  static Resources                  resources;
  static SharedPreferences          sharedPreferences;
  @SuppressWarnings("unused")
  private static final Object       prefsLock                                 = new Object();

  private static DateFormat         formatDateInfosDonnees;
  private static DateFormat         formatTimeInfosDonnees;
  private static DateFormat         formatDateMessageFfvl;

  private static boolean            debugMode;

  // Dialogues
  private static Handler            alertDialogHandler;
  static final SparseArray<Dialog>  alertDialogs                              = new SparseArray<Dialog>();
  private static Handler            confirmDialogHandler;

  // Gestion de la boite de dialogue de progression
  private static Handler            progressDialogHandler;
  static final SparseArray<Dialog>  progressDialogs                           = new SparseArray<Dialog>();
  private static final int          DIALOG_SHOW                               = 1;
  private static final int          DIALOG_CANCEL                             = 2;

  // Gestion de la touche back
  private static Timer              backKeyTimer;
  static final Object               backKeyTimerLock                          = new Object();
  static boolean                    backKeyTimerStarted                       = false;
  private static boolean            backKeyPressed                            = false;
  private static Toast              backKeyToast;
  private static boolean            backKeyManagerInitialized                 = false;
  private static final long         DELAI_TIMER_CONFIRM_QUIT                  = 2000;

  // Gestion du WakeLock
  private static WakeLock           wakeLock;
  private static final String       WAKELOCK_TAG                              = "MobiBalises";

  // Gestion du message de statut
  private static final String       STATUS_MESSAGE_SEPARATOR                  = ", ";
  private static final List<String> statusMessageNamesList                    = new ArrayList<String>();
  private static String             statusMessage;
  public static final Object        statusMessageLock                         = new Object();
  private static final List<String> statusExceptionsKeyList                   = new ArrayList<String>();
  private static final List<String> statusExceptionsList                      = new ArrayList<String>();
  private static boolean            networkOff;

  public enum ProvidersStatus
  {
    OK, WARNING, ERROR
  }

  // Gestion du dialogue de localisation
  public static boolean                locationSettingsLaunched     = false;

  // Gestion du dialogue d'infos sur les donnees
  static final Object                  dataInfosLock                = new Object();
  static boolean                       dataInfosVisible             = false;
  private static final DataInfosCloser dataInfosCloser              = new DataInfosCloser();

  // Gestion du message FFVL
  static boolean                       ffvlMessageThreadInterrupted = false;

  // Unites
  private static String                unitSpeedLabel;
  private static double                unitSpeedFactor;
  private static String                unitAltitudeLabel;
  private static double                unitAltitudeFactor;
  private static String                unitDistanceLabel;
  private static double                unitDistanceFactor;
  private static String                unitTemperatureLabel;
  private static double                unitTemperatureFactor;
  private static double                unitTemperatureDelta;

  // Revelation des providers caches
  private static final List<String>    baliseProviderShowFiles      = new ArrayList<String>();
  private static final String          SHOW_FILE_EXTENSION          = ".show";

  /**
   * 
   * @param context
   */
  public static void init(final Context context)
  {
    // Une seule fois
    synchronized (initLock)
    {
      // TODO 201609
      Log.d(ActivityCommons.class.getSimpleName(), "########### Environment.getExternalStorageDirectory() = " + Environment.getExternalStorageDirectory());
      Log.d(ActivityCommons.class.getSimpleName(), "########### context.getExternalFilesDir(null) = " + context.getExternalFilesDir(null));
      MOBIBALISES_EXTERNAL_STORAGE_PATH = context.getExternalFilesDir(null);

      // Initialisation formats dates
      formatDateInfosDonnees = android.text.format.DateFormat.getDateFormat(context);
      formatTimeInfosDonnees = android.text.format.DateFormat.getTimeFormat(context);
      formatDateMessageFfvl = android.text.format.DateFormat.getDateFormat(context);

      if (initialized)
      {
        return;
      }

      // Gestion des exceptions
      initExceptionHandler(context);

      // Initialisations
      resources = context.getResources();
      sharedPreferences = getSharedPreferences(context);

      debugMode = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) > 0;

      // Dialogues
      initAlertDialogHandler();
      initConfirmDialogHandler();
      initProgressDialogHandler();

      // Preferences unites
      updateUnitPreferences(context, null, null, null, null);

      // Fichiers de revelation des providers
      initBaliseProviderShowFiles();

      // Flag
      initialized = true;
    }
  }

  /**
   * 
   */
  public static void initFromActivity()
  {
    initBackKeyManager();
  }

  /**
   * 
   * @param intentAction
   */
  public static void saveStartActivity(final String intentAction)
  {
    final SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putString(AbstractBalisesPreferencesActivity.CONFIG_START_ACTIVITY, intentAction);
    commitPreferences(editor);
  }

  /**
   * 
   * @return
   */
  public static BalisesExceptionHandler initExceptionHandler(final Context context)
  {
    // Initialisations
    BalisesExceptionHandler exceptionHandler;
    final UncaughtExceptionHandler ueh = Thread.getDefaultUncaughtExceptionHandler();

    if ((ueh != null) && BalisesExceptionHandler.class.isAssignableFrom(ueh.getClass()))
    {
      // Deja renseigne
      exceptionHandler = (BalisesExceptionHandler)ueh;
    }
    else
    {
      // Renseignement
      exceptionHandler = new BalisesExceptionHandler(context, ueh);
      Thread.setDefaultUncaughtExceptionHandler(exceptionHandler);
    }

    return exceptionHandler;
  }

  /**
   * 
   * @param timestamp
   * @param buffer
   */
  private static void dataInfosFormatDate(final long timestamp, final StringBuilder buffer)
  {
    if (timestamp > 0)
    {
      final Date date = new Date(timestamp);
      date.setTime(timestamp); // Un bug JVM ?
      buffer.append(formatDateInfosDonnees.format(date));
      buffer.append(Strings.CHAR_SPACE);
      buffer.append(formatTimeInfosDonnees.format(date));
    }
    else
    {
      buffer.append(STRING_TROIS_POINTS_INTERROGATION);
    }
  }

  /**
   * 
   */
  private static void initAlertDialogHandler()
  {
    alertDialogHandler = new Handler(Looper.getMainLooper())
    {
      @Override
      public void handleMessage(final Message msg)
      {
        final int dialogId = msg.arg1;
        switch (msg.what)
        {
          case DIALOG_SHOW:
            synchronized (alertDialogs)
            {
              if (alertDialogs.get(dialogId) == null)
              {
                final Activity activity = (Activity)((Object[])msg.obj)[0];
                final String msgTitle = (String)((Object[])msg.obj)[1];
                final String msgMessage = (String)((Object[])msg.obj)[2];
                final DialogInterface.OnClickListener closeButtonListener = (DialogInterface.OnClickListener)((Object[])msg.obj)[3];
                final Boolean cancelable = (Boolean)((Object[])msg.obj)[4];
                final DialogInterface.OnCancelListener onCancelListener = (DialogInterface.OnCancelListener)((Object[])msg.obj)[5];
                final int linkify = ((Integer)((Object[])msg.obj)[6]).intValue();
                final int icon = ((Integer)((Object[])msg.obj)[7]).intValue();
                final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(msgTitle);
                builder.setIcon(icon <= 0 ? R.drawable.icon : icon);
                builder.setCancelable(cancelable == null ? false : cancelable.booleanValue());

                // TextView personnalise
                final View view = LayoutInflater.from(activity).inflate(R.layout.alert_dialog, null);
                final TextView textView = (TextView)view.findViewById(R.id.alert_dialog_text);
                textView.setAutoLinkMask(linkify);
                textView.setText(Html.fromHtml(Strings.toHtmlString(msgMessage)));
                builder.setView(view);

                builder.setNegativeButton(resources.getString(R.string.button_close), new DialogInterface.OnClickListener()
                {
                  @Override
                  public void onClick(final DialogInterface dialog, final int id)
                  {
                    if (closeButtonListener != null)
                    {
                      closeButtonListener.onClick(dialog, id);
                    }
                    dismissAlertDialog(dialogId);
                  }
                });
                if (onCancelListener != null)
                {
                  builder.setOnCancelListener(onCancelListener);
                }

                if (!activity.isFinishing())
                {
                  final AlertDialog alertDialog = builder.create();
                  alertDialog.show();
                  registerAlertDialog(dialogId, alertDialog, null);
                }
              }
            }
            break;

          case DIALOG_CANCEL:
            dismissAlertDialog(dialogId);
            break;
        }
      }
    };
  }

  /**
   * 
   */
  private static void initConfirmDialogHandler()
  {
    confirmDialogHandler = new Handler(Looper.getMainLooper())
    {
      @Override
      public void handleMessage(final Message msg)
      {
        final int dialogId = msg.arg1;
        switch (msg.what)
        {
          case DIALOG_SHOW:
            synchronized (alertDialogs)
            {
              if (alertDialogs.get(dialogId) == null)
              {
                final Activity activity = (Activity)((Object[])msg.obj)[0];
                final String msgTitle = (String)((Object[])msg.obj)[1];
                final String msgMessage = (String)((Object[])msg.obj)[2];
                final DialogInterface.OnClickListener okButtonListener = (DialogInterface.OnClickListener)((Object[])msg.obj)[3];
                final DialogInterface.OnClickListener middleButtonListener = (DialogInterface.OnClickListener)((Object[])msg.obj)[4];
                final DialogInterface.OnClickListener cancelButtonListener = (DialogInterface.OnClickListener)((Object[])msg.obj)[5];
                final Boolean cancelable = (Boolean)((Object[])msg.obj)[6];
                final DialogInterface.OnCancelListener onCancelListener = (DialogInterface.OnCancelListener)((Object[])msg.obj)[7];
                final Integer okButtonInteger = (Integer)((Object[])msg.obj)[8];
                final int okButtonId = (okButtonInteger == null ? R.string.button_ok : (okButtonInteger.intValue() <= 0 ? R.string.button_ok : okButtonInteger.intValue()));
                final Integer middleButtonInteger = (Integer)((Object[])msg.obj)[9];
                final int middleButtonId = (middleButtonInteger == null ? -1 : middleButtonInteger.intValue());
                final Integer cancelButtonInteger = (Integer)((Object[])msg.obj)[10];
                final int cancelButtonId = (cancelButtonInteger == null ? R.string.button_cancel : (cancelButtonInteger.intValue() <= 0 ? R.string.button_cancel : cancelButtonInteger.intValue()));
                final String[] choiceItems = (String[])((Object[])msg.obj)[11];
                final boolean[] checkedItems = (boolean[])((Object[])msg.obj)[12];
                final OnMultiChoiceClickListener itemsListener = (OnMultiChoiceClickListener)((Object[])msg.obj)[13];
                final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(msgTitle);
                builder.setIcon(R.drawable.icon);
                if (msgMessage != null)
                {
                  builder.setMessage(msgMessage);
                }
                builder.setCancelable(cancelable == null ? false : cancelable.booleanValue());
                if ((choiceItems != null) && (checkedItems != null))
                {
                  builder.setMultiChoiceItems(choiceItems, checkedItems, itemsListener);
                }
                builder.setPositiveButton(resources.getString(okButtonId), new DialogInterface.OnClickListener()
                {
                  @Override
                  public void onClick(final DialogInterface dialog, final int id)
                  {
                    if (okButtonListener != null)
                    {
                      okButtonListener.onClick(dialog, id);
                    }
                    dismissConfirmDialog(msg.arg1);
                  }
                });
                if (middleButtonInteger != null)
                {
                  builder.setNeutralButton(resources.getString(middleButtonId), new DialogInterface.OnClickListener()
                  {
                    @Override
                    public void onClick(final DialogInterface dialog, final int id)
                    {
                      if (middleButtonListener != null)
                      {
                        middleButtonListener.onClick(dialog, id);
                      }
                      dismissConfirmDialog(dialogId);
                    }
                  });
                }
                builder.setNegativeButton(resources.getString(cancelButtonId), new DialogInterface.OnClickListener()
                {
                  @Override
                  public void onClick(final DialogInterface dialog, final int id)
                  {
                    if (cancelButtonListener != null)
                    {
                      cancelButtonListener.onClick(dialog, id);
                    }
                    dismissConfirmDialog(dialogId);
                  }
                });
                if (onCancelListener != null)
                {
                  builder.setOnCancelListener(onCancelListener);
                }

                if (!activity.isFinishing())
                {
                  final AlertDialog confirmDialog = builder.create();
                  confirmDialog.show();
                  registerConfirmDialog(dialogId, confirmDialog, null);
                }
              }
            }
            break;

          case DIALOG_CANCEL:
            dismissConfirmDialog(dialogId);
            break;
        }
      }
    };
  }

  /**
   * 
   */
  private static void initProgressDialogHandler()
  {
    progressDialogHandler = new Handler(Looper.getMainLooper())
    {
      @Override
      public void handleMessage(final Message msg)
      {
        final int dialogId = msg.arg1;
        switch (msg.what)
        {
          case DIALOG_SHOW:
            synchronized (progressDialogs)
            {
              if (progressDialogs.get(dialogId) == null)
              {
                final Activity activity = (Activity)((Object[])msg.obj)[0];
                final String msgTitle = (String)((Object[])msg.obj)[1];
                final String msgMessage = (String)((Object[])msg.obj)[2];
                final Boolean indeterminate = (Boolean)((Object[])msg.obj)[3];
                final Boolean cancelable = (Boolean)((Object[])msg.obj)[4];
                final DialogInterface.OnCancelListener onCancelListener = (DialogInterface.OnCancelListener)((Object[])msg.obj)[5];
                if (!activity.isFinishing())
                {
                  final ProgressDialog progressDialog = new ProgressDialog(activity);
                  progressDialog.setTitle(msgTitle);
                  progressDialog.setMessage(msgMessage);
                  progressDialog.setIndeterminate(indeterminate.booleanValue());
                  progressDialog.setCancelable(cancelable.booleanValue());
                  progressDialog.setOnCancelListener(onCancelListener);
                  progressDialog.show();
                  registerProgressDialog(dialogId, progressDialog, null);
                }
              }
            }
            break;

          case DIALOG_CANCEL:
            dismissProgressDialog(dialogId);
            break;
        }
      }
    };
  }

  /**
   * 
   * @param dialogId
   * @param progressDialog
   * @param dismissListener
   */
  public static void registerProgressDialog(final int dialogId, final ProgressDialog progressDialog, final OnDismissListener dismissListener)
  {
    synchronized (progressDialogs)
    {
      progressDialog.setOnDismissListener(new OnDismissListener()
      {
        @Override
        public void onDismiss(final DialogInterface dialog)
        {
          if (dismissListener != null)
          {
            dismissListener.onDismiss(dialog);
          }
          unregisterProgressDialog(dialogId);
        }
      });
      progressDialogs.put(dialogId, progressDialog);
    }
  }

  /**
   * 
   * @param dialogId
   */
  public static void unregisterProgressDialog(final int dialogId)
  {
    synchronized (progressDialogs)
    {
      progressDialogs.remove(dialogId);
    }
  }

  /**
   * 
   * @param dialogId
   * @param alertDialog
   * @param dismissListener
   */
  public static void registerAlertDialog(final int dialogId, final AlertDialog alertDialog, final OnDismissListener dismissListener)
  {
    synchronized (alertDialogs)
    {
      alertDialog.setOnDismissListener(new OnDismissListener()
      {
        @Override
        public void onDismiss(final DialogInterface dialog)
        {
          if (dismissListener != null)
          {
            dismissListener.onDismiss(dialog);
          }
          unregisterAlertDialog(dialogId);
        }
      });
      alertDialogs.put(dialogId, alertDialog);
    }
  }

  /**
   * 
   * @param dialogId
   */
  public static void unregisterAlertDialog(final int dialogId)
  {
    synchronized (alertDialogs)
    {
      alertDialogs.remove(dialogId);
    }
  }

  /**
   * 
   * @param dialogId
   * @param confirmDialog
   * @param dismissListener
   */
  public static void registerConfirmDialog(final int dialogId, final AlertDialog confirmDialog, final OnDismissListener dismissListener)
  {
    registerAlertDialog(dialogId, confirmDialog, dismissListener);
  }

  /**
   * 
   * @param dialogId
   */
  public static void unregisterConfirmDialog(final int dialogId)
  {
    unregisterAlertDialog(dialogId);
  }

  /**
   * 
   * @param dialogId
   */
  static void dismissAlertDialog(final int dialogId)
  {
    synchronized (alertDialogs)
    {
      final AlertDialog alertDialog = (AlertDialog)alertDialogs.get(dialogId);
      if (alertDialog != null)
      {
        if (alertDialog.isShowing())
        {
          alertDialog.dismiss();
        }
      }
    }
  }

  /**
   * 
   * @param id
   */
  static void dismissConfirmDialog(final int id)
  {
    dismissAlertDialog(id);
  }

  /**
   * 
   * @param dialogId
   */
  static void dismissProgressDialog(final int dialogId)
  {
    synchronized (progressDialogs)
    {
      final ProgressDialog progressDialog = (ProgressDialog)progressDialogs.get(dialogId);
      if (progressDialog != null)
      {
        if (progressDialog.isShowing())
        {
          progressDialog.dismiss();
        }
      }
    }
  }

  /**
   * 
   * @param activity
   * @param id
   * @param icon
   * @param title
   * @param message
   * @param closeButtonListener
   * @param cancelable
   * @param onCancelListener
   * @param linkify
   */
  public static void alertDialog(final Activity activity, final int id, final int icon, final String title, final String message, final DialogInterface.OnClickListener closeButtonListener, final boolean cancelable,
      final DialogInterface.OnCancelListener onCancelListener, final int linkify)
  {
    final Message msg = new Message();
    msg.what = DIALOG_SHOW;
    msg.arg1 = id;
    msg.obj = new Object[] { activity, title, message, closeButtonListener, Boolean.valueOf(cancelable), onCancelListener, Integer.valueOf(linkify), Integer.valueOf(icon) };
    alertDialogHandler.sendMessage(msg);
  }

  /**
   * 
   * @param activity
   * @param id
   * @param title
   * @param message
   * @param okButtonListener
   * @param cancelButtonListener
   * @param cancelable
   * @param onCancelListener
   * @param okButtonMessageId
   * @param cancelButtonMessageId
   * @param items
   * @param checkedItems
   * @param itemsListener
   */
  public static void confirmDialog(final Activity activity, final int id, final String title, final String message, final DialogInterface.OnClickListener okButtonListener, final DialogInterface.OnClickListener cancelButtonListener,
      final boolean cancelable, final DialogInterface.OnCancelListener onCancelListener, final int okButtonMessageId, final int cancelButtonMessageId, final String[] items, final boolean[] checkedItems,
      final OnMultiChoiceClickListener itemsListener)
  {
    final Message msg = new Message();
    msg.what = DIALOG_SHOW;
    msg.arg1 = id;
    msg.obj = new Object[] { activity, title, message, okButtonListener, null, cancelButtonListener, Boolean.valueOf(cancelable), onCancelListener, Integer.valueOf(okButtonMessageId), null, Integer.valueOf(cancelButtonMessageId), items,
        checkedItems, itemsListener };
    confirmDialogHandler.sendMessage(msg);
  }

  /**
   * 
   * @param activity
   * @param id
   * @param title
   * @param message
   * @param okButtonListener
   * @param middleButtonListener
   * @param cancelButtonListener
   * @param cancelable
   * @param onCancelListener
   * @param okButtonMessageId
   * @param middleButtonMessageId
   * @param cancelButtonMessageId
   */
  public static void choiceDialog(final Activity activity, final int id, final String title, final String message, final DialogInterface.OnClickListener okButtonListener, final DialogInterface.OnClickListener middleButtonListener,
      final DialogInterface.OnClickListener cancelButtonListener, final boolean cancelable, final DialogInterface.OnCancelListener onCancelListener, final int okButtonMessageId, final int middleButtonMessageId,
      final int cancelButtonMessageId)
  {
    final Message msg = new Message();
    msg.what = DIALOG_SHOW;
    msg.arg1 = id;
    msg.obj = new Object[] { activity, title, message, okButtonListener, middleButtonListener, cancelButtonListener, Boolean.valueOf(cancelable), onCancelListener, Integer.valueOf(okButtonMessageId), Integer.valueOf(middleButtonMessageId),
        Integer.valueOf(cancelButtonMessageId), null, null, null };
    confirmDialogHandler.sendMessage(msg);
  }

  /**
   * 
   * @param activity
   * @param id
   * @param title
   * @param message
   * @param indeterminate
   * @param cancelable
   * @param onCancelListener
   */
  public static void progressDialog(final Activity activity, final int id, final String title, final String message, final boolean indeterminate, final boolean cancelable, final DialogInterface.OnCancelListener onCancelListener)
  {
    // Listener de fermeture
    final DialogInterface.OnCancelListener finalOnCancelListener;
    if (!cancelable)
    {
      finalOnCancelListener = null;
    }
    else
    {
      finalOnCancelListener = new DialogInterface.OnCancelListener()
      {
        @Override
        public void onCancel(final DialogInterface dialog)
        {
          cancelProgressDialog(id);
          if (onCancelListener != null)
          {
            onCancelListener.onCancel(dialog);
          }
        }
      };
    }

    // Message pour ouvrir le dialog
    final Message msg = new Message();
    msg.what = DIALOG_SHOW;
    msg.arg1 = id;
    msg.obj = new Object[] { activity, title, message, Boolean.valueOf(indeterminate), Boolean.valueOf(cancelable), finalOnCancelListener };
    progressDialogHandler.sendMessage(msg);
  }

  /**
   * 
   * @param activity
   * @param id
   * @param indeterminate
   * @param cancelable
   * @param onCancelListener
   */
  public static void progressDialog(final Activity activity, final int id, final boolean indeterminate, final boolean cancelable, final DialogInterface.OnCancelListener onCancelListener)
  {
    progressDialog(activity, id, resources.getString(R.string.app_name), resources.getString(R.string.message_please_wait), indeterminate, cancelable, onCancelListener);
  }

  /**
   * 
   * @param id
   */
  public static void cancelProgressDialog(final int id)
  {
    cancelProgressDialog(id, false);
  }

  /**
   * 
   * @param id
   * @param directly
   */
  public static void cancelAlertDialog(final int id, final boolean directly)
  {
    if (directly)
    {
      // Directement
      dismissAlertDialog(id);
    }
    else
    {
      // Sinon par Handler
      final Message msg = new Message();
      msg.what = DIALOG_CANCEL;
      msg.arg1 = id;
      alertDialogHandler.sendMessage(msg);
    }
  }

  /**
   * 
   * @param id
   * @param directly
   */
  public static void cancelConfirmDialog(final int id, final boolean directly)
  {
    if (directly)
    {
      // Directement
      dismissConfirmDialog(id);
    }
    else
    {
      // Sinon par Handler
      final Message msg = new Message();
      msg.what = DIALOG_CANCEL;
      msg.arg1 = id;
      confirmDialogHandler.sendMessage(msg);
    }
  }

  /**
   * 
   * @param id
   * @param directly
   */
  public static void cancelProgressDialog(final int id, final boolean directly)
  {
    if (directly)
    {
      // Directement
      dismissProgressDialog(id);
    }
    else
    {
      // Sinon par Handler
      final Message msg = new Message();
      msg.what = DIALOG_CANCEL;
      msg.arg1 = id;
      progressDialogHandler.sendMessage(msg);
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class DataInfosCloser implements OnClickListener, OnCancelListener
  {
    /**
     * 
     */
    DataInfosCloser()
    {
      super();
    }

    /**
     * 
     */
    private static void release()
    {
      synchronized (dataInfosLock)
      {
        dataInfosVisible = false;
      }
    }

    @Override
    public void onCancel(final DialogInterface dialog)
    {
      release();
    }

    @Override
    public void onClick(final DialogInterface dialog, final int which)
    {
      release();
    }
  }

  /**
   * 
   */
  private static final void initBaliseProviderShowFiles()
  {
    baliseProviderShowFiles.clear();
    final String[] files = MOBIBALISES_EXTERNAL_STORAGE_PATH.list();
    if (files != null)
    {
      for (final String file : files)
      {
        if (file.endsWith(SHOW_FILE_EXTENSION))
        {
          baliseProviderShowFiles.add(file);
        }
      }
    }
  }

  /**
   * 
   * @param key
   * @param stringHidden
   * @return
   */
  public static final boolean isBaliseProviderHidden(final String key, final String stringHidden)
  {
    // Provider non cache...
    final boolean hidden = Boolean.parseBoolean(stringHidden);
    if (!hidden)
    {
      return false;
    }

    // Dans le cache ?
    final String providerShowFile = key + SHOW_FILE_EXTENSION;
    if (baliseProviderShowFiles.contains(providerShowFile))
    {
      return false;
    }

    // Fichier existant ?
    final File showFile = new File(MOBIBALISES_EXTERNAL_STORAGE_PATH, providerShowFile);
    if (showFile.exists())
    {
      baliseProviderShowFiles.add(providerShowFile);
      return false;
    }

    // Provider cache
    return true;
  }

  /**
   * 
   * @param activity
   * @param providersService
   */
  public static void dataInfos(final Activity activity, final IProvidersService providersService)
  {
    // Une seule fois !
    synchronized (dataInfosLock)
    {
      if (dataInfosVisible)
      {
        return;
      }

      dataInfosVisible = true;
    }

    // Initialisations
    final StringBuilder networkBuffer = new StringBuilder(64);
    final StringBuilder providersBuffer = new StringBuilder(256);

    // Etat du reseau
    if (networkOff)
    {
      networkBuffer.append(MessageFormat.format(HTML_FONT_COLOR_PREFIX, Strings.toHexColor(resources.getColor(R.color.data_infos_status_error))));
      networkBuffer.append(HTML_NETWORK_ERROR_PREFIX);
      networkBuffer.append(resources.getString(R.string.message_data_infos_nonetwork));
      networkBuffer.append(HTML_NETWORK_ERROR_SUFFIX);
      networkBuffer.append(HTML_FONT_COLOR_SUFFIX);
    }

    // Balises
    final boolean almostOneBaliseProvider = baliseDataInfos(activity, providersService, providersBuffer);

    // Webcams
    final boolean almostOneWebcamProvider = webcamDataInfos(activity, providersService, providersBuffer, almostOneBaliseProvider);

    // Message

    if (almostOneBaliseProvider || almostOneWebcamProvider)
    {
      alertDialog(activity, ALERT_DIALOG_DATA_INFOS, -1, resources.getString(R.string.message_data_infos_title), networkBuffer.toString() + providersBuffer.toString(), dataInfosCloser, true, dataInfosCloser, 0);
    }
    else
    {
      alertDialog(activity, ALERT_DIALOG_DATA_INFOS, -1, resources.getString(R.string.message_data_infos_title), networkBuffer.toString() + resources.getString(R.string.message_data_infos_noprovider), dataInfosCloser, true,
          dataInfosCloser, 0);
    }
  }

  /**
   * 
   * @param activity
   * @param providersService
   * @param providersBuffer
   */
  private static final boolean baliseDataInfos(final Activity activity, final IProvidersService providersService, final StringBuilder providersBuffer)
  {
    // Initialisations
    final String[] keys = resources.getStringArray(R.array.providers_keys);
    final String[] forDebugs = resources.getStringArray(R.array.providers_forDebugs);
    final String[] hiddens = resources.getStringArray(R.array.providers_hidden);

    // Pour chaque provider
    boolean almostOne = false;
    for (int i = 0; i < keys.length; i++)
    {
      // On ne prend en compte le provider que si mode debug ou non provider de debug et selectionne
      final boolean forDebug = Boolean.parseBoolean(forDebugs[i]);
      final boolean hidden = isBaliseProviderHidden(keys[i], hiddens[i]);
      if ((debugMode || !forDebug) && !hidden)
      {
        final List<String> countries = BaliseProviderUtils.getBaliseProviderActiveCountries(activity.getApplicationContext(), sharedPreferences, keys[i], i);
        for (final String country : countries)
        {
          // Initialisations
          final String fullKey = BaliseProviderUtils.getBaliseProviderFullKey(keys[i], country);
          final BaliseProviderInfos infos = providersService.getBaliseProviderInfos(fullKey);
          final BaliseProvider provider = providersService.getBaliseProvider(fullKey);
          final boolean providerError = ((infos != null) && !infos.isPaused() && ((infos.getRelevesException() != null) || (infos.getBalisesException() != null)));

          // Nom du provider
          if (almostOne)
          {
            providersBuffer.append(Strings.CHAR_NEWLINE);
            providersBuffer.append(Strings.CHAR_NEWLINE);
          }
          almostOne = true;
          providersBuffer.append(HTML_BIG_BOLD_UNDERLINE);
          providersBuffer.append(MessageFormat.format(HTML_FONT_COLOR_PREFIX, Strings.toHexColor(resources.getColor(providerError ? R.color.data_infos_status_error : R.color.data_infos_status_ok))));
          providersBuffer.append(infos == null ? Strings.TROIS_POINTS_INTERROGATION : infos.getName());
          providersBuffer.append(HTML_FONT_COLOR_SUFFIX);
          providersBuffer.append(HTML_BIG_BOLD_UNDERLINE_END);

          // Infos
          if (infos != null)
          {
            // Statut
            if (infos.isPaused())
            {
              providersBuffer.append(HTML_PROVIDER_STATUS_PREFIX);
              providersBuffer.append(resources.getString(R.string.message_data_infos_provider_paused));
              providersBuffer.append(HTML_PROVIDER_STATUS_SUFFIX);
            }
            else if (infos.isSleeping())
            {
              providersBuffer.append(HTML_PROVIDER_STATUS_PREFIX);
              final long millis = infos.getNextWakeUp() - System.currentTimeMillis();
              final long seconds = (millis / 1000) % 60;
              final long minutes = (long)Math.floor(millis / 60000);
              final StringBuilder timeBuffer = new StringBuilder(32);
              if (minutes > 0)
              {
                timeBuffer.append(minutes);
                timeBuffer.append(resources.getString(R.string.unit_minutes_abv));
              }
              timeBuffer.append(seconds);
              timeBuffer.append(resources.getString(R.string.unit_seconds_abv));
              providersBuffer.append(resources.getString(R.string.message_data_infos_provider_sleeping, timeBuffer.toString()));
              providersBuffer.append(HTML_PROVIDER_STATUS_SUFFIX);
            }
            else if (infos.isUpdateInProgress())
            {
              providersBuffer.append(HTML_PROVIDER_STATUS_PREFIX);
              providersBuffer.append(resources.getString(R.string.message_data_infos_provider_in_progress));
              providersBuffer.append(HTML_PROVIDER_STATUS_SUFFIX);
            }

            // Releves
            providersBuffer.append(HTML_RELEVES_BALISES_PREFIX);
            providersBuffer.append(resources.getString(R.string.message_data_infos_releves));
            providersBuffer.append(HTML_RELEVES_BALISES_SUFFIX);
            providersBuffer.append(HTML_INFO_LINE_PREFIX);
            dataInfosFormatDate(Utils.fromUTC(infos.getLastRelevesUpdateDate()), providersBuffer);
            providersBuffer.append(HTML_LINE_TYPE_PREFIX);
            providersBuffer.append(resources.getString(R.string.message_data_infos_line_server));
            providersBuffer.append(HTML_LINE_TYPE_SUFFIX);
            providersBuffer.append(HTML_INFO_LINE_PREFIX);
            dataInfosFormatDate(infos.getLastRelevesUpdateLocalDate(), providersBuffer);
            providersBuffer.append(HTML_LINE_TYPE_PREFIX);
            providersBuffer.append(resources.getString(R.string.message_data_infos_line_last_update));
            providersBuffer.append(HTML_LINE_TYPE_SUFFIX);
            providersBuffer.append(HTML_INFO_LINE_PREFIX);
            dataInfosFormatDate(infos.getLastRelevesCheckLocalDate(), providersBuffer);
            providersBuffer.append(HTML_LINE_TYPE_PREFIX);
            providersBuffer.append(resources.getString(R.string.message_data_infos_line_last_check));
            providersBuffer.append(HTML_LINE_TYPE_SUFFIX);
            if (!infos.isPaused() && (infos.getRelevesException() != null))
            {
              providersBuffer.append(HTML_INFO_LINE_PREFIX);
              providersBuffer.append(MessageFormat.format(HTML_FONT_COLOR_PREFIX, Strings.toHexColor(resources.getColor(R.color.data_infos_status_error))));
              providersBuffer.append(HTML_ERROR_PREFIX);
              providersBuffer.append(provider.filterExceptionMessage(infos.getRelevesException().getMessage()));
              providersBuffer.append(HTML_ERROR_SUFFIX);
              providersBuffer.append(HTML_FONT_COLOR_SUFFIX);
            }

            // Balises
            providersBuffer.append(HTML_RELEVES_BALISES_PREFIX);
            providersBuffer.append(resources.getString(R.string.message_data_infos_balises));
            providersBuffer.append(HTML_RELEVES_BALISES_SUFFIX);
            providersBuffer.append(HTML_INFO_LINE_PREFIX);
            dataInfosFormatDate(Utils.fromUTC(infos.getLastBalisesUpdateDate()), providersBuffer);
            providersBuffer.append(HTML_LINE_TYPE_PREFIX);
            providersBuffer.append(resources.getString(R.string.message_data_infos_line_server));
            providersBuffer.append(HTML_LINE_TYPE_SUFFIX);
            providersBuffer.append(HTML_INFO_LINE_PREFIX);
            dataInfosFormatDate(infos.getLastBalisesUpdateLocalDate(), providersBuffer);
            providersBuffer.append(HTML_LINE_TYPE_PREFIX);
            providersBuffer.append(resources.getString(R.string.message_data_infos_line_last_update));
            providersBuffer.append(HTML_LINE_TYPE_SUFFIX);
            providersBuffer.append(HTML_INFO_LINE_PREFIX);
            dataInfosFormatDate(infos.getLastBalisesCheckLocalDate(), providersBuffer);
            providersBuffer.append(HTML_LINE_TYPE_PREFIX);
            providersBuffer.append(resources.getString(R.string.message_data_infos_line_last_check));
            providersBuffer.append(HTML_LINE_TYPE_SUFFIX);
            if (!infos.isPaused() && (infos.getBalisesException() != null))
            {
              providersBuffer.append(HTML_INFO_LINE_PREFIX);
              providersBuffer.append(MessageFormat.format(HTML_FONT_COLOR_PREFIX, Strings.toHexColor(resources.getColor(R.color.data_infos_status_error))));
              providersBuffer.append(HTML_ERROR_PREFIX);
              providersBuffer.append(provider.filterExceptionMessage(infos.getBalisesException().getMessage()));
              providersBuffer.append(HTML_ERROR_SUFFIX);
              providersBuffer.append(HTML_FONT_COLOR_SUFFIX);
            }
          }
          else
          {
            providersBuffer.append(resources.getString(R.string.message_data_infos_nodata));
          }
        }
      }
    }

    return almostOne;
  }

  /**
   * 
   * @param activity
   * @param providersService
   * @param providersBuffer
   * @param almostOneBefore
   */
  private static final boolean webcamDataInfos(final Activity activity, final IProvidersService providersService, final StringBuilder providersBuffer, final boolean almostOneBefore)
  {
    // Initialisations
    final String[] keys = resources.getStringArray(R.array.webcam_providers_keys);

    // Pour chaque provider
    boolean almostOne = false;
    for (int i = 0; i < keys.length; i++)
    {
      // Initialisations
      final String key = keys[i];
      final WebcamProviderInfos infos = providersService.getWebcamProviderInfos(key);
      final boolean providerError = ((infos != null) && (infos.getWebcamsException() != null));

      // Nom du provider
      if (almostOne || almostOneBefore)
      {
        providersBuffer.append(Strings.CHAR_NEWLINE);
        providersBuffer.append(Strings.CHAR_NEWLINE);
      }
      almostOne = true;
      providersBuffer.append(HTML_BIG_BOLD_UNDERLINE);
      providersBuffer.append(MessageFormat.format(HTML_FONT_COLOR_PREFIX, Strings.toHexColor(resources.getColor(providerError ? R.color.data_infos_status_error : R.color.data_infos_status_ok))));
      providersBuffer.append(resources.getString(R.string.message_data_infos_webcams));
      providersBuffer.append(HTML_FONT_COLOR_SUFFIX);
      providersBuffer.append(HTML_BIG_BOLD_UNDERLINE_END);

      // Infos
      if (infos != null)
      {
        // Statut
        if (infos.isPaused())
        {
          providersBuffer.append(HTML_PROVIDER_STATUS_PREFIX);
          providersBuffer.append(resources.getString(R.string.message_data_infos_provider_paused));
          providersBuffer.append(HTML_PROVIDER_STATUS_SUFFIX);
        }
        else if (infos.isSleeping())
        {
          providersBuffer.append(HTML_PROVIDER_STATUS_PREFIX);
          final long millis = infos.getNextWakeUp() - System.currentTimeMillis();
          final long seconds = (millis / 1000) % 60;
          final long minutes = (millis / 60000) % 60;
          final long hours = (long)Math.floor(millis / 3600000);
          final StringBuilder timeBuffer = new StringBuilder(32);
          if (hours > 0)
          {
            timeBuffer.append(hours);
            timeBuffer.append(resources.getString(R.string.unit_hours_abv));
          }
          if (minutes > 0)
          {
            timeBuffer.append(minutes);
            timeBuffer.append(resources.getString(R.string.unit_minutes_abv));
          }
          if (hours == 0)
          {
            // Affichage des secondes seulement  si les heures sont à zéro
            timeBuffer.append(seconds);
            timeBuffer.append(resources.getString(R.string.unit_seconds_abv));
          }
          providersBuffer.append(resources.getString(R.string.message_data_infos_provider_sleeping, timeBuffer.toString()));
          providersBuffer.append(HTML_PROVIDER_STATUS_SUFFIX);
        }
        else if (infos.isUpdateInProgress())
        {
          providersBuffer.append(HTML_PROVIDER_STATUS_PREFIX);
          providersBuffer.append(resources.getString(R.string.message_data_infos_provider_in_progress));
          providersBuffer.append(HTML_PROVIDER_STATUS_SUFFIX);
        }

        // Webcams
        providersBuffer.append(HTML_INFO_LINE_PREFIX);
        dataInfosFormatDate(infos.getLastWebcamsUpdateLocalDate(), providersBuffer);
        providersBuffer.append(HTML_LINE_TYPE_PREFIX);
        providersBuffer.append(resources.getString(R.string.message_data_infos_line_last_update));
        providersBuffer.append(HTML_LINE_TYPE_SUFFIX);
        providersBuffer.append(HTML_INFO_LINE_PREFIX);
        dataInfosFormatDate(infos.getLastWebcamsCheckLocalDate(), providersBuffer);
        providersBuffer.append(HTML_LINE_TYPE_PREFIX);
        providersBuffer.append(resources.getString(R.string.message_data_infos_line_last_check));
        providersBuffer.append(HTML_LINE_TYPE_SUFFIX);
        if (!infos.isPaused() && (infos.getWebcamsException() != null))
        {
          providersBuffer.append(HTML_INFO_LINE_PREFIX);
          providersBuffer.append(MessageFormat.format(HTML_FONT_COLOR_PREFIX, Strings.toHexColor(resources.getColor(R.color.data_infos_status_error))));
          providersBuffer.append(HTML_ERROR_PREFIX);
          providersBuffer.append(infos.getWebcamsException().getMessage());
          providersBuffer.append(HTML_ERROR_SUFFIX);
          providersBuffer.append(HTML_FONT_COLOR_SUFFIX);
        }
      }
      else
      {
        providersBuffer.append(resources.getString(R.string.message_data_infos_nodata));
      }
    }

    return almostOne;
  }

  /**
   * 
   * @param activity
   * @param tileCacheSize
   * @param mapZoom
   * @param mode
   */
  public static void preferences(final Activity activity, final long tileCacheSize, final int mapZoom, final int mode)
  {
    // Initialisations
    final Intent intent = new Intent(activity.getString(R.string.intent_preferences_action));

    // Transmission de la taille utilisee par le cache
    if (tileCacheSize >= 0)
    {
      intent.putExtra(activity.getPackageName() + AbstractBalisesPreferencesActivity.INTENT_CACHE_CURRENT_SIZE, tileCacheSize);
    }

    // Transmission du niveau de zoom
    intent.putExtra(activity.getPackageName() + AbstractBalisesPreferencesActivity.INTENT_ZOOM_LEVEL, mapZoom);

    // Transmission du mode
    intent.putExtra(activity.getPackageName() + AbstractBalisesPreferencesActivity.INTENT_MODE, mode);

    // Demarrage
    activity.startActivity(intent);
  }

  /**
   * 
   * @param activity
   */
  public static void about(final Activity activity)
  {
    // Initialisations
    final Dialog dialog = new Dialog(activity);
    dialog.setContentView(R.layout.about);

    // Titre
    dialog.setTitle(R.string.app_name);

    // Version
    String version = resources.getString(R.string.app_version);
    final String buildDate = getBuildDate(activity);
    if (buildDate != null)
    {
      version = version.concat(" (").concat(buildDate).concat(")");
    }
    if (debugMode)
    {
      version = version.concat(Strings.SPACE).concat(resources.getString(R.string.app_version_debug));
    }

    // ID du terminal
    final String devId = getDeviceId(activity.getApplicationContext());

    // Texte
    final TextView text = (TextView)dialog.findViewById(R.id.about_text);
    text.setAutoLinkMask(Linkify.WEB_URLS + Linkify.EMAIL_ADDRESSES);

    // Elaboration du message
    final StringBuilder message = new StringBuilder(256);
    final String[] messages = resources.getStringArray(R.array.messages_about);
    for (int i = 0; i < messages.length; i++)
    {
      if (i != 0)
      {
        message.append(Strings.CHAR_NEWLINE);
      }

      if (i == 0)
      {
        // Version
        message.append(MessageFormat.format(messages[i], version));
      }
      else if (i == messages.length - 1)
      {
        // ID
        message.append(MessageFormat.format(messages[i], devId));
      }
      else
      {
        message.append(messages[i]);
      }
    }

    // RAM
    if (debugMode)
    {
      final String[] ram = logHeap();
      message.append(Strings.CHAR_NEWLINE).append(Strings.CHAR_NEWLINE);
      message.append("Heap : ");
      message.append(ram[0]);
      message.append(Strings.CHAR_NEWLINE);
      message.append("Mem : ");
      message.append(ram[1]);
    }

    // Affichage
    text.setText(message.toString());
    dialog.show();
  }

  /**
   * 
   * @param activity
   * @param url
   */
  public static void goToUrl(final Activity activity, final String url)
  {
    final Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.parse(url));
    activity.startActivity(intent);
  }

  /**
   * 
   * @param activity
   * @param force
   */
  public static void displayWhatsNewMessage(final Activity activity, final boolean force)
  {
    // Derniere version utilisee
    final String lastVersion = sharedPreferences.getString(AbstractBalisesPreferencesActivity.CONFIG_LAST_VERSION_USED, null);

    // Si premier lancement de la version (ou premiere installation)
    final boolean newVersion = (lastVersion == null) || (!lastVersion.equals(resources.getString(R.string.app_version)));
    if (force || newVersion)
    {
      // Message
      final StringBuilder message = new StringBuilder(256);
      message.append(HTML_WHATS_NEW_PREFIX);
      final String[] messages = resources.getStringArray(R.array.messages_whats_new);
      for (int i = 0; i < messages.length; i++)
      {
        if (i > 0)
        {
          message.append(Strings.NEWLINE);
        }
        message.append(HTML_WHATS_NEW_ITEM_PREFIX);
        message.append(messages[i]);
      }
      message.append(HTML_WHATS_NEW_SUFFIX);

      // Dialogue
      alertDialog(activity, ALERT_DIALOG_WHATS_NEW, -1, resources.getString(R.string.message_whats_new_title), message.toString(), null, true, null, Linkify.ALL);

      // Sauvegarde de la derniere version utilisee
      if (newVersion)
      {
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(AbstractBalisesPreferencesActivity.CONFIG_LAST_VERSION_USED, resources.getString(R.string.app_version));
        commitPreferences(editor);
      }
    }
  }

  /**
   * 
   * @param activity
   * @param tileCacheSize
   */
  public static void displayStartupMessage(final Activity activity, final long tileCacheSize)
  {
    // Derniere version utilisee
    final String lastVersion = sharedPreferences.getString(AbstractBalisesPreferencesActivity.CONFIG_LAST_VERSION_USED, null);

    // Si premier lancement de la version (ou premiere installation)
    if (lastVersion == null)
    {
      // Message d'info pour configurer les sources de donnees
      final StringBuilder message = new StringBuilder(256);
      message.append(HTML_FIRST_LAUNCH_PREFIX);
      message.append(resources.getString(R.string.message_first_launch, resources.getString(R.string.app_name)));
      message.append(HTML_FIRST_LAUNCH_SUFFIX);
      alertDialog(activity, ALERT_DIALOG_STARTUP, -1, resources.getString(R.string.app_name), message.toString(), new OnClickListener()
      {
        @Override
        public void onClick(final DialogInterface dialog, final int wich)
        {
          final int zoom = sharedPreferences.getInt(AbstractBalisesPreferencesActivity.CONFIG_ZOOM, 9);
          ActivityCommons.preferences(activity, tileCacheSize, zoom, AbstractBalisesPreferencesActivity.INTENT_MODE_BALISES_SOURCES);
        }
      }, true, null, Linkify.ALL);
    }

    // Affichage de la boite de dialogue "quoi de neuf" si besoin
    displayWhatsNewMessage(activity, false);
  }

  /**
   * 
   */
  public static void hideBackKeyToast()
  {
    synchronized (backKeyTimerLock)
    {
      if (backKeyTimerStarted)
      {
        backKeyTimerStarted = false;
        backKeyToast.cancel();
      }
    }
  }

  /**
   * 
   */
  private static void initBackKeyManager()
  {
    synchronized (backKeyTimerLock)
    {
      if (backKeyManagerInitialized)
      {
        return;
      }

      backKeyTimer = new Timer(AbstractBalisesMapActivity.class.getName() + ".backKeyTimer");
      backKeyManagerInitialized = true;
    }
  }

  /**
   * 
   */
  public static void finishBackKeyManager()
  {
    synchronized (backKeyTimerLock)
    {
      if (backKeyTimer != null)
      {
        backKeyTimer.cancel();
        backKeyTimer = null;
      }

      hideBackKeyToast();
      backKeyToast = null;

      backKeyManagerInitialized = false;
    }
  }

  /**
   * 
   */
  public static void manageDoubleBackKeyDown()
  {
    backKeyPressed = true;
  }

  /**
   * 
   * @param activity
   */
  public static void manageDoubleBackKeyUp(final Activity activity)
  {
    // Gestion du timer pour quitter
    synchronized (backKeyTimerLock)
    {
      if (activity.isFinishing())
      {
        finishBackKeyManager();
        return;
      }

      if (backKeyTimerStarted)
      {
        hideBackKeyToast();
        finishBackKeyManager();
        activity.finish();
      }
      else if (backKeyPressed)
      {
        // Sinon, gestion du timer pour quitter
        backKeyTimerStarted = true;
        backKeyToast = Toast.makeText(activity.getApplicationContext(), resources.getString(R.string.message_confirm_quit), Toast.LENGTH_SHORT);
        backKeyToast.show();
        if (backKeyTimer != null)
        {
          backKeyTimer.schedule(new TimerTask()
          {
            @Override
            public void run()
            {
              synchronized (backKeyTimerLock)
              {
                backKeyTimerStarted = false;
              }
            }
          }, DELAI_TIMER_CONFIRM_QUIT);
        }
      }
    }

    backKeyPressed = false;
  }

  /**
   * 
   */
  public static void releaseWakeLock()
  {
    if ((wakeLock != null) && wakeLock.isHeld())
    {
      wakeLock.release();
    }
  }

  /**
   * 
   */
  public static void acquireWakeLock()
  {
    if ((wakeLock != null) && !wakeLock.isHeld())
    {
      wakeLock.acquire();
    }
  }

  /**
   * 
   * @param context
   * @param flightMode
   */
  public static void manageWakeLockConfig(final Context context, final boolean flightMode)
  {
    // Initialisation si besoin
    init(context);

    // Liberation du lock si existant
    if (wakeLock != null)
    {
      if (wakeLock.isHeld())
      {
        wakeLock.release();
      }
      wakeLock = null;
    }

    // Creation du nouveau lock si besoin
    final boolean checked = sharedPreferences.getBoolean(resources.getString(R.string.config_wakelock_key), Boolean.parseBoolean(resources.getString(R.string.config_wakelock_default)));
    final boolean flightModeOnly = sharedPreferences.getBoolean(resources.getString(R.string.config_wakelock_flight_mode_key), Boolean.parseBoolean(resources.getString(R.string.config_wakelock_flight_mode_default)));
    if (checked && (flightMode || !flightModeOnly))
    {
      final boolean bright = sharedPreferences.getBoolean(resources.getString(R.string.config_wakelockbright_key), Boolean.parseBoolean(resources.getString(R.string.config_wakelockbright_default)));
      final int flags = PowerManager.ON_AFTER_RELEASE + (bright ? PowerManager.SCREEN_BRIGHT_WAKE_LOCK : PowerManager.SCREEN_DIM_WAKE_LOCK);
      final PowerManager powerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
      wakeLock = powerManager.newWakeLock(flags, WAKELOCK_TAG);
    }
  }

  /**
   * 
   * @return
   */
  public static String getStatusMessage()
  {
    return statusMessage;
  }

  /**
   * 
   * @param context
   * @param providerKeys
   * @return
   */
  public static ProvidersStatus getProvidersStatus(final Context context, final List<String> providerKeys)
  {
    synchronized (initLock)
    {
      // Si l'initialisation n'est pas faite
      if (!initialized)
      {
        return ProvidersStatus.OK;
      }

      // Si le reseau n'est pas dispo => ERREUR quoi qu'il en soit
      if (networkOff)
      {
        return ProvidersStatus.ERROR;
      }

      // Recuperation des resources
      final String[] keys = resources.getStringArray(R.array.providers_keys);
      final String[] forDebugs = resources.getStringArray(R.array.providers_forDebugs);
      final String[] hiddens = resources.getStringArray(R.array.providers_hidden);

      // Pour chaque provider
      boolean existsOk = false;
      boolean existsKo = false;
      for (int i = 0; i < keys.length; i++)
      {
        final boolean forDebug = Boolean.parseBoolean(forDebugs[i]);
        final boolean hidden = isBaliseProviderHidden(keys[i], hiddens[i]);
        if ((debugMode || !forDebug) && !hidden)
        {
          final List<String> countries = BaliseProviderUtils.getBaliseProviderActiveCountries(context, sharedPreferences, keys[i], i);
          for (final String country : countries)
          {
            final String fullKey = BaliseProviderUtils.getBaliseProviderFullKey(keys[i], country);
            if ((providerKeys == null) || (providerKeys.size() == 0) || providerKeys.contains(fullKey))
            {
              // Exception ?
              if (statusExceptionsKeyList.contains(fullKey))
              {
                existsKo = true;
              }
              else
              {
                existsOk = true;
              }
            }
          }
        }
      }

      return (existsOk ? (existsKo ? ProvidersStatus.WARNING : ProvidersStatus.OK) : (existsKo ? ProvidersStatus.ERROR : ProvidersStatus.OK));
    }
  }

  /**
   * 
   */
  private static void calculateStatusMessage()
  {
    // Plus rien...
    if (statusMessageNamesList.size() == 0)
    {
      statusMessage = null;
      return;
    }

    // Initialisations
    final StringBuilder buffer = new StringBuilder(64);
    String separator = Strings.VIDE;
    for (final String name : statusMessageNamesList)
    {
      buffer.append(separator);
      buffer.append(name);

      separator = STATUS_MESSAGE_SEPARATOR;
    }

    // Message
    statusMessage = resources.getString(R.string.message_loading_providers, buffer.toString());
  }

  /**
   * 
   * @param providerName
   * @return
   */
  public static boolean addStatusMessageProviderName(final String providerName)
  {
    synchronized (statusMessageLock)
    {
      if (!statusMessageNamesList.contains(providerName))
      {
        // Enregistrement
        final boolean added = statusMessageNamesList.add(providerName);

        if (added)
        {
          // Elaboration du message
          calculateStatusMessage();
        }

        return added;
      }

      return false;
    }
  }

  /**
   * 
   * @param providerKey
   * @param providerName
   * @param balisesException
   * @param relevesException
   * @param paused
   */
  private static void manageExceptionsList(final String providerKey, final String providerName, final Throwable balisesException, final Throwable relevesException, final boolean paused)
  {
    // Gestion de la liste
    if (paused || ((balisesException == null) && (relevesException == null)))
    {
      statusExceptionsKeyList.remove(providerKey);
      statusExceptionsList.remove(providerName);
    }
    else if (!statusExceptionsKeyList.contains(providerKey))
    {
      statusExceptionsKeyList.add(providerKey);
      statusExceptionsList.add(providerName);
    }
  }

  /**
   * 
   * @param netOff
   */
  public static void manageNetworkStatus(final boolean netOff)
  {
    networkOff = netOff;
  }

  /**
   * 
   * @param providerKey
   * @param providerName
   * @param balisesException
   * @param relevesException
   * @param paused
   * @return
   */
  public static boolean removeStatusMessageProviderName(final String providerKey, final String providerName, final Throwable balisesException, final Throwable relevesException, final boolean paused)
  {
    synchronized (statusMessageLock)
    {
      // Gestion des erreurs
      manageExceptionsList(providerKey, providerName, balisesException, relevesException, paused);

      // Enregistrement
      final boolean removed = statusMessageNamesList.remove(providerName);

      if (removed)
      {
        // Elaboration du message
        calculateStatusMessage();
      }

      return removed;
    }
  }

  /**
   * 
   * @author pedro.m
   */
  public interface LocationSettingsDialogListener
  {
    /**
     * Annulation (bouton annuler ou touche "back")
     */
    public void onCancelLocationSettingsDialog();
  }

  /**
   * 
   * @param activity
   * @param dialogListener
   */
  public static void locationSettingsDialog(final Activity activity, final LocationSettingsDialogListener dialogListener)
  {
    final OnClickListener okButtonListener = new DialogInterface.OnClickListener()
    {
      @Override
      public void onClick(final DialogInterface dialog, final int onClick)
      {
        final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        activity.startActivity(intent);
        locationSettingsLaunched = true;
      }
    };

    final OnClickListener cancelButtonListener = new DialogInterface.OnClickListener()
    {
      @Override
      public void onClick(final DialogInterface dialog, final int onClick)
      {
        if (dialogListener != null)
        {
          dialogListener.onCancelLocationSettingsDialog();
        }
      }
    };

    final DialogInterface.OnCancelListener cancelListener = new DialogInterface.OnCancelListener()
    {
      @Override
      public void onCancel(final DialogInterface dialog)
      {
        if (dialogListener != null)
        {
          dialogListener.onCancelLocationSettingsDialog();
        }
      }
    };

    ActivityCommons.confirmDialog(activity, ActivityCommons.CONFIRM_DIALOG_LOCATION_SETTINGS, resources.getString(R.string.app_name), resources.getString(R.string.message_location_non_active), okButtonListener, cancelButtonListener, false,
        cancelListener, -1, -1, null, null, null);
  }

  /**
   * 
   * @param locationManager
   * @return
   */
  public static boolean isLocationAvailable(final LocationManager locationManager)
  {
    return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) || locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
  }

  /**
   * 
   * @param locationManager
   * @return
   */
  public static boolean checkLocationProviders(final LocationManager locationManager)
  {
    // Choix du provider
    String firstLocationProvider = null;
    String secondLocationProvider = null;
    if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
    {
      // Via reseaux (wifi ou cellulaire)
      firstLocationProvider = LocationManager.NETWORK_PROVIDER;
      secondLocationProvider = LocationManager.NETWORK_PROVIDER;
    }
    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
    {
      // Via GPS
      if (firstLocationProvider == null)
      {
        // Pour le premier provider, priorite au reseau (plus rapide)
        firstLocationProvider = LocationManager.GPS_PROVIDER;
      }
      secondLocationProvider = LocationManager.GPS_PROVIDER;
    }

    // Aucun fournisseur de position => retour
    if ((firstLocationProvider == null) && (secondLocationProvider == null))
    {
      return false;
    }

    // GPS ou reseau actif
    return true;
  }

  /**
   * 
   * @param activity
   * @param providersService
   * @param locationListener
   * @param doFineLocation
   * @param requestLocationUpdate
   * @param showProgress
   * @param progressDialogId
   * @param cancelable
   * @return
   */
  public static boolean startLocation(final Activity activity, final IProvidersService providersService, final LocationListener locationListener, final boolean doFineLocation, final boolean requestLocationUpdate,
      final boolean showProgress, final int progressDialogId, final boolean cancelable)
  {
    // Initialisations
    final LocationService locationService = providersService.getLocationService();

    // Dispo d'au moins un provider
    if (!locationService.isLocationEnabled())
    {
      return false;
    }

    // Localisation rapide
    final Location location = locationService.requestSingleLocation(locationListener, doFineLocation, requestLocationUpdate);

    // Position rapide trouvee ?
    if (location != null)
    {
      // Position trouvee
      locationListener.onLocationChanged(location);

      // Si pas de demande de position precise, on sort
      if (!doFineLocation)
      {
        return true;
      }
    }

    // Position rapide non trouvee ou demande de position precise
    // => affichage d'une boite de progression
    if (showProgress)
    {
      final int messageId = (location == null ? R.string.message_location_progress : R.string.message_finer_location_progress);
      progressDialog(activity, progressDialogId, resources.getString(R.string.app_name), resources.getString(messageId), true, cancelable, new DialogInterface.OnCancelListener()
      {
        @Override
        public void onCancel(final DialogInterface arg0)
        {
          locationService.cancelSingleRequest(locationListener);
        }
      });
    }

    // GPS ou reseau actif
    return true;
  }

  /**
   * 
   * @param providersService
   * @param locationListener
   */
  public static void endLocation(final IProvidersService providersService, final LocationListener locationListener)
  {
    // Service ok ?
    if (providersService == null)
    {
      return;
    }

    // Initialisations
    final LocationService locationService = providersService.getLocationService();

    // Annulation
    locationService.cancelSingleRequest(locationListener);
  }

  /**
   * 
   * @param activity
   */
  public static void goToMarket(final Activity activity)
  {
    try
    {
      // Play Store
      final Intent goToMarket = new Intent(Intent.ACTION_VIEW, MARKET_URI);
      activity.startActivity(goToMarket);
    }
    catch (final RuntimeException re)
    {
      // Log
      Log.w(activity.getClass().getSimpleName(), "Impossible d'aller sur le play store", re);

      // Site web
      try
      {
        final Intent goToMobibalisesWeb = new Intent(Intent.ACTION_VIEW, Uri.parse(MOBIBALISES_WEB_URL));
        activity.startActivity(goToMobibalisesWeb);
      }
      catch (final RuntimeException wre)
      {
        // Log
        Log.w(activity.getClass().getSimpleName(), "Impossible d'aller sur le site web", wre);
      }
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class MessageFfvl
  {
    String titre;
    Date   date;
    String texte;

    /**
     * 
     */
    MessageFfvl()
    {
      super();
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class FfvlInfosContentHandler implements ContentHandler
  {
    private static final DateFormat DATE_FORMAT   = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final String     TAG_INFO      = "info";
    private static final String     TAG_DATE      = "date";
    private static final String     TAG_TITRE     = "titre";
    private static final String     TAG_TEXTE     = "texte";
    private static final String     ATT_VALUE     = "value";

    private String                  currentString = Strings.VIDE;
    final List<MessageFfvl>         messages      = new ArrayList<MessageFfvl>();
    private MessageFfvl             message;

    /**
     * 
     */
    FfvlInfosContentHandler()
    {
      super();
    }

    @Override
    public void startDocument() throws SAXException
    {
      messages.clear();
      currentString = Strings.VIDE;
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes atts) throws SAXException
    {
      // Initialisation
      final String finalName;
      if ((qName == null) || (qName.length() == 0))
      {
        finalName = localName;
      }
      else
      {
        finalName = qName;
      }

      // Nouvelle info
      if (TAG_INFO.equals(finalName))
      {
        message = new MessageFfvl();
        messages.add(message);
      }

      // Date
      if (TAG_DATE.equals(finalName))
      {
        final String valueString = atts.getValue(ATT_VALUE);
        if (valueString != null)
        {
          try
          {
            // Analyse date
            message.date = DATE_FORMAT.parse(valueString);

            // Decalage dans le fuseau horaire UTC
            Utils.toUTC(message.date, FfvlProvider.sourceTimeZone);
          }
          catch (final ParseException pe)
          {
            // Rien
          }
        }
      }
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException
    {
      // Initialisation
      final String finalName;
      if ((qName == null) || (qName.length() == 0))
      {
        finalName = localName;
      }
      else
      {
        finalName = qName;
      }

      // Titre
      if (TAG_TITRE.equals(finalName))
      {
        message.titre = currentString;
      }
      else if (TAG_TEXTE.equals(finalName))
      {
        message.texte = currentString;
      }

      // RAZ
      currentString = Strings.VIDE;
    }

    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException
    {
      currentString += new String(ch, start, length);
    }

    @Override
    public void endDocument() throws SAXException
    {
      //Nothing
    }

    @Override
    public void endPrefixMapping(final String arg0) throws SAXException
    {
      //Nothing
    }

    @Override
    public void ignorableWhitespace(final char[] ch, final int start, final int length) throws SAXException
    {
      //Nothing
    }

    @Override
    public void processingInstruction(final String arg0, final String arg1) throws SAXException
    {
      //Nothing
    }

    @Override
    public void setDocumentLocator(final Locator locator)
    {
      //Nothing
    }

    @Override
    public void skippedEntity(final String arg0) throws SAXException
    {
      //Nothing
    }

    @Override
    public void startPrefixMapping(final String arg0, final String arg1) throws SAXException
    {
      //Nothing
    }
  }

  /**
   * 
   * @param parser
   * @param ffvlKey
   * @return
   * @throws IOException
   */
  private static Long getLastFfvlMessageTimestamp(final SAXParser parser, final String ffvlKey) throws IOException
  {
    try
    {
      // Initialisations
      final LastUpdateFfvlContentHandler lastUpdateHandler = new LastUpdateFfvlContentHandler();
      final String finalUrl = FfvlProvider.URL_LAST_UPDATE.replaceAll(FfvlProvider.URL_FFVL_KEY_GROUP, ffvlKey);
      final InputStream input = FfvlProvider.getUnzippedInputStream(finalUrl, true);

      // Analyse XML
      final XMLReader reader = parser.getXMLReader();
      reader.setContentHandler(lastUpdateHandler);
      reader.parse(new InputSource(input));

      // Stockage
      return lastUpdateHandler.getUpdateDates().get(LastUpdateFfvlContentHandler.INFOS_KEY);
    }
    catch (final SAXException se)
    {
      final IOException ioe = new IOException(se.getMessage());
      ioe.setStackTrace(se.getStackTrace());
      throw ioe;
    }
  }

  /**
   * 
   * @param activity
   * @param ffvlKey
   * @param showProgressDialog
   * @param forceDisplay
   */
  public static void checkForFfvlMessage(final Activity activity, final String ffvlKey, final boolean showProgressDialog, final boolean forceDisplay)
  {
    // Thread de tache de fond
    final Thread ffvlMessageThread = new Thread(ActivityCommons.class.getName() + ".ffvlMessageThread")
    {
      @Override
      public void run()
      {
        // Recuperation du message
        final String message = checkForFfvlMessage(ffvlKey, forceDisplay);

        // Effacement du dialogue de progression
        cancelProgressDialog(PROGRESS_DIALOG_FFVL);

        // Affichage du message
        if (!ffvlMessageThreadInterrupted)
        {
          if ((message != null) && (message.trim().length() > 0))
          {
            // Affichage
            alertDialog(activity, ALERT_DIALOG_FFVL_MESSAGE, R.drawable.ic_ffvl, resources.getString(R.string.ffvl_message_dialog_title), message, null, true, null, Linkify.ALL);
          }
          else if (forceDisplay)
          {
            alertDialog(activity, ALERT_DIALOG_FFVL_MESSAGE, R.drawable.ic_ffvl, resources.getString(R.string.ffvl_message_dialog_title), resources.getString(R.string.ffvl_message_none), null, true, null, Linkify.ALL);
          }
        }
      }
    };

    // Message de progression
    if (showProgressDialog)
    {
      progressDialog(activity, PROGRESS_DIALOG_FFVL, resources.getString(R.string.ffvl_message_progress_dialog_title), resources.getString(R.string.ffvl_message_progress_dialog_message), true, true, new DialogInterface.OnCancelListener()
      {
        @Override
        public void onCancel(final DialogInterface dialog)
        {
          ffvlMessageThreadInterrupted = true;
          if (ffvlMessageThread.isAlive())
          {
            ffvlMessageThread.interrupt();
            ThreadUtils.join(ffvlMessageThread);
          }
        }
      });
    }

    // Demarrage
    ffvlMessageThreadInterrupted = false;
    ffvlMessageThread.start();
  }

  /**
   * 
   * @param ffvlKey
   * @param force
   */
  static String checkForFfvlMessage(final String ffvlKey, final boolean force)
  {
    try
    {
      // Initialisations
      Log.d(ActivityCommons.class.getSimpleName(), ">>> checkForFfvlMessage(..., " + force + ")");
      final SAXParserFactory factory = SAXParserFactory.newInstance();
      final SAXParser parser = factory.newSAXParser();

      // Recuperation de la date de MAJ du message
      final String message;
      final Long messageTimestamp = getLastFfvlMessageTimestamp(parser, ffvlKey);
      final long lastMessageTimestamp = sharedPreferences.getLong(AbstractBalisesPreferencesActivity.CONFIG_LAST_FFVL_MESSAGE, -1);
      Log.d(ActivityCommons.class.getSimpleName(), "ffvl timestamp=" + messageTimestamp + ", last timestamp=" + lastMessageTimestamp);
      if ((messageTimestamp != null) && ((messageTimestamp.longValue() > lastMessageTimestamp) || force))
      {
        // Initialisations
        final FfvlInfosContentHandler infosHandler = new FfvlInfosContentHandler();
        final String finalUrl = FfvlProvider.URL_INFOS.replaceAll(FfvlProvider.URL_FFVL_KEY_GROUP, ffvlKey);
        final InputStream input = FfvlProvider.getUnzippedInputStream(finalUrl, true);

        // Analyse XML
        final XMLReader reader = parser.getXMLReader();
        reader.setContentHandler(infosHandler);
        reader.parse(new InputSource(input));

        // Transformation des messages en chaine
        if (infosHandler.messages.size() > 0)
        {
          final StringBuilder builder = new StringBuilder();
          boolean first = true;
          for (final MessageFfvl messageFfvl : infosHandler.messages)
          {
            // Premier ?
            if (!first)
            {
              builder.append(Strings.CHAR_NEWLINE);
            }
            first = false;

            builder.append(HTML_TITRE_FFVL_PREFIX);
            builder.append(messageFfvl.titre);
            builder.append(HTML_TITRE_FFVL_SUFFIX);
            if (messageFfvl.date != null)
            {
              builder.append(Strings.CHAR_SPACE);
              builder.append(Strings.CHAR_PARENTHESE_DEB);
              Utils.fromUTC(messageFfvl.date); // Conversion dans le timezone du terminal
              builder.append(formatDateMessageFfvl.format(messageFfvl.date));
              builder.append(Strings.CHAR_PARENTHESE_FIN);
              builder.append(Strings.CHAR_NEWLINE);
            }
            builder.append(messageFfvl.texte);
          }
          message = builder.toString();
        }
        else
        {
          message = null;
        }

        // Sauvegarde timestamp
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(AbstractBalisesPreferencesActivity.CONFIG_LAST_FFVL_MESSAGE, messageTimestamp.longValue());
        commitPreferences(editor);
      }
      else
      {
        // Pas de message
        message = null;
      }

      // Fin
      Log.d(ActivityCommons.class.getSimpleName(), "<<< checkForFfvlMessage() : " + message);
      return message;
    }
    catch (final IOException ioe)
    {
      Log.e(ActivityCommons.class.getSimpleName(), "Erreur d'analyse message FFVL", ioe);
      return null;
    }
    catch (final ParserConfigurationException pce)
    {
      Log.e(ActivityCommons.class.getSimpleName(), "Erreur d'analyse message FFVL", pce);
      return null;
    }
    catch (final SAXException se)
    {
      Log.e(ActivityCommons.class.getSimpleName(), "Erreur d'analyse message FFVL", se);
      return null;
    }
  }

  /**
   * 
   * @param context
   * @param speedValue
   * @param altitudeValue
   * @param distanceValue
   * @param temperatureValue
   * @return
   */
  public static boolean updateUnitPreferences(final Context context, final String speedValue, final String altitudeValue, final String distanceValue, final String temperatureValue)
  {
    // Initialisations
    boolean changed = false;

    // Vitesse
    {
      final String value = (speedValue != null ? speedValue : sharedPreferences.getString(resources.getString(R.string.config_unit_speed_key), resources.getString(R.string.config_unit_speed_default)));
      final int labelId = resources.getIdentifier(AbstractBalisesPreferencesActivity.UNIT_SPEED_PREFIX + value, Strings.RESOURCES_STRING, context.getPackageName());
      final String newUnitSpeedLabel = resources.getString(labelId);
      changed = changed || !newUnitSpeedLabel.equals(unitSpeedLabel);
      unitSpeedLabel = newUnitSpeedLabel;

      final int factorId = resources.getIdentifier(AbstractBalisesPreferencesActivity.UNIT_SPEED_FACTOR_PREFIX + value, Strings.RESOURCES_STRING, context.getPackageName());
      unitSpeedFactor = Double.parseDouble(resources.getString(factorId));
    }

    // Altitude
    {
      final String value = (altitudeValue != null ? altitudeValue : sharedPreferences.getString(resources.getString(R.string.config_unit_altitude_key), resources.getString(R.string.config_unit_altitude_default)));
      final int labelId = resources.getIdentifier(AbstractBalisesPreferencesActivity.UNIT_ALTITUDE_PREFIX + value, Strings.RESOURCES_STRING, context.getPackageName());
      final String newUnitAltitudeLabel = resources.getString(labelId);
      changed = changed || !newUnitAltitudeLabel.equals(unitAltitudeLabel);
      unitAltitudeLabel = newUnitAltitudeLabel;

      final int factorId = resources.getIdentifier(AbstractBalisesPreferencesActivity.UNIT_ALTITUDE_FACTOR_PREFIX + value, Strings.RESOURCES_STRING, context.getPackageName());
      unitAltitudeFactor = Double.parseDouble(resources.getString(factorId));
    }

    // Distance
    {
      final String value = (distanceValue != null ? distanceValue : sharedPreferences.getString(resources.getString(R.string.config_unit_distance_key), resources.getString(R.string.config_unit_distance_default)));
      final int labelId = resources.getIdentifier(AbstractBalisesPreferencesActivity.UNIT_DISTANCE_PREFIX + value, Strings.RESOURCES_STRING, context.getPackageName());
      final String newUnitDistanceLabel = resources.getString(labelId);
      changed = changed || !newUnitDistanceLabel.equals(unitDistanceLabel);
      unitDistanceLabel = newUnitDistanceLabel;

      final int factorId = resources.getIdentifier(AbstractBalisesPreferencesActivity.UNIT_DISTANCE_FACTOR_PREFIX + value, Strings.RESOURCES_STRING, context.getPackageName());
      unitDistanceFactor = Double.parseDouble(resources.getString(factorId));
    }

    // Temperature
    {
      final String value = (temperatureValue != null ? temperatureValue : sharedPreferences.getString(resources.getString(R.string.config_unit_temperature_key), resources.getString(R.string.config_unit_temperature_default)));
      final int labelId = resources.getIdentifier(AbstractBalisesPreferencesActivity.UNIT_TEMPERATURE_PREFIX + value, Strings.RESOURCES_STRING, context.getPackageName());
      final String newUnitTemperatureLabel = resources.getString(labelId);
      changed = changed || !newUnitTemperatureLabel.equals(unitTemperatureLabel);
      unitTemperatureLabel = newUnitTemperatureLabel;

      final int factorId = resources.getIdentifier(AbstractBalisesPreferencesActivity.UNIT_TEMPERATURE_FACTOR_PREFIX + value, Strings.RESOURCES_STRING, context.getPackageName());
      unitTemperatureFactor = Double.parseDouble(resources.getString(factorId));
      final int deltaId = resources.getIdentifier(AbstractBalisesPreferencesActivity.UNIT_TEMPERATURE_DELTA_PREFIX + value, Strings.RESOURCES_STRING, context.getPackageName());
      unitTemperatureDelta = Double.parseDouble(resources.getString(deltaId));
    }

    return changed;
  }

  /**
   * 
   * @param speed
   * @return
   */
  public static double getFinalSpeed(final double speed)
  {
    return speed * unitSpeedFactor;
  }

  /**
   * 
   * @param speed
   * @return
   */
  public static double getInitialSpeed(final double speed)
  {
    return speed / unitSpeedFactor;
  }

  /**
   * 
   * @return
   */
  public static String getSpeedUnit()
  {
    return unitSpeedLabel;
  }

  /**
   * 
   * @param altitude
   * @return
   */
  public static int getFinalAltitude(final int altitude)
  {
    return (int)Math.round(altitude * unitAltitudeFactor);
  }

  /**
   * 
   * @return
   */
  public static String getAltitudeUnit()
  {
    return unitAltitudeLabel;
  }

  /**
   * 
   * @param distance
   * @return
   */
  public static double getFinalDistance(final double distance)
  {
    return distance * unitDistanceFactor;
  }

  /**
   * 
   * @return
   */
  public static String getDistanceUnit()
  {
    return unitDistanceLabel;
  }

  /**
   * 
   * @param temperature
   * @return
   */
  public static double getFinalTemperature(final double temperature)
  {
    return temperature * unitTemperatureFactor + unitTemperatureDelta;
  }

  /**
   * 
   * @param temperature
   * @return
   */
  public static double getInitialTemperature(final double temperature)
  {
    return (temperature - unitTemperatureDelta) / unitTemperatureFactor;
  }

  /**
   * 
   * @return
   */
  public static String getTemperatureUnit()
  {
    return unitTemperatureLabel;
  }

  /**
   * 
   * @param value
   * @param limite
   * @param format
   * @param tendance
   * @param underLimitColor
   * @param overLimitColor
   * @return
   */
  public static String formatDouble(final double value, final Integer limite, final NumberFormat format, final String tendance, final int underLimitColor, final int overLimitColor)
  {
    // Initialisations
    if (Double.isNaN(value))
    {
      return Strings.TIRET;
    }
    final StringBuffer retour = new StringBuffer(128);

    // Limite depassee ?
    boolean doLimit = false;
    boolean overLimit = false;
    if (limite != null)
    {
      doLimit = true;
      overLimit = (value > limite.intValue());
    }

    // En tete depassement
    if (doLimit)
    {
      retour.append(MessageFormat.format(HTML_FONT_COLOR_PREFIX, Strings.toHexColor(overLimit ? overLimitColor : underLimitColor)));
    }

    // Valeur
    retour.append(format.format(value));

    // Pied depassement
    if (doLimit)
    {
      retour.append(HTML_FONT_COLOR_SUFFIX);
    }

    // Tendance
    if (!Utils.isStringVide(tendance))
    {
      retour.append(tendance);
    }

    return retour.toString();
  }

  /**
   * 
   * @param context
   * @return
   */
  public static String getDeviceId(final Context context)
  {
    // ANDROID_ID
    return Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
  }

  /**
   * 
   * @param providersService
   * @param category
   * @param action
   * @param label
   */
  public static void trackEvent(final IProvidersService providersService, final String category, final String action, final String label)
  {
    // Service ok ?
    if (providersService == null)
    {
      return;
    }

    // Analytics service
    final AnalyticsService analyticsService = providersService.getAnalyticsService();
    if (analyticsService == null)
    {
      return;
    }

    // Track
    analyticsService.trackEvent(category, action, label);
  }

  /**
   * 
   * @param view
   */
  public static void unbindDrawables(final View view)
  {
    if (view == null)
    {
      return;
    }

    if (view.getBackground() != null)
    {
      view.getBackground().setCallback(null);
    }

    if (ViewGroup.class.isAssignableFrom(view.getClass()))
    {
      final ViewGroup viewGroup = (ViewGroup)view;
      final int count = viewGroup.getChildCount();
      for (int i = 0; i < count; i++)
      {
        unbindDrawables(viewGroup.getChildAt(i));
      }
      try
      {
        viewGroup.removeAllViews();
      }
      catch (final UnsupportedOperationException uoe)
      {
        //Nothing
      }
    }
  }

  /**
   * 
   * @param source
   * @param dest
   */
  private static void copyArray(final SparseArray<Dialog> source, final SparseArray<Dialog> dest)
  {
    for (int i = 0; i < source.size(); i++)
    {
      final int key = source.keyAt(i);
      final Dialog value = source.valueAt(i);
      dest.put(key, value);
    }
  }

  /**
   * 
   */
  public static void closeDialogs()
  {
    // Alertes
    synchronized (alertDialogs)
    {
      final SparseArray<Dialog> copie = new SparseArray<Dialog>();
      copyArray(alertDialogs, copie);
      for (int i = 0; i < copie.size(); i++)
      {
        final int id = copie.keyAt(i);
        dismissAlertDialog(id);
      }
    }

    // Progress
    synchronized (progressDialogs)
    {
      final SparseArray<Dialog> copie = new SparseArray<Dialog>();
      copyArray(progressDialogs, copie);
      for (int i = 0; i < copie.size(); i++)
      {
        final int id = copie.keyAt(i);
        dismissProgressDialog(id);
      }
    }
  }

  /**
   * 
   * @return
   */
  public static boolean isFroyo()
  {
    return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO);
  }

  /**
   * 
   * @return
   */
  public static boolean isGingerBread()
  {
    return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD);
  }

  /**
   * 
   * @return
   */
  public static boolean isHoneyComb()
  {
    return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB);
  }

  /**
   * 
   * @return
   */
  public static boolean isJellyBean()
  {
    return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN);
  }

  /**
   * 
   * @return
   */
  public static boolean isKitKat()
  {
    return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT);
  }

  /**
   * 
   * @param context
   * @return
   */
  public static boolean hasPermanentMenuKey(final Context context)
  {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    {
      return ViewConfiguration.get(context).hasPermanentMenuKey();
    }
    else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
    {
      // L'appareil a forcement une touche de menu physique
      return true;
    }

    // Impossible de savoir, on considere qu'il n'y en a pas par securite
    return false;
  }

  /**
   * 
   * @param context
   * @return
   */
  public static final boolean isDebug(final Context context)
  {
    return (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) > 0;
  }

  /**
   * 
   * @param context
   * @return
   */
  public static final long getTotalRAM(final Context context)
  {
    // Jelly Bean (API 16, Android 4.1)
    if (isJellyBean())
    {
      final ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
      final MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
      activityManager.getMemoryInfo(memoryInfo);

      return memoryInfo.totalMem;
    }

    // Avant JB
    FileReader fr = null;
    BufferedReader br = null;
    try
    {
      fr = new FileReader("/proc/meminfo");
      br = new BufferedReader(fr);
      final String line = br.readLine();
      final Pattern pattern = Pattern.compile("[^\\d]*(\\d+)[^\\d]*");
      final Matcher matcher = pattern.matcher(line);
      if (matcher.matches())
      {
        return 1024 * Long.parseLong(matcher.group(1), 10);
      }
    }
    catch (final IOException ioe)
    {
      Log.e(ActivityCommons.class.getName(), ioe.getMessage(), ioe);
    }
    finally
    {
      try

      {
        if (br != null)
        {
          br.close();
        }
        if (fr != null)
        {
          fr.close();
        }
      }
      catch (final IOException ioe)
      {
        Log.w(ActivityCommons.class.getSimpleName(), ioe);
      }
    }

    return -1;
  }

  /**
   * 
   * @param activity
   */
  public static final void setSplashVersion(final Activity activity)
  {
    // Version
    final TextView textView = (TextView)activity.findViewById(R.id.start_layout_version);
    if (textView != null)
    {
      final String version = String.format(activity.getResources().getString(R.string.message_startup), activity.getResources().getString(R.string.app_version));
      textView.setText(version);
    }
  }

  /**
   * 
   * @param activity
   * @param createTimestamp
   */
  public static final void hideSplash(final Activity activity, final long createTimestamp)
  {
    // Gestion de createTimestamp
    final long delta = System.currentTimeMillis() - createTimestamp;
    final long waitTime = 3500 - delta;

    // Procédure d'effacement du splash
    final Runnable hideRunnable = new Runnable()
    {
      @Override
      public void run()
      {
        final View splashView = activity.findViewById(R.id.splashview);
        if (splashView != null)
        {
          Log.d(activity.getClass().getSimpleName(), "Hiding splash screen");
          splashView.setVisibility(View.GONE);
        }
      }
    };

    // Effacement du splash direct si delai atteint, par tache de fond apres attente sinon
    if (waitTime > 0)
    {
      final AsyncTask<Void, Void, Void> hideTask = new AsyncTask<Void, Void, Void>()
      {
        @Override
        protected Void doInBackground(Void... arg0)
        {
          try
          {
            Log.d(activity.getClass().getSimpleName(), "Waiting " + waitTime + "ms before hiding splash screen");
            Thread.sleep(waitTime);
          }
          catch (final InterruptedException ie)
          {
            Log.w(activity.getClass().getSimpleName(), ie);
          }

          return null;
        }

        @Override
        protected void onPostExecute(final Void result)
        {
          hideRunnable.run();
        }
      };
      hideTask.execute();
    }
    else
    {
      activity.runOnUiThread(hideRunnable);
    }
  }

  /**
   * 
   * @param activity
   * @param splashId
   * @param noSplashId
   * @return
   */
  public static final boolean manageSplash(final Activity activity, final int splashId, final int noSplashId)
  {
    // Splash screen ?
    final boolean showSplash = activity.getIntent().getBooleanExtra(AbstractBalisesStartActivity.EXTRA_SPLASH_SCREEN, false);
    activity.getIntent().removeExtra(AbstractBalisesStartActivity.EXTRA_SPLASH_SCREEN);
    if (showSplash)
    {
      // Splash
      activity.setContentView(splashId);
      ActivityCommons.setSplashVersion(activity);
    }
    else
    {
      // Sans splash
      activity.setContentView(noSplashId);
    }

    return showSplash;
  }

  /**
   * 
   * @param context
   * @return
   */
  public static final SharedPreferences getSharedPreferences(final Context context)
  {
    // Trace
    //Log.d(ActivityCommons.class.getSimpleName(), ">>> getSharedPreferences() - " + getSimpleTrace());

    // Recuperation des preferences
    final SharedPreferences prefs = context.getSharedPreferences(context.getResources().getString(R.string.preferences_shared_name), Context.MODE_PRIVATE);

    // Trace
    //Log.d(ActivityCommons.class.getSimpleName(), "<<< getSharedPreferences() - " + getSimpleTrace() + " - " + prefs);

    // Fin
    return prefs;
  }

  /**
   * 
   * @param editor
   * @return
   */
  public static final boolean commitPreferences(final SharedPreferences.Editor editor)
  {
    synchronized (Thread.currentThread())
    {
      // Trace
      //Log.d(ActivityCommons.class.getSimpleName(), ">>> commit() - " + getSimpleTrace());

      // Interruption ?
      final boolean wasInterrupted = Thread.currentThread().isInterrupted();

      // Commit
      final boolean commited = editor.commit();

      // Fin
      if (wasInterrupted)
      {
        Thread.currentThread().interrupt();
      }

      // Trace
      //Log.d(ActivityCommons.class.getSimpleName(), "<<< commit() - " + getSimpleTrace());

      return commited;
    }
  }

  /**
   * 
   * @return
   */
  @SuppressWarnings("unused")
  private static String getSimpleTrace()
  {
    // Initialisations
    final StringBuilder buffer = new StringBuilder();
    final StackTraceElement trace = Thread.currentThread().getStackTrace()[4];

    // Thread
    final String threadName = Thread.currentThread().getName();
    final int threadNameLength = threadName.length();
    if (threadNameLength > 23)
    {
      buffer.append(threadName.substring(0, 10));
      buffer.append("...");
      buffer.append(threadName.substring(threadNameLength - 10));
    }
    else
    {
      buffer.append(threadName);
    }

    // Classe (sans le package)
    buffer.append(" - ");
    final String className = trace.getClassName();
    final int lastPoint = className.lastIndexOf('.');
    buffer.append(lastPoint >= 0 ? className.substring(lastPoint + 1) : className);

    // Methode
    buffer.append('.');
    buffer.append(trace.getMethodName());
    buffer.append('(');
    buffer.append(trace.getLineNumber());
    buffer.append(')');

    return buffer.toString();
  }

  /**
   * 
   * @param context
   * @return
   */
  public static String getBuildDate(final Context context)
  {
    // Initialisations
    String retour = null;
    ZipFile zipFile = null;

    try
    {
      ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
      zipFile = new ZipFile(appInfo.sourceDir);
      final ZipEntry entry = zipFile.getEntry("classes.dex");
      final long time = entry.getTime();

      retour = DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(time));
    }
    catch (final IOException ioe)
    {
      Log.w(context.getClass().getSimpleName(), ioe.getMessage(), ioe);
    }
    catch (final NameNotFoundException nnfe)
    {
      Log.w(context.getClass().getSimpleName(), nnfe.getMessage(), nnfe);
    }
    finally
    {
      try
      {
        if (zipFile != null)
        {
          zipFile.close();
        }
      }
      catch (final IOException ioe)
      {
        Log.w(context.getClass().getSimpleName(), ioe.getMessage(), ioe);
      }
    }

    return retour;
  }

  /**
   * 
   * @param context
   */
  public static DisplayMetrics getDisplayMetrics(final Context context)
  {
    // WindowManager
    final WindowManager windowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
    if (windowManager == null)
    {
      return null;
    }

    // DisplayMetrics
    final DisplayMetrics metrics = new DisplayMetrics();
    windowManager.getDefaultDisplay().getMetrics(metrics);

    return metrics;
  }

  /**
   * 
   */
  public static String[] logHeap()
  {
    final Double allocated = new Double(Debug.getNativeHeapAllocatedSize() / 1048576.0);
    final Double available = new Double(Debug.getNativeHeapSize() / 1048576.0);
    final Double free = new Double(Debug.getNativeHeapFreeSize() / 1048576.0);
    final DecimalFormat df = new DecimalFormat();
    df.setMaximumFractionDigits(2);
    df.setMinimumFractionDigits(2);

    final String heap = "allocated " + df.format(allocated) + "MB of " + df.format(available) + "MB (" + df.format(free) + "MB free)";
    final String memory = "allocated " + df.format(new Double(Runtime.getRuntime().totalMemory() / 1048576)) + "MB of " + df.format(new Double(Runtime.getRuntime().maxMemory() / 1048576)) + "MB ("
        + df.format(new Double(Runtime.getRuntime().freeMemory() / 1048576)) + "MB free)";
    Log.d("heap", "debug.heap native: " + heap);
    Log.d("heap", "debug.memory: " + memory);

    return new String[] { heap, memory };
  }
}
