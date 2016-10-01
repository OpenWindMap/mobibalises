package com.pedro.android.mobibalises.voice;

import java.io.File;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.Strings;
import org.pedro.android.mobibalises_common.provider.BaliseProviderUtils;
import org.pedro.android.mobibalises_common.provider.BaliseProviderUtils.TendanceVent;
import org.pedro.android.mobibalises_common.view.BaliseDrawable;
import org.pedro.balises.Balise;
import org.pedro.balises.Releve;
import org.pedro.balises.Utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Build;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.util.FloatMath;
import android.util.Log;

import com.pedro.android.mobibalises.R;

/**
 * 
 * @author pedro.m
 */
public final class VoiceService implements OnInitListener
{
  public static final int                    MAP_VOICE_CLIENT         = 1;
  public static final int                    FAVORITES_VOICE_CLIENT   = 2;
  public static final int                    FLIGHT_MODE_VOICE_CLIENT = 3;
  public static final int                    WIDGETS_VOICE_CLIENT     = 4;

  private static final String                STRING_ZERO_ESPACE       = "0 ";
  private static final String                STRING_ZERO_HEURE_PREFIX = "0:";
  private static final String                STRING_VIRGULE_SPACE     = ", ";
  private static final File                  SDCARD_PATH              = Environment.getExternalStorageDirectory();
  private static final String                INTENT_FILE_DATA_SCHEME  = "file";

  private static final String                REGEXP_SAINT_ST_ESPACE   = "^(?i:st )";
  private static final String                REGEXP_SAINT_ST_POINT    = "(?i:st\\.)";
  private static final String                REMPLACEMENT_SAINT       = "Saint ";

  private static boolean                     SPEAK_ALTITUDE;
  private static boolean                     SPEAK_STATUT;
  private static boolean                     SPEAK_HEURE;
  private static boolean                     SPEAK_DIRECTION_MOYENNE;
  private static boolean                     SPEAK_DIRECTION_INSTANTANEE;
  private static boolean                     SPEAK_DIRECTION_VARIATION;
  private static boolean                     SPEAK_VITESSE_MOYENNE;
  private static boolean                     SPEAK_VITESSE_MINI;
  private static boolean                     SPEAK_VITESSE_MAXI;
  private static boolean                     SPEAK_TEMPERATURE;
  private static boolean                     SPEAK_POINT_ROSEE;
  private static boolean                     SPEAK_HYDROMETRIE;
  private static boolean                     SPEAK_NUAGES;
  private static boolean                     SPEAK_PRESSION;
  private static boolean                     SPEAK_LUMINOSITE;
  private static boolean                     SPEAK_HUMIDITE;

  private static String                      TEXTE_NOM_BALISE;
  private static String                      TEXTE_ALTITUDE;
  private static String                      TEXTE_STATUT_INACTIVE;
  private static String                      TEXTE_STATUT_PAS_DE_RELEVE;
  private static String                      TEXTE_NON_DISPONIBLE;
  private static String                      TEXTE_DIRECTION_MOYENNE;
  private static String                      TEXTE_DIRECTION_MOYENNE_ND;
  private static String                      TEXTE_DIRECTION_INSTANTANEE;
  private static String                      TEXTE_DIRECTION_INSTANTANEE_ND;
  private static String                      TEXTE_DIRECTION_VARIATION;
  private static String                      TEXTE_DIRECTION_VARIATION_ND;
  private static String                      TEXTE_VITESSE_MOYENNE;
  private static String                      TEXTE_VITESSE_MOYENNE_TEND;
  private static String                      TEXTE_VITESSE_MOYENNE_ND;
  private static String                      TEXTE_VITESSE_MINI;
  private static String                      TEXTE_VITESSE_MINI_TEND;
  private static String                      TEXTE_VITESSE_MINI_ND;
  private static String                      TEXTE_VITESSE_MAXI;
  private static String                      TEXTE_VITESSE_MAXI_TEND;
  private static String                      TEXTE_VITESSE_MAXI_ND;
  private static String                      TEXTE_HEURE_VITESSE_MAXI;
  private static String                      TEXTE_TEMPERATURE;
  private static String                      TEXTE_TEMPERATURE_ND;
  private static String                      TEXTE_POINT_ROSEE;
  private static String                      TEXTE_POINT_ROSEE_ND;
  private static String                      TEXTE_PLUIE_HYDROMETRIE;
  private static String                      TEXTE_PLUIE_HYDROMETRIE_ND;
  private static String[]                    TEXTE_PLUIES;
  private static String                      TEXTE_HYDROMETRIE;
  private static String                      TEXTE_NUAGES;
  private static String                      TEXTE_NUAGES_ND;
  private static String                      TEXTE_NUAGES_PLAFOND;
  private static String                      TEXTE_NUAGES_CB_CU;
  private static String                      TEXTE_PRESSION;
  private static String                      TEXTE_PRESSION_ND;
  private static String                      TEXTE_LUMINOSITE;
  private static String                      TEXTE_LUMINOSITE_ND;
  private static String                      TEXTE_HUMIDITE;
  private static String                      TEXTE_HUMIDITE_ND;

  private static String                      UNITE_ALTITUDE;
  private static String                      UNITE_VITESSE;
  private static String                      UNITE_TEMPERATURE;
  private static String                      UNITE_HYDROMETRIE;
  private static String                      UNITE_PRESSION;
  private static String                      UNITE_HUMIDITE;

