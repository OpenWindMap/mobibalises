package com.pedro.android.mobibalises.preferences;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.Strings;
import org.pedro.android.mobibalises_common.service.AbstractProvidersService;
import org.pedro.android.mobibalises_common.service.ProvidersServiceBinder;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pedro.android.mobibalises.FullActivityCommons;
import com.pedro.android.mobibalises.R;
import com.pedro.android.mobibalises.service.IFullProvidersService;
import com.pedro.android.mobibalises.service.ProvidersService;
import com.pedro.android.mobibalises.widget.BalisesWidgets;

/**
 * 
 * @author pedro.m
 */
public abstract class AbstractBalisesWidgetPreferencesActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener
{
  private static final int           PROGRESS_DIALOG_SERVICE_INIT = 301;

  public static final int            WIDGET_TYPE_11               = 11;
  public static final int            WIDGET_TYPE_21               = 21;
  public static final int            WIDGET_TYPE_22               = 22;
  public static final int            WIDGET_TYPE_33               = 33;
  public static final int            WIDGET_TYPE_41               = 41;
  public static final int            WIDGET_TYPE_42               = 42;
  public static final int            WIDGET_TYPE_43               = 43;
  public static final int            WIDGET_TYPE_44               = 44;

  protected SharedPreferences        sharedPreferences;

  IFullProvidersService              providersService;
  private ProvidersServiceConnection providersServiceConnection;

  protected int                      launchAppWidgetId            = AppWidgetManager.INVALID_APPWIDGET_ID;
  protected Resources                resources;

  private final List<ListPreference> listePreferences             = new ArrayList<ListPreference>();
  private final List<Integer>        appwidgetIds                 = new ArrayList<Integer>();

  @Override
  public void onCreate(final Bundle savedInstanceState)
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> abstract.onCreate()");
    super.onCreate(savedInstanceState);
    resources = getResources();

    // Vue
    setContentView(R.layout.preferences);

    // Connexion au service
    initProvidersService();

    // Preferences partagees
    final PreferenceManager manager = getPreferenceManager();
    manager.setSharedPreferencesName(resources.getString(R.string.preferences_shared_name));
    manager.setSharedPreferencesMode(Context.MODE_PRIVATE);
    sharedPreferences = manager.getSharedPreferences();

    // Traitements specifiques pour le widget
    manageWidget();
    Log.d(getClass().getSimpleName(), "launchAppWidgetId = " + launchAppWidgetId);

    // Sauvegarde du type
    if (launchAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID)
    {
      final Editor editor = sharedPreferences.edit();
      editor.putInt(AbstractBalisesWidgetPreferencesActivity.getKeyForWidget(resources.getString(R.string.config_widget_type_key), launchAppWidgetId), getWidgetType());
      ActivityCommons.commitPreferences(editor);
    }

    // Reglage ecran
    final PreferenceScreen prefScreen = getPreferenceManager().createPreferenceScreen(this);
    setPreferenceScreen(prefScreen);

