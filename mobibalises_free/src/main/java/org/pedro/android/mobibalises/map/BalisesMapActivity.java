package org.pedro.android.mobibalises.map;

import org.pedro.android.mobibalises.FreeActivityCommons;
import org.pedro.android.mobibalises.R;
import org.pedro.android.mobibalises.service.ProvidersService;
import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.map.AbstractBalisesMapActivity;
import org.pedro.android.mobibalises_common.service.AbstractProvidersService;
import org.pedro.android.mobibalises_common.service.IProvidersService;
import org.pedro.balises.BaliseProvider;

import android.app.AlertDialog;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;

/**
 * 
 * @author pedro.m
 */
public final class BalisesMapActivity extends AbstractBalisesMapActivity
{
  @Override
  public void onDestroy()
  {
    // Pour garder une trace du service pour pouvoir le fermer APRES l'appel a super.onDestroy() qui met la variable
    // providersService a null !
    final IProvidersService innerProvidersService = providersService;

    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> onDestroy()");
    super.onDestroy();

    // Fin du service
    if (innerProvidersService != null)
    {
      innerProvidersService.stopSelfIfPossible();
    }

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onDestroy()");
  }

  @Override
  protected Class<? extends AbstractProvidersService> getProvidersServiceClass()
  {
    return ProvidersService.class;
  }

  @Override
  public boolean onOptionsItemSelected(final MenuItem item)
  {
    // Super
    final boolean retour = super.onOptionsItemSelected(item);

    // Mode vol
    if (item.getItemId() == R.id.item_map_mode_flight)
    {
      // Version free
      FreeActivityCommons.promoteFullVersion(this, R.string.full_version_flight_mode, R.drawable.full_version_flight_mode, R.raw.full_version_speech);
      return true;
    }

    // Ma position
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

    // Donnees (balises et/ou sites)
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
      // Version free
      FreeActivityCommons.promoteFullVersion(this, R.string.full_version_list_mode, R.drawable.full_version_list_mode, -1);
      return true;
    }

    // Gestion des listes
    else if (item.getItemId() == R.id.item_map_labels)
    {
      // Version free
      FreeActivityCommons.promoteFullVersion(this, R.string.full_version_list_mode, R.drawable.full_version_list_mode, -1);
      return true;
    }

    // Ajout des balises visibles a une liste
    else if (item.getItemId() == R.id.item_map_add_to_label)
    {
      // Version free
      FreeActivityCommons.promoteFullVersion(this, R.string.full_version_list_mode, R.drawable.full_version_list_mode, -1);
      return true;
    }

    // Mode historique
    else if (item.getItemId() == R.id.item_map_mode_history)
    {
      // Version free
      FreeActivityCommons.promoteFullVersion(this, R.string.full_version_history_mode, R.drawable.full_version_history_mode, -1);
      return true;
    }

    // Mode alarme
    else if (item.getItemId() == R.id.item_map_mode_alarm)
    {
      // Version free
      FreeActivityCommons.promoteFullVersion(this, R.string.full_version_alarm_mode, R.drawable.full_version_alarm_mode, -1);
      return true;
    }

    // Gestion des alarmes
    else if (item.getItemId() == R.id.item_map_alarms)
    {
      FreeActivityCommons.promoteFullVersion(this, R.string.full_version_alarm_mode, R.drawable.full_version_alarm_mode, -1);
      return true;
    }

    // Preferences
    else if (item.getItemId() == R.id.item_map_preferences)
    {
      preferences();
      return true;
    }

    // Preferences des widgets
    else if (item.getItemId() == R.id.item_map_widget_preferences)
    {
      // Version free
      FreeActivityCommons.promoteFullVersion(this, R.string.full_version_widgets, R.drawable.full_version_widgets, -1);
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

  @Override
  public boolean onContextItemSelected(final MenuItem item)
  {
    if (item.getGroupId() == 0)
    {
      // Balise
      if (item.getItemId() == R.id.item_context_balise_historique)
      {
        historiqueBalise();
        return true;
      }
      else if (item.getItemId() == R.id.item_context_balise_lien_detail_web)
      {
        lienDetailBalise();
        return true;
      }
      else if (item.getItemId() == R.id.item_context_balise_lien_historique_web)
      {
        lienHistoriqueBalise();
        return true;
      }
      else if (item.getItemId() == R.id.item_context_balise_tooltip)
      {
        baliseTooltip();
        return true;
      }
      else if (item.getItemId() == R.id.item_context_balise_speak)
      {
        FreeActivityCommons.promoteFullVersion(this, R.string.full_version_speech, -1, R.raw.full_version_speech);
        return true;
      }
      else if (item.getItemId() == R.id.item_context_balise_alarm)
      {
        FreeActivityCommons.promoteFullVersion(this, R.string.full_version_alarm_mode, R.drawable.full_version_alarm_mode, -1);
        return true;
      }
      else if (item.getItemId() == R.id.item_context_balise_favorite)
      {
        FreeActivityCommons.promoteFullVersion(this, R.string.full_version_list_mode, R.drawable.full_version_list_mode, -1);
        return true;
      }

      // Spot
      else if (item.getItemId() == R.id.item_context_spot_infos)
      {
        infosSpot(contextMenuSpotItem);
        return true;
      }
      else if (item.getItemId() == R.id.item_context_spot_navigate)
      {
        navigateToSpot();
        return true;
      }
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
   */
  private void historiqueBalise()
  {
    FreeActivityCommons.promoteFullVersion(this, R.string.full_version_history_mode, R.drawable.full_version_history_mode, -1);
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

    // Infobulle
    final MenuItem tooltipItem = menu.findItem(R.id.item_context_balise_tooltip);
    tooltipItem.setVisible(false);

    // Synthese vocale
    final MenuItem voiceItem = menu.findItem(R.id.item_context_balise_speak);
    voiceItem.setVisible(true);
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
      FreeActivityCommons.promoteFullVersion(this, R.string.full_version_speech, -1, R.raw.full_version_speech);
    }
  }

  @Override
  public void onBaliseInfoLinkTap(final BaliseProvider provider, final String providerKey, final String idBalise)
  {
    historiqueBalise();
  }

  @Override
  protected boolean onMapLayerClicked(final AlertDialog dialog, final int item, final boolean checked)
  {
    if (item == 1) /* Balises - Meteo */
    {
      // On empeche de cocher l'option
      dialog.getListView().setItemChecked(item, false);

      // Promo pour la version complete
      FreeActivityCommons.promoteFullVersion(this, R.string.full_version_weather_icons, R.drawable.full_version_weather_icons, -1);

      // Retour a faux si cochage demande, pour ne pas prendre en compte
      // Retour a vrai si decochage demande, pour prendre en compte
      return !checked;
    }

    // Par defaut
    return super.onMapLayerClicked(dialog, item, checked);
  }

  @Override
  protected void initCommons()
  {
    super.initCommons();
    FreeActivityCommons.init(getApplicationContext());
  }

  @Override
  protected String getProdIgnApiKey()
  {
    return IGN_FREE_API_KEY;
  }
}