  private static String[]                    LABELS_POINTS_CARDINAUX;
  private static float                       DELTA_POINTS_CARDINAUX;
  private static float                       DECALAGE_POINTS_CARDINAUX;

  private Context                            context;
  private final Resources                    resources;
  final Object                               speakerLock              = new Object();
  private TextToSpeech                       speaker;
  private boolean                            available                = false;
  private boolean                            sdAvailable              = false;
  boolean                                    installAsked             = false;
  private int                                deltaPeremption;
  private long                               deltaPeremptionMs;

  // Ecoute de la SDCard
  private final SDCardBroadcastReceiver      sdcardReceiver           = new SDCardBroadcastReceiver(this);

  // Clients
  private final List<Integer>                clients                  = new ArrayList<Integer>();

  // Parametres du speaker
  final HashMap<String, String>              speakerParams            = new HashMap<String, String>();

  // Focus Listener
  OnAudioFocusChangeListener                 audioFocusListener;

  // Utterances
  final List<String>                         utterances               = new ArrayList<String>();
  private final OnUtteranceCompletedListener utteranceCompletedListener;

  /**
   * 
   * @author pedro.m
   */
  private static class SDCardBroadcastReceiver extends BroadcastReceiver
  {
    private VoiceService voiceService;

    /**
     * 
     * @param voiceService
     */
    SDCardBroadcastReceiver(final VoiceService voiceService)
    {
      super();
      this.voiceService = voiceService;
    }

    @Override
    public void onReceive(final Context inContext, final Intent intent)
    {
      voiceService.refreshSDCardAvailability();
    }

    /**
     * 
     */
    void onShutdown()
    {
      voiceService = null;
    }
  }

  /**
   * 
   * @param inContext
   * @param sharedPreferences
   */
  public VoiceService(final Context inContext, final SharedPreferences sharedPreferences)
  {
    context = inContext;
    resources = inContext.getResources();
    initialize(resources);
    updatePreferences(sharedPreferences);

    refreshSDCardAvailability();
    registerSDCardListener();

    if (ActivityCommons.isFroyo())
    {
      audioFocusListener = new OnAudioFocusChangeListener()
      {
        @Override
        public void onAudioFocusChange(final int arg0)
        {
          // Nothing
        }
      };
    }
    utteranceCompletedListener = new OnUtteranceCompletedListener()
    {
      @Override
      public void onUtteranceCompleted(final String utteranceId)
      {
        Log.d(VoiceService.this.getClass().getSimpleName(), "Text finished : " + utteranceId);
        synchronized (speakerLock)
        {
          utterances.remove(utteranceId);
          Log.d(VoiceService.this.getClass().getSimpleName(), utterances.size() + " text(s) remaining...");
          if (utterances.size() <= 0)
          {
            abandonAudioFocus();
          }
        }
      }
    };
  }

