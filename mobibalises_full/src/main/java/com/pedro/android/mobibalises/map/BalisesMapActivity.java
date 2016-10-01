package com.pedro.android.mobibalises.map;

import java.util.List;

import org.pedro.android.map.MapView;
import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.Strings;
import org.pedro.android.mobibalises_common.analytics.AnalyticsService;
import org.pedro.android.mobibalises_common.map.AbstractBalisesMapActivity;
import org.pedro.android.mobibalises_common.map.BaliseItem;
import org.pedro.android.mobibalises_common.map.ItemsOverlay;
import org.pedro.android.mobibalises_common.map.LocationOverlay;
import org.pedro.android.mobibalises_common.service.AbstractProvidersService;
import org.pedro.balises.BaliseProvider;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.pedro.android.mobibalises.FullActivityCommons;
import com.pedro.android.mobibalises.FullActivityCommons.FavoriteLabelChooserListener;
import com.pedro.android.mobibalises.FullActivityCommons.FavoritesLabelsChooserListener;
import com.pedro.android.mobibalises.R;
import com.pedro.android.mobibalises.alarm.AlarmUtils;
import com.pedro.android.mobibalises.alarm.AlarmsFragmentActivity;
import com.pedro.android.mobibalises.favorites.BaliseFavorite;
import com.pedro.android.mobibalises.service.IFullProvidersService;
import com.pedro.android.mobibalises.service.MobibalisesLicenseChecker;
import com.pedro.android.mobibalises.service.ProvidersService;
import com.pedro.android.mobibalises.view.FullBaliseDrawable;
import com.pedro.android.mobibalises.voice.VoiceService;
import com.pedro.android.mobibalises.widget.BalisesWidgets;

/**
 * 
 * @author pedro.m
 */
public final class BalisesMapActivity extends AbstractBalisesMapActivity implements FavoritesLabelsChooserListener, LicenseCheckerCallback, FullLocationOverlay.FlightModeCancelListener
{
  // Constantes
  private static final int          FLIGHT_MODE_LOCATION_MIN_TIME     = 10000;
  private static final float        FLIGHT_MODE_LOCATION_MIN_DISTANCE = 10;

  // Service "full"
  IFullProvidersService             fullProvidersService;

  // Flag
  private boolean                   otherActivityLaunched;

  // Mode vol
  private boolean                   flightModeAsked;

  // Licence
  private MobibalisesLicenseChecker licenseChecker;

  @Override
  protected void onCreate(final Bundle savedInstanceState)
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> onCreate()");

    // Parent
    super.onCreate(savedInstanceState);