    // Creation des preferences
    if (launchAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID)
    {
      initSingleWidget(prefScreen);
      final Intent resultValue = new Intent();
      resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, launchAppWidgetId);
      setResult(RESULT_OK, resultValue);
    }
    else
    {
      initAllWidgets(prefScreen);
    }

    // Message si vide
    final LayoutInflater inflater = getLayoutInflater();
    final View emptyView = inflater.inflate(R.layout.widgets_preferences_empty_textview_layout, (ViewGroup)getListView().getParent());
    final TextView emptyTextview = (TextView)emptyView.findViewById(R.id.widgets_preferences_empty_textview);
    getListView().setEmptyView(emptyTextview);

    // Fin
    Log.d(getClass().getSimpleName(), "<<< abstract.onCreate()");
  }

  @Override
  public void onPause()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> abstract.onPause()");
    super.onPause();

    // Retrait de la notification de widget parlant
    if (BalisesWidgets.existsSpeakBlackWidget(getApplicationContext()))
    {
      FullActivityCommons.addSpeakBlackWidgetNotification(getApplicationContext());
    }
    else
    {
      FullActivityCommons.removeSpeakBlackWidgetNotification(getApplicationContext());
    }

    // MAJ de la synthese vocale pour widgets
    BalisesWidgets.manageVoiceClient(getApplicationContext(), providersService);

    // MAJ des widgets
    BalisesWidgets.synchronizeWidgets(getApplicationContext(), providersService, null);

    // Fin
    Log.d(getClass().getSimpleName(), "<<< abstract.onPause()");
  }

  @Override
  public void onDestroy()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> abstract.onDestroy()");
    super.onDestroy();

    // Notification au service
    providersServiceConnection.privateOnServiceDisconnected();

    // Deconnexion du service
    unbindService(providersServiceConnection);

    // Liberation vues
    ActivityCommons.unbindDrawables(findViewById(android.R.id.content));

    // Fin
    Log.d(getClass().getSimpleName(), "<<< abstract.onDestroy()");
  }

  /**
   * 
   * @author pedro.m
   */
  private static class ProvidersServiceConnection implements ServiceConnection
  {
    private final WeakReference<AbstractBalisesWidgetPreferencesActivity> prefsActivity;

    /**
     * 
     * @param prefsActivity
     */
    ProvidersServiceConnection(final AbstractBalisesWidgetPreferencesActivity prefsActivity)
    {
      this.prefsActivity = new WeakReference<AbstractBalisesWidgetPreferencesActivity>(prefsActivity);
    }

    @Override
    public void onServiceConnected(final ComponentName name, final IBinder inBinder)
    {
      // Recuperation du service
      prefsActivity.get().providersService = (IFullProvidersService)((ProvidersServiceBinder)inBinder).getService();

      // MAJ des listes
      prefsActivity.get().updateListePrefs();

      // Message d'attente
      ActivityCommons.cancelProgressDialog(PROGRESS_DIALOG_SERVICE_INIT);
    }

    @Override
    public void onServiceDisconnected(final ComponentName name)
    {
      privateOnServiceDisconnected();
    }

    /**
     * 
     */
    void privateOnServiceDisconnected()
    {
      // Il peut arriver que cette methode soit appelee (via onDestroy()) avant onServiceConnected !
      if (prefsActivity.get().providersService != null)
      {
        // Gestion du service de localisation
        if (prefsActivity.get().providersService.getFullLocationService() != null)
        {
          prefsActivity.get().providersService.getFullLocationService().manageListeners(true);
        }

        // Reveil eventuel des threads endormis
        prefsActivity.get().providersService.notifyNeededBaliseProvidersChanged(true);

        // Message d'attente
        ActivityCommons.cancelProgressDialog(PROGRESS_DIALOG_SERVICE_INIT);
      }
    }
  }

  /**
   * 
   */
  private void initProvidersService()
  {
    // Message d'attente
    ActivityCommons.init(getApplicationContext());
    ActivityCommons.progressDialog(this, PROGRESS_DIALOG_SERVICE_INIT, true, false, null);

    // Connection au service
    providersServiceConnection = new ProvidersServiceConnection(this);

    // Connexion au service
    final Intent providersServiceIntent = new Intent(getApplicationContext(), ProvidersService.class);
    providersServiceIntent.putExtra(AbstractProvidersService.STARTED_FROM_ACTIVITY, true);
    startService(providersServiceIntent);
    bindService(providersServiceIntent, providersServiceConnection, Context.BIND_AUTO_CREATE + Context.BIND_NOT_FOREGROUND);
  }

  /**
   * 
   * @param prefScreen
   */
  private void initSingleWidget(final PreferenceScreen prefScreen)
  {
    // Titre
    setTitle(resources.getString(R.string.label_widget_prefs_single_title, Integer.valueOf(launchAppWidgetId)));

    // Ajout de l'ecran
    addPreferencesForWidget(prefScreen, launchAppWidgetId, false);
  }

  /**
   * 
   * @param prefScreen
   */
  private void initAllWidgets(final PreferenceScreen prefScreen)
  {
    // Titre
    setTitle(resources.getString(R.string.label_widget_prefs_all_title));

    // Pour chaque widget
    final int[] appWidgetIds = BalisesWidgets.getAllAppWidgetIds(getApplicationContext());
    for (final int appWidgetId : appWidgetIds)
    {
      // Categorie
      final PreferenceCategory cat = new PreferenceCategory(this);
      prefScreen.addPreference(cat);

      // Titre
      cat.setTitle(resources.getString(R.string.label_widget_prefs_single_title, Integer.valueOf(appWidgetId)));

      // Remplissage categorie
      addPreferencesForWidget(cat, appWidgetId, true);
    }
  }

  /**
   * 
   * @param baseKey
   * @param appWidgetId
   * @return
   */
  public static String getKeyForWidget(final String baseKey, final int appWidgetId)
  {
    return baseKey + Strings.CHAR_UNDERSCORE + appWidgetId;
  }

  /**
   * 
   * @param group
   * @param appWidgetId
   * @param deleteButton
   */
  private void addPreferencesForWidget(final PreferenceGroup group, final int appWidgetId, final boolean deleteButton)
  {
    // Liste de favoris
    final ListPreference listePref = new ListPreference(this);
    final String listeKey = getKeyForWidget(resources.getString(R.string.config_widget_label_key), appWidgetId);
    listePref.setKey(listeKey);
    final String listeDefaultValue = resources.getString(getDefautListValue());
    listePref.setDefaultValue(listeDefaultValue);
    listePref.setTitle(resources.getString(R.string.label_widget_prefs_choosen_list));

    // Affichage ou non de la meteo
    final CheckBoxPreference displayWeatherPref = new CheckBoxPreference(this);
    final String displayWeatherKey = getKeyForWidget(resources.getString(R.string.config_widget_display_weather_key), appWidgetId);
    displayWeatherPref.setKey(displayWeatherKey);
    final String displayWeatherDefaultValue = resources.getString(R.string.config_widget_display_weather_default);
    displayWeatherPref.setDefaultValue(Boolean.valueOf(displayWeatherDefaultValue));
    displayWeatherPref.setTitle(resources.getString(R.string.label_widget_prefs_display_weather));

    // Affichage ou non du titre
    final CheckBoxPreference displayTitlePref = new CheckBoxPreference(this);
    final String displayTitleKey = getKeyForWidget(resources.getString(R.string.config_widget_display_title_key), appWidgetId);
    displayTitlePref.setKey(displayTitleKey);
    final String displayTitleDefaultValue = resources.getString(R.string.config_widget_display_title_default);
    displayTitlePref.setDefaultValue(Boolean.valueOf(displayTitleDefaultValue));
    displayTitlePref.setTitle(resources.getString(R.string.label_widget_prefs_display_title));

    // Affichage ou non de l'en tete
    final CheckBoxPreference displayHeaderPref = new CheckBoxPreference(this);
    final String displayHeaderKey = getKeyForWidget(resources.getString(R.string.config_widget_display_header_key), appWidgetId);
    displayHeaderPref.setKey(displayHeaderKey);
    final String displayHeaderDefaultValue = resources.getString(R.string.config_widget_display_header_default);
    displayHeaderPref.setDefaultValue(Boolean.valueOf(displayHeaderDefaultValue));
    displayHeaderPref.setTitle(resources.getString(R.string.label_widget_prefs_display_header));
    displayHeaderPref.setSummary(resources.getString(R.string.label_widget_prefs_display_header_summary));

    // Widget parlant
    final CheckBoxPreference speakPref = new CheckBoxPreference(this);
    final String speakKey = getKeyForWidget(resources.getString(R.string.config_widget_speak_key), appWidgetId);
    speakPref.setKey(speakKey);
    final String speakDefaultValue = resources.getString(R.string.config_widget_speak_default);
    speakPref.setDefaultValue(Boolean.valueOf(speakDefaultValue));
    speakPref.setTitle(resources.getString(R.string.label_widget_prefs_speak));

    // Widget parlant avec ecran eteint
    final CheckBoxPreference speakBlackPref = new CheckBoxPreference(this);
    final String speakBlackKey = getKeyForWidget(resources.getString(R.string.config_widget_speak_black_key), appWidgetId);
    speakBlackPref.setKey(speakBlackKey);
    final String speakBlackDefaultValue = resources.getString(R.string.config_widget_speak_black_default);
    speakBlackPref.setDefaultValue(Boolean.valueOf(speakBlackDefaultValue));
    speakBlackPref.setTitle(resources.getString(R.string.label_widget_prefs_speak_black));
    speakBlackPref.setSummary(resources.getString(R.string.label_widget_prefs_speak_black_summary));

    // Widget parlant seulement sur les balises affichees
    final CheckBoxPreference speakAllPref = new CheckBoxPreference(this);
    final String speakAllKey = getKeyForWidget(resources.getString(R.string.config_widget_speak_all_key), appWidgetId);
    speakAllPref.setKey(speakAllKey);
    final String speakAllDefaultValue = resources.getString(R.string.config_widget_speak_all_default);
    speakAllPref.setDefaultValue(Boolean.valueOf(speakAllDefaultValue));
    speakAllPref.setTitle(resources.getString(R.string.label_widget_prefs_speak_all));
    speakAllPref.setSummary(resources.getString(R.string.label_widget_prefs_speak_all_summary));

    // Bouton de suppression du widget
    final Preference deletePref;
    if (deleteButton)
    {
      deletePref = new Preference(this);
      final String deleteKey = getKeyForWidget(resources.getString(R.string.config_widget_delete_key), appWidgetId);
      deletePref.setKey(deleteKey);
      deletePref.setTitle(resources.getString(R.string.label_widget_prefs_delete));
      deletePref.setSummary(resources.getString(R.string.label_widget_prefs_delete_summary));
      deletePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
      {
        @Override
        public boolean onPreferenceClick(final Preference pref)
        {
          cleanWidget(appWidgetId);
          //BalisesWidgets.
          AbstractBalisesWidgetPreferencesActivity.this.finish();
          return true;
        }
      });
    }
    else
    {
      deletePref = null;
    }

    // Sauvegarde
    listePreferences.add(listePref);
    appwidgetIds.add(Integer.valueOf(appWidgetId));

    // Listeners
    onPreferenceChange(listePref, sharedPreferences.getString(listeKey, listeDefaultValue));
    listePref.setOnPreferenceChangeListener(this);

    // Ajout au groupe
    group.addPreference(listePref);
    group.addPreference(displayWeatherPref);
    group.addPreference(displayHeaderPref);
    group.addPreference(displayTitlePref);
    group.addPreference(speakPref);
    group.addPreference(speakBlackPref);
    group.addPreference(speakAllPref);
    if (deleteButton)
    {
      group.addPreference(deletePref);
    }

    // Dependences
    updateDependencies(group, appWidgetId);
  }

  /**
   * 
   * @param group
   * @param appWidgetId
   */
  private void updateDependencies(final PreferenceGroup group, final int appWidgetId)
  {
    // Gestion synthese vocale
    final String speakKey = getKeyForWidget(resources.getString(R.string.config_widget_speak_key), appWidgetId);

    final String speakBlackKey = getKeyForWidget(resources.getString(R.string.config_widget_speak_black_key), appWidgetId);
    final Preference speakBlackPref = group.findPreference(speakBlackKey);
    speakBlackPref.setDependency(speakKey);

    final String speakAllKey = getKeyForWidget(resources.getString(R.string.config_widget_speak_all_key), appWidgetId);
    final Preference speakAllPref = group.findPreference(speakAllKey);
    speakAllPref.setDependency(speakKey);
  }

  /**
   * 
   * @return
   */
  @SuppressWarnings("static-method")
  protected int getDefautListValue()
  {
    return R.string.config_widget_label_value_proximity;
  }

  /**
   * 
   */
  void updateListePrefs()
  {
    final Iterator<Integer> appwidgetIdsIterator = appwidgetIds.iterator();
    for (final ListPreference listePref : listePreferences)
    {
      updateListePref(listePref, appwidgetIdsIterator.next().intValue());
    }
  }

  /**
   * 
   * @param listePref
   * @param appwidgetId
   */
  private void updateListePref(final ListPreference listePref, final int appwidgetId)
  {
    // Initialisations
    final int nbLabels = providersService.getFavoritesService().getLabels().size();
    final boolean aroundModeAvailable = isAroundModeAvailable(appwidgetId);

    // Nombre items
    final int nbFixedItems = (aroundModeAvailable ? 2 : 1);
    final String[] entries = new String[nbLabels + nbFixedItems];
    final String[] entryValues = new String[nbLabels + nbFixedItems];

    // Mode proximite
    entries[0] = resources.getString(R.string.label_proximity);
    entryValues[0] = resources.getString(R.string.config_widget_label_value_proximity);

    // Mode around
    if (aroundModeAvailable)
    {
      entries[1] = resources.getString(R.string.label_around);
      entryValues[1] = resources.getString(R.string.config_widget_label_value_around);
    }

    // Labels
    for (int i = nbFixedItems; i < nbLabels + nbFixedItems; i++)
    {
      entries[i] = providersService.getFavoritesService().getLabels().get(i - nbFixedItems);
      entryValues[i] = providersService.getFavoritesService().getLabels().get(i - nbFixedItems);
    }
    listePref.setEntries(entries);
    listePref.setEntryValues(entryValues);
  }

  /**
   * 
   * @return
   */
  private boolean isAroundModeAvailable(final int appwidgetId)
  {
    final int widgetType = sharedPreferences.getInt(AbstractBalisesWidgetPreferencesActivity.getKeyForWidget(resources.getString(R.string.config_widget_type_key), appwidgetId), -1);

    switch (widgetType)
    {
      case WIDGET_TYPE_33:
        return true;

      default:
        return false;
    }
  }

  @Override
  public boolean onPreferenceChange(final Preference preference, final Object newValue)
  {
    // Liste choisie
    if (preference.getKey().startsWith(resources.getString(R.string.config_widget_label_key)))
    {
      final String stringValue = (String)newValue;
      final String proximityValue = resources.getString(R.string.config_widget_label_value_proximity);
      final String proximityDisplay = resources.getString(R.string.label_proximity);
      final String aroundValue = resources.getString(R.string.config_widget_label_value_around);
      final String aroundDisplay = resources.getString(R.string.label_around);
      preference.setSummary(stringValue.equals(proximityValue) ? proximityDisplay : stringValue.equals(aroundValue) ? aroundDisplay : stringValue);

      return true;
    }

    return false;
  }

  /**
   * 
   */
  private void manageWidget()
  {
    // Initialisations
    final Intent intent = getIntent();

    // Recuperation de l'ID du widget, d'abord directement
    if (intent.getExtras() != null)
    {
      launchAppWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    }
    Log.d(getClass().getSimpleName(), "launchAppWidgetId = " + launchAppWidgetId);
  }

  /**
   * Pour supprimer un widget
   * 
   * @param appWidgetId
   */
  void cleanWidget(final int appWidgetId)
  {
    final AppWidgetHost host = new AppWidgetHost(getApplicationContext(), 0);
    Log.d(getClass().getSimpleName(), "Deleting appwidget " + appWidgetId);
    host.deleteAppWidgetId(appWidgetId);
  }

  /**
   * 
   * @return
   */
  public abstract int getWidgetType();
}