  /**
   * 
   * @param resources
   */
  private static void initialize(final Resources resources)
  {
    // Labels
    TEXTE_NOM_BALISE = resources.getString(R.string.voice_balise_nom);
    TEXTE_STATUT_INACTIVE = resources.getString(R.string.voice_balise_inactive);
    TEXTE_STATUT_PAS_DE_RELEVE = resources.getString(R.string.voice_balise_aucun_releve);
    TEXTE_NON_DISPONIBLE = resources.getString(R.string.voice_balise_non_disponible);
    TEXTE_DIRECTION_MOYENNE = resources.getString(R.string.voice_balise_dir_moy);
    TEXTE_DIRECTION_MOYENNE_ND = resources.getString(R.string.voice_balise_dir_moy_nd);
    TEXTE_DIRECTION_INSTANTANEE = resources.getString(R.string.voice_balise_dir_inst);
    TEXTE_DIRECTION_INSTANTANEE_ND = resources.getString(R.string.voice_balise_dir_inst_nd);
    TEXTE_DIRECTION_VARIATION = resources.getString(R.string.voice_balise_dir_variation);
    TEXTE_DIRECTION_VARIATION_ND = resources.getString(R.string.voice_balise_dir_variation_nd);
    TEXTE_VITESSE_MOYENNE = resources.getString(R.string.voice_balise_vit_moy);
    TEXTE_VITESSE_MOYENNE_TEND = resources.getString(R.string.voice_balise_vit_moy_tend);
    TEXTE_VITESSE_MOYENNE_ND = resources.getString(R.string.voice_balise_vit_moy_nd);
    TEXTE_VITESSE_MINI = resources.getString(R.string.voice_balise_vit_min);
    TEXTE_VITESSE_MINI_TEND = resources.getString(R.string.voice_balise_vit_min_tend);
    TEXTE_VITESSE_MINI_ND = resources.getString(R.string.voice_balise_vit_min_nd);
    TEXTE_VITESSE_MAXI = resources.getString(R.string.voice_balise_vit_max);
    TEXTE_VITESSE_MAXI_TEND = resources.getString(R.string.voice_balise_vit_max_tend);
    TEXTE_VITESSE_MAXI_ND = resources.getString(R.string.voice_balise_vit_max_nd);
    TEXTE_HEURE_VITESSE_MAXI = resources.getString(R.string.voice_balise_heure_vit_max);
    TEXTE_TEMPERATURE = resources.getString(R.string.voice_balise_temp);
    TEXTE_TEMPERATURE_ND = resources.getString(R.string.voice_balise_temp_nd);
    TEXTE_POINT_ROSEE = resources.getString(R.string.voice_balise_point_rosee);
    TEXTE_POINT_ROSEE_ND = resources.getString(R.string.voice_balise_point_rosee_nd);
    TEXTE_ALTITUDE = resources.getString(R.string.voice_balise_alti);
    TEXTE_PLUIE_HYDROMETRIE = resources.getString(R.string.voice_balise_pluie_hydro);
    TEXTE_PLUIE_HYDROMETRIE_ND = resources.getString(R.string.voice_balise_pluie_hydro_nd);
    TEXTE_PLUIES = resources.getStringArray(R.array.voice_balise_pluies);
    TEXTE_HYDROMETRIE = resources.getString(R.string.voice_balise_hydrometrie);
    TEXTE_NUAGES = resources.getString(R.string.voice_balise_nuages);
    TEXTE_NUAGES_ND = resources.getString(R.string.voice_balise_nuages_nd);
    TEXTE_NUAGES_PLAFOND = resources.getString(R.string.voice_balise_nuages_plafond);
    TEXTE_NUAGES_CB_CU = resources.getString(R.string.voice_balise_nuages_cb_cu);
    TEXTE_PRESSION = resources.getString(R.string.voice_balise_pression);
    TEXTE_PRESSION_ND = resources.getString(R.string.voice_balise_pression_nd);
    TEXTE_LUMINOSITE = resources.getString(R.string.voice_balise_lum);
    TEXTE_LUMINOSITE_ND = resources.getString(R.string.voice_balise_lum_nd);
    TEXTE_HUMIDITE = resources.getString(R.string.voice_balise_hum);
    TEXTE_HUMIDITE_ND = resources.getString(R.string.voice_balise_hum_nd);

    // Labels unites
    UNITE_VITESSE = resources.getString(R.string.voice_balise_unit_vit);
    UNITE_TEMPERATURE = resources.getString(R.string.voice_balise_unit_temp);
    UNITE_ALTITUDE = resources.getString(R.string.voice_balise_unit_alti);
    UNITE_HYDROMETRIE = resources.getString(R.string.voice_balise_unit_hydro);
    UNITE_PRESSION = resources.getString(R.string.voice_balise_unit_pression);
    UNITE_HUMIDITE = resources.getString(R.string.voice_balise_unit_hum);

    LABELS_POINTS_CARDINAUX = resources.getStringArray(R.array.voice_balise_points_cardinaux);

    // Pour la transformation de la direction en texte
    DELTA_POINTS_CARDINAUX = 360f / LABELS_POINTS_CARDINAUX.length;
    DECALAGE_POINTS_CARDINAUX = DELTA_POINTS_CARDINAUX / 2;
  }

  /**
   * 
   */
  void refreshSDCardAvailability()
  {
    // SD dispo ?
    sdAvailable = SDCARD_PATH.exists() && SDCARD_PATH.canRead();

    if (sdAvailable)
    {
      // La carte est (devenue) dispo => tentative si client au moins un client enregistre
      synchronized (clients)
      {
        if (clients.size() > 0)
        {
          initializeSpeaker();
        }
      }
    }
    else
    {
      // Sinon fermeture
      shutdownSpeaker();
    }
  }

  /**
   * 
   */
  private void registerSDCardListener()
  {
    final IntentFilter filter = new IntentFilter();
    filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
    filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
    filter.addDataScheme(INTENT_FILE_DATA_SCHEME);
    context.registerReceiver(sdcardReceiver, filter);
  }

  /**
   * 
   */
  private void unregisterSDCardListener()
  {
    context.unregisterReceiver(sdcardReceiver);
    sdcardReceiver.onShutdown();
  }

  /**
   * 
   */
  private void initializeSpeaker()
  {
    synchronized (speakerLock)
    {
      // Fin si besoin
      shutdownSpeaker();

      // Initialisation
      speaker = new TextToSpeech(context, this);
    }
  }