    // Verification de la license
    if (FullActivityCommons.needsLicenseCheck(getApplicationContext()))
    {
      licenseChecker = FullActivityCommons.initLicenseChecker(getApplicationContext(), this);
      licenseChecker.checkAccess();
    }

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onCreate()");
  }

  @Override
  protected void onPause()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> onPause()");
    super.onPause();

    // Si pause (pas d'autre activite demandee)
    if (!otherActivityLaunched && !preferencesLaunched && (fullProvidersService != null))
    {
      FullActivityCommons.addFlightModeNotification(getApplicationContext(), fullProvidersService, false);
      FullActivityCommons.addHistoryModeNotification(getApplicationContext());
      FullActivityCommons.addSpeakBlackWidgetNotification(getApplicationContext());
      FullActivityCommons.addAlarmModeNotification(getApplicationContext());
      fullProvidersService.setActivityOnForeground(false);
    }

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onPause()");
  }

  @Override
  protected void onResume()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> onResume()");
    super.onResume();

    // Notification mode vol et historique
    FullActivityCommons.removeFlightModeNotification(getApplicationContext());
    FullActivityCommons.removeHistoryModeNotification(getApplicationContext());
    FullActivityCommons.removeSpeakBlackWidgetNotification(getApplicationContext());
    FullActivityCommons.removeAlarmModeNotification(getApplicationContext());
    if (fullProvidersService != null)
    {
      fullProvidersService.setActivityOnForeground(true);
    }

    // RAZ flag de lancement d'un autre activite
    otherActivityLaunched = false;

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onResume()");
  }

  @Override
  public void onStop()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> onStop()");
    super.onStop();

    // Fin
    Log.d(getClass().getSimpleName(), ">>> onStop()");
  }

  @Override
  protected void onDestroy()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> onDestroy()");

    // Localisation
    if (fullProvidersService != null)
    {
      fullProvidersService.getFullLocationService().removeListener(locationListener, true);
    }

    // Synthese vocale
    if (fullProvidersService != null)
    {
      fullProvidersService.getVoiceService().unregisterVoiceClient(VoiceService.MAP_VOICE_CLIENT);
    }

    // Parent
    super.onDestroy();

    // Si fin (pas d'autre activite demandee)
    if (!otherActivityLaunched && (fullProvidersService != null))
    {
      fullProvidersService.stopSelfIfPossible();
    }

    // License
    if (licenseChecker != null)
    {
      licenseChecker.onDestroy();
    }

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onDestroy()");
  }

  @Override
  protected void initCommons()
  {
    super.initCommons();
    FullActivityCommons.init(getApplicationContext());
    FullActivityCommons.removeFlightModeNotification(getApplicationContext());
    FullActivityCommons.removeHistoryModeNotification(getApplicationContext());
    FullActivityCommons.removeSpeakBlackWidgetNotification(getApplicationContext());
    FullActivityCommons.removeAlarmModeNotification(getApplicationContext());
  }

  @Override
  protected void initGraphics()
  {
    // Appel du parent
    super.initGraphics();

    // Gestion de la densite de pixels de l'ecran
    FullBaliseDrawable.initGraphics(getApplicationContext());
  }

  @Override
  protected LocationOverlay getNewLocationOverlay(final MapView mapView)
  {
    return new FullLocationOverlay(mapView, this);
  }

  /**
   * 
   * @return
   */
  private FullLocationOverlay getFullLocationOverlay()
  {
    return (FullLocationOverlay)locationOverlay;
  }

  @Override
  protected Class<? extends AbstractProvidersService> getProvidersServiceClass()
  {
    return ProvidersService.class;
  }

  @Override
  protected void onServiceBinded()
  {
    fullProvidersService = (IFullProvidersService)providersService;
  }

  @Override
  protected void onServiceConnected()
  {
    // Localisation si mode vol
    if (FullActivityCommons.isFlightMode(fullProvidersService))
    {
      // Boite de dialogue de progression
      showLocationProgressDialog();

      // Mode vol pour le layer de positionnement
      getFullLocationOverlay().setFlightMode(true, this);

      // Enregistrement aupres du service de localisation
      final boolean useGps = sharedPreferences.getBoolean(resources.getString(R.string.config_flight_mode_use_gps_key), Boolean.parseBoolean(resources.getString(R.string.config_flight_mode_use_gps_default)));
      fullProvidersService.getFullLocationService().addListener(locationListener, useGps, FLIGHT_MODE_LOCATION_MIN_TIME, FLIGHT_MODE_LOCATION_MIN_DISTANCE, true);
    }
    else if (doLocationAtStartup)
    {
      super.onServiceConnected();
    }

    // Notification activite en premier plan
    fullProvidersService.setActivityOnForeground(true);

    // Notification au service de synthese vocale
    fullProvidersService.getVoiceService().registerVoiceClient(VoiceService.MAP_VOICE_CLIENT);
  }

  @Override
  protected void onLocationSettingsReturn()
  {
    // Mode vol ?
    if (flightModeAsked)
    {
      flightModeAsked = false;
      if (fullProvidersService.getFullLocationService().isLocationEnabled())
      {
        // Boite de dialogue de progression
        showLocationProgressDialog();

        // Activation du mode vol
        toggleFlightMode();
      }
    }
    // Mode proximite
    else
    {
      super.onLocationSettingsReturn();
    }
  }

  @Override
  public boolean onPrepareOptionsMenu(final Menu menu)
  {
    // Gestion du menu mode vol
    final MenuItem flightModeItem = menu.findItem(R.id.item_map_mode_flight);
    final boolean flightMode = FullActivityCommons.manageFlightModeMenuItem(flightModeItem, fullProvidersService);

    // Gestion du menu "ma position"
    final MenuItem locationItem = menu.findItem(R.id.item_map_location);
    locationItem.setEnabled(!flightMode);

    // Gestion du menu mode historique
    final MenuItem historyModeItem = menu.findItem(R.id.item_map_mode_history);
    FullActivityCommons.manageHistoryModeMenuItem(historyModeItem, fullProvidersService);

    // Gestion du menu mode alarme
    final MenuItem alarmModeItem = menu.findItem(R.id.item_map_mode_alarm);
    FullActivityCommons.manageAlarmModeMenuItem(alarmModeItem, fullProvidersService);

    return super.onPrepareOptionsMenu(menu);
  }

  /**
   * 
   */
  boolean isCurrentBaliseWindDatasDisplayed()
  {
    return currentBaliseWindDatasDisplayed;
  }

  /**
   * 
   */
  boolean isCurrentBaliseWeatherDatasDisplayed()
  {
    return currentBaliseWeatherDatasDisplayed;
  }

  /**
   * 
   * @return
   */
  ItemsOverlay getItemsOverlay()
  {
    return itemsOverlay;
  }

  @Override
  public boolean onOptionsItemSelected(final MenuItem item)
  {
    // Super
    boolean retour = super.onOptionsItemSelected(item);

    // Mode vol
    if (item.getItemId() == R.id.item_map_mode_flight)
    {
      // Si pas deja en mode vol
      if (!FullActivityCommons.isFlightMode(fullProvidersService))
      {
        // Initialisation
        flightModeAsked = true;

        // Verification des parametres de localisation
        if (!fullProvidersService.getFullLocationService().isLocationEnabled())
        {
          // GPS et GSM non actifs, question pour redirection vers les parametres de localisation
          ActivityCommons.locationSettingsDialog(this, null);
          return true;
        }

        // Boite de dialogue de progression
        showLocationProgressDialog();
      }

      // Mode vol
      toggleFlightMode();

      // Fin
      return true;
    }

    // Localisation
    else if (item.getItemId() == R.id.item_map_location)
    {
      location();
      return true;
    }

    // Type de carte
    else if (item.getItemId() == R.id.item_map_maptype)
    {
      mapType();
      return true;
    }

    // Donnees affichees sur la carte (balise et/ou sites)
    else if (item.getItemId() == R.id.item_map_mapdatas)
    {
      mapLayers();
      return true;
    }

    // Recherche
    else if (item.getItemId() == R.id.item_map_search)
    {
      search();
      return true;
    }

    // Infos donnees
    else if (item.getItemId() == R.id.item_map_datainfos)
    {
      dataInfos();
      return true;
    }

    // Mode liste
    else if (item.getItemId() == R.id.item_map_mode_list)
    {
      // Version full
      otherActivityLaunched = true;
      final Intent intent = new Intent(getApplicationContext().getString(R.string.intent_favorites_action));
      startActivity(intent);
      finish();
      return true;
    }

    // Mode historique
    else if (item.getItemId() == R.id.item_map_mode_history)
    {
      // Mode historique
      toggleHistoryMode();

      // Fin
      return true;
    }

    // Mode alarme
    else if (item.getItemId() == R.id.item_map_mode_alarm)
    {
      // Mode alarme
      toggleAlarmMode();

      // Fin
      return true;
    }

    // Gestion des alarmes
    else if (item.getItemId() == R.id.item_map_alarms)
    {
      launchAlarmManagement(-1);
      return true;
    }

    // Gestion des listes
    else if (item.getItemId() == R.id.item_map_labels)
    {
      otherActivityLaunched = true;
      final Intent intent = new Intent(getApplicationContext().getString(R.string.intent_favorites_labels_action));
      startActivity(intent);
      return true;
    }

    // Ajout des balises visibles a une liste
    else if (item.getItemId() == R.id.item_map_add_to_label)
    {
      final FavoriteLabelChooserListener listener = new FavoriteLabelChooserListener()
      {
        @Override
        public void onProximityModeChoosed()
        {
          //Nothing
        }

        @Override
        public void onFavoriteLabelChoosed(final String label)
        {
          if (isCurrentBaliseWindDatasDisplayed() || isCurrentBaliseWeatherDatasDisplayed())
          {
            // Recuperation des balises visibles
            final List<BaliseItem> visibles = getItemsOverlay().getVisibleBalises();

            // Si il y a des balises visibles...
            if ((visibles != null) && (visibles.size() > 0))
            {
              // Ajout de chaque balise visible
              for (final BaliseItem visible : visibles)
              {
                // Ajout de la balise visible
                final BaliseFavorite favorite = new BaliseFavorite(visible.providerKey, visible.baliseId, fullProvidersService.getFavoritesService());
                fullProvidersService.getFavoritesService().addBaliseFavorite(favorite, label);
              }

              // Sauvegarde des favoris
              fullProvidersService.getFavoritesService().saveBalisesFavorites();
            }
          }
        }
      };
      FullActivityCommons.chooseFavoriteLabel(this, fullProvidersService, listener, null, true, false, false, FullActivityCommons.isFlightMode(fullProvidersService));
      return true;
    }

    // Preferences
    else if (item.getItemId() == R.id.item_map_preferences)
    {
      otherActivityLaunched = true;
      preferences();
      return true;
    }

    // Preferences des widgets
    else if (item.getItemId() == R.id.item_map_widget_preferences)
    {
      FullActivityCommons.widgetPreferences(this);
      return true;
    }

    // Message FFVL
    else if (item.getItemId() == R.id.item_map_ffvl_message)
    {
      ActivityCommons.checkForFfvlMessage(this, AbstractProvidersService.FFVL_KEY, true, true);
      return true;
    }

    // A propos
    else if (item.getItemId() == R.id.item_map_about)
    {
      ActivityCommons.about(this);
      return true;
    }

    // Aide
    else if (item.getItemId() == R.id.item_map_help)
    {
      ActivityCommons.goToUrl(this, ActivityCommons.HELP_URL);
      return true;
    }

    // Quoi de neuf ?
    else if (item.getItemId() == R.id.item_map_whatsnew)
    {
      ActivityCommons.displayWhatsNewMessage(this, true);
      return true;
    }

    // Aucun
    else
    {
      return retour;
    }
  }

  /**
   * 
   * @param position
   */
  private void launchAlarmManagement(final int position)
  {
    otherActivityLaunched = true;

    // Alarmes
    final Intent intent = new Intent(getApplicationContext().getString(R.string.intent_alarms_action));
    if (position >= 0)
    {
      intent.putExtra(AlarmsFragmentActivity.PARAM_POSITION, position);
    }
    startActivity(intent);
  }

  /**
   * 
   */
  private void showLocationProgressDialog()
  {
    ActivityCommons.progressDialog(this, ActivityCommons.PROGRESS_DIALOG_LOCATION, resources.getString(R.string.app_name), resources.getString(R.string.message_location_progress), true, true, new DialogInterface.OnCancelListener()
    {
      @Override
      public void onCancel(final DialogInterface dialog)
      {
        // Fin du mode vol
        cancelFlightMode();
      }
    });
  }

  /**
   * 
   */
  private void toggleFlightMode()
  {
    // Bascule
    if (fullProvidersService != null)
    {
      fullProvidersService.setFlightMode(!fullProvidersService.isFlightMode());
    }
    final boolean flightMode = fullProvidersService.isFlightMode();

    // Position courante
    getFullLocationOverlay().setFlightMode(flightMode, this);

    // Passage en mode vol
    if (flightMode)
    {
      final boolean useGps = sharedPreferences.getBoolean(resources.getString(R.string.config_flight_mode_use_gps_key), Boolean.parseBoolean(resources.getString(R.string.config_flight_mode_use_gps_default)));
      fullProvidersService.getFullLocationService().addListener(locationListener, useGps, FLIGHT_MODE_LOCATION_MIN_TIME, FLIGHT_MODE_LOCATION_MIN_DISTANCE, true);
    }
    // Sinon effacement du point de localisation
    else
    {
      fullProvidersService.getFullLocationService().removeListener(locationListener, true);
    }

    // Gestion du wakelock
    ActivityCommons.manageWakeLockConfig(getApplicationContext(), flightMode);
    ActivityCommons.acquireWakeLock();
  }

  /**
   * 
   */
  void cancelFlightMode()
  {
    // Bascule
    if (fullProvidersService != null)
    {
      fullProvidersService.setFlightMode(false);
      fullProvidersService.getFullLocationService().removeListener(locationListener, true);
    }

    // Position courante
    getFullLocationOverlay().setFlightMode(false, null);

    // Gestion du wakelock
    ActivityCommons.manageWakeLockConfig(getApplicationContext(), false);
    ActivityCommons.acquireWakeLock();
  }

  @Override
  protected boolean isFlightMode()
  {
    return FullActivityCommons.isFlightMode(fullProvidersService);
  }

  @Override
  public boolean onContextItemSelected(final MenuItem item)
  {
    if (item.getGroupId() == 0)
    {
      // Historique balise
      if (item.getItemId() == R.id.item_context_balise_historique)
      {
        historiqueBalise(contextMenuBaliseProvider, contextMenuBaliseProviderKey, contextMenuIdBalise);
        return true;
      }
      // Detail Web
      else if (item.getItemId() == R.id.item_context_balise_lien_detail_web)
      {
        lienDetailBalise();
        return true;
      }
      // Historique web
      else if (item.getItemId() == R.id.item_context_balise_lien_historique_web)
      {
        lienHistoriqueBalise();
        return true;
      }
      // Infos
      else if (item.getItemId() == R.id.item_context_balise_tooltip)
      {
        baliseTooltip();
        return true;
      }
      // Synthese vocale
      else if (item.getItemId() == R.id.item_context_balise_speak)
      {
        speak();
        return true;
      }
      // Ajout alarme
      else if (item.getItemId() == R.id.item_context_balise_alarm)
      {
        final int position = AlarmUtils.addNewAlarm(getApplicationContext(), fullProvidersService, contextMenuBaliseProviderKey, contextMenuIdBalise, null);
        launchAlarmManagement(position);
        return true;
      }
      // Ajout en favorite
      else if (item.getItemId() == R.id.item_context_balise_favorite)
      {
        if (fullProvidersService.getFavoritesService().isBaliseFavorite(contextMenuBaliseProviderKey, contextMenuIdBalise))
        {
          final List<String> favoriteLabels = fullProvidersService.getFavoritesService().getBaliseFavoriteLabels(contextMenuBaliseProviderKey, contextMenuIdBalise);
          FullActivityCommons.chooseFavoritesLabels(this, fullProvidersService, this, favoriteLabels);
        }
        else
        {
          FullActivityCommons.chooseFavoritesLabels(this, fullProvidersService, this, null);
        }
        return true;
      }

      // Infos spot
      else if (item.getItemId() == R.id.item_context_spot_infos)
      {
        infosSpot(contextMenuSpotItem);
        return true;
      }
      // Lien web
      else if (item.getItemId() == R.id.item_context_spot_navigate)
      {
        navigateToSpot();
        return true;
      }
      // Details
      else if (item.getItemId() == R.id.item_context_spot_lien_detail)
      {
        lienDetailSpot();
        return true;
      }
    }

    return super.onContextItemSelected(item);
  }

  /**
   * 
   * @param menu
   * @param view
   * @param menuInfo
   */
  @Override
  protected void onCreateBaliseContextMenu(final ContextMenu menu, final View view, final ContextMenu.ContextMenuInfo menuInfo)
  {
    // Super
    super.onCreateBaliseContextMenu(menu, view, menuInfo);

    // Action sur touch
    final String touchAction = sharedPreferences.getString(resources.getString(R.string.config_map_touch_action_key), resources.getString(R.string.config_map_touch_action_default));
    final String bothTouchAction = resources.getString(R.string.config_map_touch_action_both);

    // Infobulle
    final MenuItem tooltipItem = menu.findItem(R.id.item_context_balise_tooltip);
    final String tooltipTouchAction = resources.getString(R.string.config_map_touch_action_tooltip);
    tooltipItem.setVisible(!tooltipTouchAction.equals(touchAction) && !bothTouchAction.equals(touchAction));

    // Synthese vocale
    final MenuItem voiceItem = menu.findItem(R.id.item_context_balise_speak);
    final String speakTouchAction = resources.getString(R.string.config_map_touch_action_speech);
    voiceItem.setVisible(!speakTouchAction.equals(touchAction) && !bothTouchAction.equals(touchAction));
    if (!fullProvidersService.getVoiceService().canBeAvailable())
    {
      voiceItem.setEnabled(false);
    }
  }

  @Override
  public void onFavoritesLabelsChoosed(final List<String> labels)
  {
    // Sauvegarde
    FullActivityCommons.updateAndSaveBaliseFavoriteLabels(fullProvidersService, contextMenuBaliseProviderKey, contextMenuIdBalise, labels);

    // MAJ des widgets
    BalisesWidgets.synchronizeWidgets(getApplicationContext(), fullProvidersService, null);
  }

  @Override
  public boolean onKeyUp(final int keyCode, final KeyEvent event)
  {
    // Gestion de la touche "back"
    if (keyCode == KeyEvent.KEYCODE_BACK)
    {
      // Gestion de la synthese vocale
      if ((fullProvidersService != null) && fullProvidersService.getVoiceService().stopSpeaking(false))
      {
        return true;
      }
    }

    return super.onKeyUp(keyCode, event);
  }

  @Override
  public void onBaliseTap(final BaliseProvider provider, final String providerKey, final String idBalise)
  {
    // Super
    super.onBaliseTap(provider, providerKey, idBalise);

    // Action sur touch
    final String touchAction = sharedPreferences.getString(resources.getString(R.string.config_map_touch_action_key), resources.getString(R.string.config_map_touch_action_default));
    final String speakTouchAction = resources.getString(R.string.config_map_touch_action_speech);
    final String bothTouchAction = resources.getString(R.string.config_map_touch_action_both);
    if (speakTouchAction.equals(touchAction) || bothTouchAction.equals(touchAction))
    {
      final String finalId = idBalise.substring(idBalise.indexOf(Strings.CHAR_POINT) + 1);
      fullProvidersService.getVoiceService().speakBaliseReleve(provider.getBaliseById(finalId), provider.getReleveById(finalId), true, this);
    }
  }

  @Override
  public void onBaliseInfoLinkTap(final BaliseProvider provider, final String providerKey, final String idBalise)
  {
    historiqueBalise(provider, providerKey, idBalise);
  }

  /**
   * 
   * @param provider
   * @param providerKey
   * @param idBalise
   */
  private void historiqueBalise(final BaliseProvider provider, final String providerKey, final String idBalise)
  {
    otherActivityLaunched = true;
    FullActivityCommons.historiqueBalise(this, sharedPreferences, fullProvidersService, provider, providerKey, idBalise);
  }

  /**
   * 
   */
  private void speak()
  {
    ActivityCommons.trackEvent(fullProvidersService, AnalyticsService.CAT_MAP, AnalyticsService.ACT_MAP_BALISE_SPEECH_CLICKED, contextMenuBaliseProviderKey + "." + contextMenuIdBalise);
    fullProvidersService.getVoiceService().speakBaliseReleve(contextMenuBaliseProvider.getBaliseById(contextMenuIdBalise), contextMenuBaliseProvider.getReleveById(contextMenuIdBalise), true, this);
  }

  @Override
  public void allow(final int reason)
  {
    // Transmission au service
    if (fullProvidersService != null)
    {
      fullProvidersService.allow(reason);
    }
  }

  @Override
  public void dontAllow(final int reason)
  {
    // Dialogue info
    FullActivityCommons.unlicensedDialog(this, licenseChecker);
  }

  @Override
  public void applicationError(final int errorCode)
  {
    // Dialogue info
    FullActivityCommons.unlicensedDialog(this, licenseChecker);
  }

  @Override
  public void onFlightModeCanceled()
  {
    cancelFlightMode();
  }

  /**
   * 
   */
  private void toggleHistoryMode()
  {
    FullActivityCommons.toggleHistoryMode(getApplicationContext(), fullProvidersService);
  }

  /**
   * 
   */
  private void toggleAlarmMode()
  {
    FullActivityCommons.toggleAlarmMode(getApplicationContext(), fullProvidersService);
  }

  @Override
  protected String getProdIgnApiKey()
  {
    return (FullActivityCommons.isFriend(getApplicationContext()) ? AbstractBalisesMapActivity.IGN_FREE_API_KEY : "");
  }
}