  @Override
  public void onInit(final int code)
  {
    // Verification de la disponibilite
    synchronized (speakerLock)
    {
      if (speaker != null)
      {
        // Initialisations
        final int localeAvailable = speaker.isLanguageAvailable(Locale.getDefault());
        available = (localeAvailable >= 0);

        // Parametres
        speakerParams.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));

        // Completion Listener
        speaker.setOnUtteranceCompletedListener(utteranceCompletedListener);
      }
    }
  }

  /**
   * 
   */
  public void shutdown()
  {
    unregisterSDCardListener();
    unregisterVoiceClients();
    shutdownSpeaker();
    context = null;
  }

  /**
   * 
   */
  private void shutdownSpeaker()
  {
    synchronized (speakerLock)
    {
      if (speaker != null)
      {
        speaker.shutdown();
        speaker = null;
      }
      available = false;
    }
  }

  /**
   * 
   * @return
   */
  public boolean canBeAvailable()
  {
    if (installAsked)
    {
      // Tentative d'initialisation
      synchronized (clients)
      {
        if (clients.size() > 0)
        {
          initializeSpeaker();
        }
      }
      installAsked = false;
    }

    return true;
  }

  /**
   * 
   * @param direction
   * @return
   */
  private static int getIndicePointsCardinaux(final int direction)
  {
    return (int)Math.floor(((direction + DECALAGE_POINTS_CARDINAUX) % 360) / DELTA_POINTS_CARDINAUX);
  }

  /**
   * 
   * @param tend
   * @return
   */
  private String getLabelTendance(final TendanceVent tend)
  {
    switch (tend)
    {
      case FAIBLE_BAISSE:
        return resources.getString(R.string.voice_balise_tend_faible_baisse);
      case FAIBLE_HAUSSE:
        return resources.getString(R.string.voice_balise_tend_faible_hausse);
      case FORTE_BAISSE:
        return resources.getString(R.string.voice_balise_tend_forte_baisse);
      case FORTE_HAUSSE:
        return resources.getString(R.string.voice_balise_tend_forte_hausse);
      case STABLE:
        return resources.getString(R.string.voice_balise_tend_stable);
      case INCONNUE:
      default:
        return Strings.VIDE;
    }
  }

  /**
   * 
   * @param balise
   * @param releve
   * @param inBalisePrefix
   * @return
   */
  public String getTexteBaliseReleve(final Balise balise, final Releve releve, final String inBalisePrefix)
  {
    // Initialisations
    final boolean releveValide = (Utils.getBooleanValue(balise.active) && (releve != null));
    final List<String> texts = new ArrayList<String>();

    // Formats date/heure
    final DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(context);
    final DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(context);

    // Nom de la balise
    final String balisePrefix = (Utils.isStringVide(inBalisePrefix) ? TEXTE_NOM_BALISE : inBalisePrefix);
    texts.add(MessageFormat.format(balisePrefix, getNom(balise.nom)));

    // Altitude
    if (SPEAK_ALTITUDE && (balise.altitude != Integer.MIN_VALUE))
    {
      texts.add(MessageFormat.format(TEXTE_ALTITUDE, Integer.toString(balise.altitude, 10), UNITE_ALTITUDE));
    }

    // Statut
    if (SPEAK_STATUT)
    {
      if (Utils.isBooleanNull(balise.active) || !Utils.getBooleanValue(balise.active))
      {
        texts.add(TEXTE_STATUT_INACTIVE);
      }
      else if (releve == null)
      {
        texts.add(TEXTE_STATUT_PAS_DE_RELEVE);
      }
    }

    if ((releve != null) && releveValide)
    {
      if (SPEAK_HEURE && (releve.date != null))
      {
        // La date si differente de la date courante
        String speakHeure = Strings.VIDE;
        final String dateReleve = dateFormat.format(new Date(Utils.fromUTC(releve.date.getTime())));
        if (!dateReleve.equals(dateFormat.format(new Date())))
        {
          speakHeure = dateReleve + STRING_VIRGULE_SPACE;
        }

        // Les heures
        final String speakHeureHeures = timeFormat.format(new Date(Utils.fromUTC(releve.date.getTime())));
        // Pour palier au bug de "0:xx", le zero n'est pas dit !
        if (speakHeureHeures.startsWith(STRING_ZERO_HEURE_PREFIX))
        {
          speakHeure += STRING_ZERO_ESPACE;
        }
        speakHeure += speakHeureHeures;

        // Au final
        texts.add(speakHeure);
      }

      // Direction
      if (SPEAK_DIRECTION_MOYENNE)
      {
        if (releve.directionMoyenne != Integer.MIN_VALUE)
        {
          texts.add(MessageFormat.format(TEXTE_DIRECTION_MOYENNE, LABELS_POINTS_CARDINAUX[getIndicePointsCardinaux(releve.directionMoyenne)]));
        }
        else
        {
          texts.add(MessageFormat.format(TEXTE_DIRECTION_MOYENNE_ND, TEXTE_NON_DISPONIBLE));
        }
      }

      // Direction instantanee
      if (SPEAK_DIRECTION_INSTANTANEE)
      {
        if (releve.directionInstantanee != Integer.MIN_VALUE)
        {
          texts.add(MessageFormat.format(TEXTE_DIRECTION_INSTANTANEE, LABELS_POINTS_CARDINAUX[getIndicePointsCardinaux(releve.directionInstantanee)]));
        }
        else
        {
          texts.add(MessageFormat.format(TEXTE_DIRECTION_INSTANTANEE_ND, TEXTE_NON_DISPONIBLE));
        }
      }

      // Variation direction
      if (SPEAK_DIRECTION_VARIATION)
      {
        if ((releve.directionVentVariation1 != Integer.MIN_VALUE) && (releve.directionVentVariation2 != Integer.MIN_VALUE))
        {
          texts.add(MessageFormat.format(TEXTE_DIRECTION_VARIATION, LABELS_POINTS_CARDINAUX[getIndicePointsCardinaux(releve.directionVentVariation1)], LABELS_POINTS_CARDINAUX[getIndicePointsCardinaux(releve.directionVentVariation2)]));
        }
        else
        {
          texts.add(MessageFormat.format(TEXTE_DIRECTION_VARIATION_ND, TEXTE_NON_DISPONIBLE));
        }
      }

      // Vitesse mini
      if (SPEAK_VITESSE_MINI)
      {
        if (!Double.isNaN(releve.ventMini))
        {
          final TendanceVent tendMin = BaliseProviderUtils.getTendanceVent(deltaPeremptionMs, releve.date, releve.dateRelevePrecedent, releve.ventMini, releve.ventMiniTendance);
          if ((tendMin != null) && (TendanceVent.INCONNUE != tendMin))
          {
            texts.add(MessageFormat.format(TEXTE_VITESSE_MINI_TEND, Double.valueOf(releve.ventMini), UNITE_VITESSE, getLabelTendance(tendMin)));
          }
          else
          {
            texts.add(MessageFormat.format(TEXTE_VITESSE_MINI, Double.valueOf(releve.ventMini), UNITE_VITESSE));
          }
        }
        else
        {
          texts.add(MessageFormat.format(TEXTE_VITESSE_MINI_ND, TEXTE_NON_DISPONIBLE));
        }
      }

      // Vitesse moyenne
      if (SPEAK_VITESSE_MOYENNE)
      {
        if (!Double.isNaN(releve.ventMoyen))
        {
          final TendanceVent tendMoy = BaliseProviderUtils.getTendanceVent(deltaPeremptionMs, releve.date, releve.dateRelevePrecedent, releve.ventMoyen, releve.ventMoyenTendance);
          if ((tendMoy != null) && (TendanceVent.INCONNUE != tendMoy))
          {
            texts.add(MessageFormat.format(TEXTE_VITESSE_MOYENNE_TEND, Double.valueOf(releve.ventMoyen), UNITE_VITESSE, getLabelTendance(tendMoy)));
          }
          else
          {
            texts.add(MessageFormat.format(TEXTE_VITESSE_MOYENNE, Double.valueOf(releve.ventMoyen), UNITE_VITESSE));
          }
        }
        else
        {
          texts.add(MessageFormat.format(TEXTE_VITESSE_MOYENNE_ND, TEXTE_NON_DISPONIBLE));
        }
      }

      // Vent maxi
      if (SPEAK_VITESSE_MAXI)
      {
        if (!Double.isNaN(releve.ventMaxi))
        {
          String heureMax = Strings.VIDE;
          if (releve.dateHeureVentMaxi != null)
          {
            heureMax = Strings.SPACE + MessageFormat.format(TEXTE_HEURE_VITESSE_MAXI, timeFormat.format(new Date(Utils.fromUTC(releve.dateHeureVentMaxi.getTime()))));
          }
          final TendanceVent tendMax = BaliseProviderUtils.getTendanceVent(deltaPeremptionMs, releve.date, releve.dateRelevePrecedent, releve.ventMaxi, releve.ventMaxiTendance);
          if ((tendMax != null) && (TendanceVent.INCONNUE != tendMax))
          {
            texts.add(MessageFormat.format(TEXTE_VITESSE_MAXI_TEND, Double.valueOf(releve.ventMaxi), UNITE_VITESSE, heureMax, getLabelTendance(tendMax)));
          }
          else
          {
            texts.add(MessageFormat.format(TEXTE_VITESSE_MAXI, Double.valueOf(releve.ventMaxi), UNITE_VITESSE, heureMax));
          }
        }
        else
        {
          texts.add(MessageFormat.format(TEXTE_VITESSE_MAXI_ND, TEXTE_NON_DISPONIBLE));
        }
      }

      // Temperature
      if (SPEAK_TEMPERATURE)
      {
        if (!Double.isNaN(releve.temperature))
        {
          texts.add(MessageFormat.format(TEXTE_TEMPERATURE, Double.valueOf(releve.temperature), UNITE_TEMPERATURE));
        }
        else
        {
          texts.add(MessageFormat.format(TEXTE_TEMPERATURE_ND, TEXTE_NON_DISPONIBLE));
        }
      }

      // Point de rosee
      if (SPEAK_POINT_ROSEE)
      {
        if (!Double.isNaN(releve.pointRosee))
        {
          texts.add(MessageFormat.format(TEXTE_POINT_ROSEE, Double.valueOf(releve.pointRosee), UNITE_TEMPERATURE));
        }
        else
        {
          texts.add(MessageFormat.format(TEXTE_POINT_ROSEE_ND, TEXTE_NON_DISPONIBLE));
        }
      }

      // Hydrometrie
      if (SPEAK_HYDROMETRIE)
      {
        if ((!Double.isNaN(releve.hydrometrie) && (releve.hydrometrie > 0)) || ((releve.pluie != Integer.MIN_VALUE) && (releve.pluie > 0)))
        {
          // Pluie
          final String textePluie;
          if ((releve.pluie != Integer.MIN_VALUE) && (releve.pluie > 0))
          {
            textePluie = TEXTE_PLUIES[releve.pluie - 1];
          }
          else
          {
            textePluie = Strings.VIDE;
          }

          // Hydrometrie
          final String texteHydrometrie;
          if (!Double.isNaN(releve.hydrometrie) && (releve.hydrometrie > 0))
          {
            texteHydrometrie = MessageFormat.format(TEXTE_HYDROMETRIE, Double.valueOf(releve.hydrometrie), UNITE_HYDROMETRIE);
          }
          else
          {
            texteHydrometrie = Strings.VIDE;
          }

          // Fin
          texts.add(MessageFormat.format(TEXTE_PLUIE_HYDROMETRIE, textePluie, texteHydrometrie));
        }
        else
        {
          texts.add(MessageFormat.format(TEXTE_PLUIE_HYDROMETRIE_ND, TEXTE_NON_DISPONIBLE));
        }
      }

      // Nuages
      if (SPEAK_NUAGES)
      {
        if ((releve.nuages != Integer.MIN_VALUE) && (releve.nuages > 0))
        {
          // Plafond
          final String textePlafond;
          if (releve.plafondNuages != Integer.MIN_VALUE)
          {
            final int altitudeBalise = (balise.altitude == Integer.MIN_VALUE ? 0 : balise.altitude);
            textePlafond = MessageFormat.format(TEXTE_NUAGES_PLAFOND, Integer.valueOf(releve.plafondNuages + altitudeBalise).toString(), UNITE_ALTITUDE);
          }
          else
          {
            textePlafond = Strings.VIDE;
          }

          // Cb/Cu
          final String texteCbCu;
          if (!Utils.isBooleanNull(releve.nuagesBourgeonnants) && Utils.getBooleanValue(releve.nuagesBourgeonnants))
          {
            texteCbCu = TEXTE_NUAGES_CB_CU;
          }
          else
          {
            texteCbCu = Strings.VIDE;
          }

          // Fin
          texts.add(MessageFormat.format(TEXTE_NUAGES, Integer.valueOf(releve.nuages), textePlafond, texteCbCu));
        }
        else
        {
          texts.add(MessageFormat.format(TEXTE_NUAGES_ND, TEXTE_NON_DISPONIBLE));
        }
      }

      // Pression
      if (SPEAK_PRESSION)
      {
        if (!Double.isNaN(releve.pression))
        {
          texts.add(MessageFormat.format(TEXTE_PRESSION, Double.valueOf(releve.pression), UNITE_PRESSION));
        }
        else
        {
          texts.add(MessageFormat.format(TEXTE_PRESSION_ND, TEXTE_NON_DISPONIBLE));
        }
      }

      // Luminosite
      if (SPEAK_LUMINOSITE)
      {
        if (releve.luminosite != null)
        {
          texts.add(MessageFormat.format(TEXTE_LUMINOSITE, releve.luminosite));
        }
        else
        {
          texts.add(MessageFormat.format(TEXTE_LUMINOSITE_ND, TEXTE_NON_DISPONIBLE));
        }
      }

      // Humidite
      if (SPEAK_HUMIDITE)
      {
        if (releve.humidite != Integer.MIN_VALUE)
        {
          texts.add(MessageFormat.format(TEXTE_HUMIDITE, Integer.valueOf(releve.humidite), UNITE_HUMIDITE));
        }
        else
        {
          texts.add(MessageFormat.format(TEXTE_HUMIDITE_ND, TEXTE_NON_DISPONIBLE));
        }
      }
    }

    // Voix !
    final StringBuilder speakBuffer = new StringBuilder();
    boolean first = true;
    for (final String text : texts)
    {
      if (!first)
      {
        speakBuffer.append(Strings.CHAR_POINT);
        speakBuffer.append(Strings.CHAR_SPACE);
      }
      speakBuffer.append(text);
      first = false;
    }

    return speakBuffer.toString();
  }

  /**
   * 
   * @param text
   * @param flush
   * @param activity
   * @return
   */
  public boolean speak(final String text, final boolean flush, final Activity activity)
  {
    // Verification prealable
    if (!checkForSpeak(activity))
    {
      return false;
    }

    // Debug
    Log.d(getClass().getSimpleName(), "Text to speak : " + text);

    // Voix !
    synchronized (speakerLock)
    {
      if (speaker != null)
      {
        // Meme phrase deja en attente ?
        if (utterances.contains(text))
        {
          Log.d(getClass().getSimpleName(), "... '" + text + "' already speaked, skipping !");
        }
        else
        {
          // Vidage du buffer si besoin
          if (flush)
          {
            stopSpeaking(true);
          }

          // Reclame le silence
          gainAudioFocus();

          // Parle !
          utterances.add(text);
          speakerParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, text);
          speaker.speak(text, TextToSpeech.QUEUE_ADD, speakerParams);
        }
      }
    }

    return true;
  }

  /**
   * 
   * @param balise
   * @param releve
   * @param flush
   * @param activity
   * @return
   */
  public boolean speakBaliseReleve(final Balise balise, final Releve releve, final boolean flush, final Activity activity)
  {
    // Verification prealable
    if (!checkForSpeak(activity))
    {
      return false;
    }

    // Initialisations
    if (balise == null)
    {
      return false;
    }

    // Le texte
    final String text = getTexteBaliseReleve(balise, releve, null);

    // Voix !
    return speak(text, flush, activity);
  }

  /**
   * 
   */
  @TargetApi(Build.VERSION_CODES.FROYO)
  private void gainAudioFocus()
  {
    if (ActivityCommons.isFroyo())
    {
      final AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
      audioManager.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
    }
  }

  /**
   * 
   */
  @TargetApi(Build.VERSION_CODES.FROYO)
  void abandonAudioFocus()
  {
    if (ActivityCommons.isFroyo())
    {
      final AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
      audioManager.abandonAudioFocus(audioFocusListener);
    }
  }

  /**
   * 
   * @param activity
   * @return
   */
  private boolean checkForSpeak(final Activity activity)
  {
    // Carte SD dispo ?
    if (!sdAvailable)
    {
      if (activity != null)
      {
        final String title = resources.getString(R.string.label_voice_sdcard_title);
        final String message = resources.getString(R.string.label_voice_sdcard_message);
        ActivityCommons.alertDialog(activity, ActivityCommons.ALERT_DIALOG_VOICE_SDCARD_UNAVAILABLE, -1, title, message, null, true, null, 0);
      }

      return false;
    }

    // Dispo ?
    if (!available)
    {
      if (activity != null)
      {
        // Initialisation
        installAsked = false;

        // Message de demande d'installation
        final String title = resources.getString(R.string.label_voice_install_title);
        final String message = resources.getString(R.string.label_voice_install_message, resources.getString(R.string.button_ok));
        ActivityCommons.confirmDialog(activity, ActivityCommons.CONFIRM_DIALOG_VOICE_INSTALL, title, message, new OnClickListener()
        {
          @Override
          public void onClick(final DialogInterface dialog, final int id)
          {
            // Flag
            installAsked = true;

            // Lancement
            startDownload(activity);
          }
        }, null, true, null, -1, -1, null, null, null);
      }

      // Fin
      return false;
    }

    return true;
  }

  /**
   * 
   * @param activity
   */
  static void startDownload(final Activity activity)
  {
    // Lancement
    final Intent installIntent = new Intent();
    installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
    activity.startActivity(installIntent);
  }

  /**
   * 
   * @param force
   * @return
   */
  public boolean stopSpeaking(final boolean force)
  {
    // Dispo ?
    if (!available)
    {
      return false;
    }

    synchronized (speakerLock)
    {
      if (speaker != null)
      {
        final boolean isSpeaking = speaker.isSpeaking();
        if (force || isSpeaking)
        {
          speaker.stop();
          utterances.clear();
          abandonAudioFocus();
          return true;
        }
      }
    }

    return false;
  }

  /**
   * 
   * @param nom
   * @return
   */
  private static String getNom(final String nom)
  {
    // Caractere '/'
    String retour = nom.replace(Strings.CHAR_SLASH, Strings.CHAR_SPACE);

    // "Saint"
    retour = retour.replaceAll(REGEXP_SAINT_ST_POINT, REMPLACEMENT_SAINT);
    retour = retour.replaceAll(REGEXP_SAINT_ST_ESPACE, REMPLACEMENT_SAINT);

    // Caractere '-'
    retour = retour.replace(Strings.CHAR_MOINS, Strings.CHAR_SPACE);

    return retour;
  }

  /**
   * 
   * @param sharedPreferences
   * @return
   */
  public boolean updatePreferences(final SharedPreferences sharedPreferences)
  {
    // Initialisations
    boolean preferencesChanged = false;

    // Limite de peremption releve
    try
    {
      final int value = sharedPreferences.getInt(resources.getString(R.string.config_map_outofdate_key), Integer.parseInt(resources.getString(R.string.config_map_outofdate_default), 10));
      if (value != deltaPeremption)
      {
        deltaPeremption = value;
        deltaPeremptionMs = 60000L * deltaPeremption;
        preferencesChanged = true;
      }
    }
    catch (final NumberFormatException nfe)
    {
      Log.e(BaliseDrawable.class.getSimpleName(), nfe.getMessage(), nfe);
    }

    // Altitude
    final boolean speakAltitude = sharedPreferences.getBoolean(resources.getString(R.string.config_map_speak_altitude_key), Boolean.parseBoolean(resources.getString(R.string.config_map_speak_altitude_default)));
    if (speakAltitude != SPEAK_ALTITUDE)
    {
      SPEAK_ALTITUDE = speakAltitude;
      preferencesChanged = true;
    }

    // Date et heure
    final boolean speakHeure = sharedPreferences.getBoolean(resources.getString(R.string.config_map_speak_date_key), Boolean.parseBoolean(resources.getString(R.string.config_map_speak_date_default)));
    if (speakHeure != SPEAK_HEURE)
    {
      SPEAK_HEURE = speakHeure;
      preferencesChanged = true;
    }

    // Direction moyenne
    final boolean speakDirectionMoyenne = sharedPreferences.getBoolean(resources.getString(R.string.config_map_speak_averagedir_key), Boolean.parseBoolean(resources.getString(R.string.config_map_speak_averagedir_default)));
    if (speakDirectionMoyenne != SPEAK_DIRECTION_MOYENNE)
    {
      SPEAK_DIRECTION_MOYENNE = speakDirectionMoyenne;
      preferencesChanged = true;
    }

    // Direction instantanee
    final boolean speakDirectionInstantanee = sharedPreferences.getBoolean(resources.getString(R.string.config_map_speak_instantdir_key), Boolean.parseBoolean(resources.getString(R.string.config_map_speak_instantdir_default)));
    if (speakDirectionInstantanee != SPEAK_DIRECTION_INSTANTANEE)
    {
      SPEAK_DIRECTION_INSTANTANEE = speakDirectionInstantanee;
      preferencesChanged = true;
    }

    // Variation direction
    final boolean speakVariationDirection = sharedPreferences.getBoolean(resources.getString(R.string.config_map_speak_variationdir_key), Boolean.parseBoolean(resources.getString(R.string.config_map_speak_variationdir_default)));
    if (speakVariationDirection != SPEAK_DIRECTION_VARIATION)
    {
      SPEAK_DIRECTION_VARIATION = speakVariationDirection;
      preferencesChanged = true;
    }

    // Vent moyen
    final boolean speakVentMoyen = sharedPreferences.getBoolean(resources.getString(R.string.config_map_speak_averagewind_key), Boolean.parseBoolean(resources.getString(R.string.config_map_speak_averagewind_default)));
    if (speakVentMoyen != SPEAK_VITESSE_MOYENNE)
    {
      SPEAK_VITESSE_MOYENNE = speakVentMoyen;
      preferencesChanged = true;
    }

    // Vent mini
    final boolean speakVentMini = sharedPreferences.getBoolean(resources.getString(R.string.config_map_speak_minwind_key), Boolean.parseBoolean(resources.getString(R.string.config_map_speak_minwind_default)));
    if (speakVentMini != SPEAK_VITESSE_MINI)
    {
      SPEAK_VITESSE_MINI = speakVentMini;
      preferencesChanged = true;
    }

    // Vent maxi
    final boolean speakVentMaxi = sharedPreferences.getBoolean(resources.getString(R.string.config_map_speak_maxwind_key), Boolean.parseBoolean(resources.getString(R.string.config_map_speak_maxwind_default)));
    if (speakVentMaxi != SPEAK_VITESSE_MAXI)
    {
      SPEAK_VITESSE_MAXI = speakVentMaxi;
      preferencesChanged = true;
    }

    // Temperature
    final boolean speakTemperature = sharedPreferences.getBoolean(resources.getString(R.string.config_map_speak_temperature_key), Boolean.parseBoolean(resources.getString(R.string.config_map_speak_temperature_default)));
    if (speakTemperature != SPEAK_TEMPERATURE)
    {
      SPEAK_TEMPERATURE = speakTemperature;
      preferencesChanged = true;
    }

    // Point de rosee
    final boolean speakPointRosee = sharedPreferences.getBoolean(resources.getString(R.string.config_map_speak_dew_point_key), Boolean.parseBoolean(resources.getString(R.string.config_map_speak_dew_point_default)));
    if (speakPointRosee != SPEAK_POINT_ROSEE)
    {
      SPEAK_POINT_ROSEE = speakPointRosee;
      preferencesChanged = true;
    }

    // Hydrometrie
    final boolean speakHydrometrie = sharedPreferences.getBoolean(resources.getString(R.string.config_map_speak_hydrometry_key), Boolean.parseBoolean(resources.getString(R.string.config_map_speak_hydrometry_default)));
    if (speakHydrometrie != SPEAK_HYDROMETRIE)
    {
      SPEAK_HYDROMETRIE = speakHydrometrie;
      preferencesChanged = true;
    }

    // Nuages
    final boolean speakNuages = sharedPreferences.getBoolean(resources.getString(R.string.config_map_speak_clouds_key), Boolean.parseBoolean(resources.getString(R.string.config_map_speak_clouds_default)));
    if (speakNuages != SPEAK_NUAGES)
    {
      SPEAK_NUAGES = speakNuages;
      preferencesChanged = true;
    }

    // Pression
    final boolean speakPression = sharedPreferences.getBoolean(resources.getString(R.string.config_map_speak_pressure_key), Boolean.parseBoolean(resources.getString(R.string.config_map_speak_pressure_default)));
    if (speakPression != SPEAK_PRESSION)
    {
      SPEAK_PRESSION = speakPression;
      preferencesChanged = true;
    }

    // Luminosite
    final boolean speakLuminosite = sharedPreferences.getBoolean(resources.getString(R.string.config_map_speak_luminosity_key), Boolean.parseBoolean(resources.getString(R.string.config_map_speak_luminosity_default)));
    if (speakLuminosite != SPEAK_LUMINOSITE)
    {
      SPEAK_LUMINOSITE = speakLuminosite;
      preferencesChanged = true;
    }

    // Humidite
    final boolean speakHumidite = sharedPreferences.getBoolean(resources.getString(R.string.config_map_speak_humidity_key), Boolean.parseBoolean(resources.getString(R.string.config_map_speak_humidity_default)));
    if (speakHumidite != SPEAK_HUMIDITE)
    {
      SPEAK_HUMIDITE = speakHumidite;
      preferencesChanged = true;
    }

    // Fin
    return preferencesChanged;
  }

  /**
   * 
   * @param clientId
   */
  public void registerVoiceClient(final int clientId)
  {
    synchronized (clients)
    {
      // Initialisations
      final Integer clientIntegerId = Integer.valueOf(clientId);

      // Ajout du client si non deja enregistre
      if (!clients.contains(clientIntegerId))
      {
        Log.d(getClass().getSimpleName(), "registering voice client #" + clientId);
        clients.add(clientIntegerId);

        // Si c'est le premier client, initialisation du moteur
        if (clients.size() == 1)
        {
          Log.d(getClass().getSimpleName(), "first client, initializing TTS");
          initializeSpeaker();
        }
      }
    }
  }

  /**
   * 
   * @param clientId
   */
  public void unregisterVoiceClient(final int clientId)
  {
    synchronized (clients)
    {
      // Initialisations
      final Integer clientIntegerId = Integer.valueOf(clientId);

      // Ajout du client si non deja enregistre
      if (clients.contains(clientIntegerId))
      {
        Log.d(getClass().getSimpleName(), "unregistering voice client #" + clientId);
        clients.remove(clientIntegerId);

        // Si c'est le dernier client, fermeture du moteur
        if (clients.size() == 0)
        {
          Log.d(getClass().getSimpleName(), "no more client, shutting down TTS...");
          shutdownSpeaker();
        }
      }
    }
  }

  /**
   * 
   */
  private void unregisterVoiceClients()
  {
    synchronized (clients)
    {
      Log.d(getClass().getSimpleName(), "unregistering all voice clients : " + clients);
      clients.clear();
    }
  }
}
