package com.pedro.android.mobibalises.alarm;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.IProvidersServiceActivity;
import org.pedro.android.mobibalises_common.IProvidersServiceFragment;
import org.pedro.android.mobibalises_common.SingleChoiceDialogFragment;
import org.pedro.android.mobibalises_common.service.IProvidersService;
import org.pedro.android.mobibalises_lgpl.alarm.BaliseAlarm;
import org.pedro.android.mobibalises_lgpl.alarm.BaliseAlarm.Activation;
import org.pedro.android.mobibalises_lgpl.alarm.BaliseAlarm.Notification;
import org.pedro.android.mobibalises_lgpl.alarm.BaliseAlarm.PlageHoraire;
import org.pedro.android.mobibalises_lgpl.alarm.BaliseAlarm.PlageVitesse;
import org.pedro.android.widget.RangeSeekBar;
import org.pedro.android.widget.RangeSeekBar.OnRangeSeekBarChangeListener;
import org.pedro.android.widget.SectorSelectorView;
import org.pedro.android.widget.SectorSelectorView.OnSectorCheckedListener;
import org.pedro.balises.Balise;
import org.pedro.balises.BaliseProvider;
import org.pedro.balises.Releve;
import org.pedro.misc.Sector;

import android.app.Activity;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Resources;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.pedro.android.mobibalises.R;
import com.pedro.android.mobibalises.service.IFullProvidersService;

/**
 * 
 * @author pedro.m
 */
public class AlarmFragment extends Fragment implements IProvidersServiceFragment, android.view.View.OnClickListener
{
  protected static final int  REQUEST_NOTIFICATION_ANDROID_SELECT_AUDIO = 1;
  private static final String STRING_VITESSE_MAXI_INFINIE               = "...";

  private static enum TypePlageVitesse
  {
    MINI, MOY, MAXI
  }

  private static final String   ARG_JSON          = "json";
  private static final String   ARG_POSITION      = "position";

  static final NumberFormat     VITESSES_FORMAT   = new DecimalFormat("#");

  public static final String    INTENT_ALARM_JSON = "ALARM_JON";

  static final double[]         VITESSES          = { 0, 5, 10, 15, 20, 25, 30, 40, 50, 75, 100, 150, 200 };

  final BaliseAlarm             alarm             = new BaliseAlarm();

  private ActivationAdapter     activationAdapter;

  private PlagesHorairesAdapter plagesHorairesAdapter;

  private PlagesVitessesAdapter plagesVitessesMiniAdapter;
  private PlagesVitessesAdapter plagesVitessesMoyAdapter;
  private PlagesVitessesAdapter plagesVitessesMaxiAdapter;

  private static final Field    FIELD_CHECK_NOTIFICATION_ANDROID_PERSO;
  private static final Field    FIELD_TEXTE_NOTIFICATION_ANDROID_PERSO_VERIFIEE;
  private static final Field    FIELD_TEXTE_NOTIFICATION_ANDROID_PERSO_NON_VERIFIEE;
  private static final Field    FIELD_CHECK_NOTIFICATION_VOIX_PERSO;
  private static final Field    FIELD_TEXTE_NOTIFICATION_VOIX_PERSO_VERIFIEE;
  private static final Field    FIELD_TEXTE_NOTIFICATION_VOIX_PERSO_NON_VERIFIEE;

  /**
   * 
   */
  static
  {
    Field fieldCheckNotificationAndroidPerso = null;
    Field fieldTexteNotificationAndroidPersoVerifiee = null;
    Field fieldTexteNotificationAndroidPersoNonVerifiee = null;
    Field fieldCheckNotificationVoixPerso = null;
    Field fieldTexteNotificationVoixPersoVerifiee = null;
    Field fieldTexteNotificationVoixPersoNonVerifiee = null;
    try
    {
      fieldCheckNotificationAndroidPerso = BaliseAlarm.class.getField("checkNotificationAndroidPerso");
      fieldTexteNotificationAndroidPersoVerifiee = BaliseAlarm.class.getField("texteVerifieeNotificationAndroidPerso");
      fieldTexteNotificationAndroidPersoNonVerifiee = BaliseAlarm.class.getField("texteNonVerifieeNotificationAndroidPerso");
      fieldCheckNotificationVoixPerso = BaliseAlarm.class.getField("checkNotificationVoixPerso");
      fieldTexteNotificationVoixPersoVerifiee = BaliseAlarm.class.getField("texteVerifieeNotificationVoixPerso");
      fieldTexteNotificationVoixPersoNonVerifiee = BaliseAlarm.class.getField("texteNonVerifieeNotificationVoixPerso");
    }
    catch (final NoSuchFieldException nsfe)
    {
      Log.e(AlarmFragment.class.getSimpleName(), nsfe.getMessage(), nsfe);
    }
    finally
    {
      FIELD_CHECK_NOTIFICATION_ANDROID_PERSO = fieldCheckNotificationAndroidPerso;
      FIELD_TEXTE_NOTIFICATION_ANDROID_PERSO_VERIFIEE = fieldTexteNotificationAndroidPersoVerifiee;
      FIELD_TEXTE_NOTIFICATION_ANDROID_PERSO_NON_VERIFIEE = fieldTexteNotificationAndroidPersoNonVerifiee;
      FIELD_CHECK_NOTIFICATION_VOIX_PERSO = fieldCheckNotificationVoixPerso;
      FIELD_TEXTE_NOTIFICATION_VOIX_PERSO_VERIFIEE = fieldTexteNotificationVoixPersoVerifiee;
      FIELD_TEXTE_NOTIFICATION_VOIX_PERSO_NON_VERIFIEE = fieldTexteNotificationVoixPersoNonVerifiee;
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static final class ActivationAdapter extends ArrayAdapter<String>
  {
    /**
     * 
     * @param context
     * @param viewGroup
     */
    ActivationAdapter(final Context context)
    {
      super(context, android.R.layout.simple_spinner_dropdown_item);
      final Resources resources = context.getResources();
      final int nbActivations = Activation.values().length;
      for (int i = 0; i < nbActivations; i++)
      {
        add(resources.getStringArray(R.array.alarm_label_activation)[i]);
      }
    }

    /**
     * 
     * @param inActivation
     * @return
     */
    static int getPosition(final Activation inActivation)
    {
      int i = 0;
      for (final Activation activation : Activation.values())
      {
        if (activation.equals(inActivation))
        {
          return i;
        }

        // Next
        i++;
      }

      return -1;
    }
  }

  /**
   * 
   * @author pedro.m
   * 
   * @param <T>
   */
  private static abstract class ViewGroupAdapter<T> extends ArrayAdapter<T>
  {
    protected final LayoutInflater inflater;
    private final ViewGroup        viewGroup;

    /**
     * 
     * @param context
     * @param viewGroup
     */
    protected ViewGroupAdapter(final Context context, final ViewGroup viewGroup)
    {
      super(context, android.R.layout.simple_list_item_1);
      inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      this.viewGroup = viewGroup;
    }

    /**
     * 
     * @param data
     */
    public void setData(final List<T> data)
    {
      // Vidage
      clear();
      viewGroup.removeAllViews();

      // Remplissage
      if (data != null)
      {
        // Utilisable seulement a partir de l'API 11 : addAll(data);
        for (final T item : data)
        {
          add(item);
        }
      }
    }

    @Override
    public void add(final T item)
    {
      super.add(item);
      final int position = getPosition(item);
      viewGroup.addView(getView(position, null, null));
    }

    @Override
    public void remove(final T item)
    {
      final int position = getPosition(item);
      viewGroup.removeViewAt(position);
      super.remove(item);
    }
  }

  /**
   * 
   * @author pedro.m
   *
   * @param <T>
   */
  private static abstract class AlarmViewGroupAdapter<T> extends ViewGroupAdapter<T>
  {
    protected final BaliseAlarm                       alarm;
    protected final android.view.View.OnClickListener clickListener;

    /**
     * 
     * @param context
     * @param alarm
     * @param viewGroup
     * @param clickListener
     */
    protected AlarmViewGroupAdapter(final Context context, final BaliseAlarm alarm, final ViewGroup viewGroup, final android.view.View.OnClickListener clickListener)
    {
      super(context, viewGroup);
      this.alarm = alarm;
      this.clickListener = clickListener;
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class PlagesHorairesAdapter extends AlarmViewGroupAdapter<PlageHoraire>
  {
    /**
     * 
     * @param context
     * @param alarm
     * @param viewGroup
     * @param clickListener
     */
    PlagesHorairesAdapter(final Context context, final BaliseAlarm alarm, final ViewGroup viewGroup, final android.view.View.OnClickListener clickListener)
    {
      super(context, alarm, viewGroup, clickListener);
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent)
    {
      // Recyclage eventuel de la vue
      final View view;
      if (convertView == null)
      {
        view = inflater.inflate(R.layout.alarm_plage_horaire, parent, false);
      }
      else
      {
        view = convertView;
      }

      // Item selectionne
      final PlageHoraire item = getItem(position);

      // Heure de debut
      final TextView debutText = (TextView)view.findViewById(R.id.alarm_plage_horaire_debut);
      formatHeure(debutText, item.heuresDebut, item.minutesDebut);
      debutText.setOnClickListener(new android.view.View.OnClickListener()
      {
        @Override
        public void onClick(final View v)
        {
          final DialogFragment pickerFragment = new TimePickerFragment(new OnTimeSetListener()
          {
            @Override
            public void onTimeSet(final TimePicker inView, final int hourOfDay, final int minute)
            {
              item.heuresDebut = hourOfDay;
              item.minutesDebut = minute;
              formatHeure(debutText, hourOfDay, minute);
            }
          }, item.heuresDebut, item.minutesDebut);
          pickerFragment.show(((FragmentActivity)getContext()).getSupportFragmentManager(), "pickerDebut");
        }
      });

      // Heure de fin
      final TextView finText = (TextView)view.findViewById(R.id.alarm_plage_horaire_fin);
      formatHeure(finText, item.heuresFin, item.minutesFin);
      finText.setOnClickListener(new android.view.View.OnClickListener()
      {
        @Override
        public void onClick(final View v)
        {
          final DialogFragment pickerFragment = new TimePickerFragment(new OnTimeSetListener()
          {
            @Override
            public void onTimeSet(final TimePicker inView, final int hourOfDay, final int minute)
            {
              item.heuresFin = hourOfDay;
              item.minutesFin = minute;
              formatHeure(finText, hourOfDay, minute);
            }
          }, item.heuresFin, item.minutesFin);
          pickerFragment.show(((FragmentActivity)getContext()).getSupportFragmentManager(), "pickerFin");
        }
      });

      // Bouton del
      final View delView = view.findViewById(R.id.alarm_plage_horaire_del);
      delView.setOnClickListener(clickListener);

      return view;
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static abstract class PlagesVitessesAdapter extends AlarmViewGroupAdapter<PlageVitesse>
  {
    /**
     * 
     * @param context
     * @param alarm
     * @param viewGroup
     * @param clickListener
     */
    protected PlagesVitessesAdapter(final Context context, final BaliseAlarm alarm, final ViewGroup viewGroup, final android.view.View.OnClickListener clickListener)
    {
      super(context, alarm, viewGroup, clickListener);
    }

    @SuppressWarnings("unchecked")
    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent)
    {
      // Recyclage eventuel de la vue
      final View view;
      final RangeSeekBar<Integer> seekBar;
      if (convertView == null)
      {
        view = inflater.inflate(R.layout.alarm_plage_vitesse, parent, false);
        final ViewGroup container = (ViewGroup)view.findViewById(R.id.alarm_plage_vitesse_container);
        seekBar = new RangeSeekBar<Integer>(Integer.valueOf(0), Integer.valueOf(VITESSES.length), getContext());
        seekBar.setId(R.id.alarm_plage_vitesse_plage);
        seekBar.setMinRangeDifference(Integer.valueOf(1));
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        seekBar.setLayoutParams(layoutParams);
        container.addView(seekBar);
      }
      else
      {
        view = convertView;
        final ViewGroup container = (ViewGroup)view.findViewById(R.id.alarm_plage_vitesse_container);
        seekBar = (RangeSeekBar<Integer>)container.findViewById(R.id.alarm_plage_vitesse_plage);
      }

      // Item selectionne
      final PlageVitesse plage = getItem(position);

      // MAJ de la vue
      final TextView infTextView = (TextView)view.findViewById(R.id.alarm_plage_vitesse_inferieure_value);
      final TextView supTextView = (TextView)view.findViewById(R.id.alarm_plage_vitesse_superieure_value);

      // Curseur
      seekBar.setNotifyWhileDragging(true);
      final OnRangeSeekBarChangeListener<Integer> seekBarListener = new OnRangeSeekBarChangeListener<Integer>()
      {
        @Override
        public void onRangeSeekBarValuesChanged(final RangeSeekBar<?> bar, final Integer minValue, final Integer maxValue)
        {
          // handle changed range values
          final double vitesseMini = getVitesseForProgress(minValue.intValue());
          final double vitesseMaxi = getVitesseForProgress(maxValue.intValue());
          plage.vitesseMini = vitesseMini;
          plage.vitesseMaxi = vitesseMaxi;
          infTextView.setText(getStringVitesseForProgress(minValue.intValue()));
          supTextView.setText(getStringVitesseForProgress(maxValue.intValue()));
        }
      };
      final Integer minValue = Integer.valueOf(getProgressForVitesse(plage.vitesseMini));
      final Integer maxValue = Integer.valueOf(getProgressForVitesse(plage.vitesseMaxi));
      seekBar.setSelectedMinValue(minValue);
      seekBar.setSelectedMaxValue(maxValue);
      seekBar.setOnRangeSeekBarChangeListener(seekBarListener);
      seekBarListener.onRangeSeekBarValuesChanged(seekBar, minValue, maxValue);

      // Bouton del
      final View delView = view.findViewById(R.id.alarm_plage_vitesse_del);
      delView.setTag(R.id.alarm_plage_vitesse_del_type, getTypeTag());
      delView.setOnClickListener(clickListener);

      return view;
    }

    /**
     * 
     * @return
     */
    protected abstract boolean isVitessesChecked();

    /**
     * 
     * @return
     */
    protected abstract TypePlageVitesse getTypeTag();
  }

  /**
   * 
   * @author pedro.m
   */
  private static class PlagesVitessesMiniAdapter extends PlagesVitessesAdapter
  {
    /**
     * 
     * @param context
     * @param alarm
     * @param viewGroup
     * @param clickListener
     */
    PlagesVitessesMiniAdapter(final Context context, final BaliseAlarm alarm, final ViewGroup viewGroup, final android.view.View.OnClickListener clickListener)
    {
      super(context, alarm, viewGroup, clickListener);
    }

    @Override
    protected boolean isVitessesChecked()
    {
      return alarm.checkVitesseMini;
    }

    @Override
    protected TypePlageVitesse getTypeTag()
    {
      return TypePlageVitesse.MINI;
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class PlagesVitessesMoyAdapter extends PlagesVitessesAdapter
  {
    /**
     * 
     * @param context
     * @param alarm
     * @param viewGroup
     * @param clickListener
     */
    PlagesVitessesMoyAdapter(final Context context, final BaliseAlarm alarm, final ViewGroup viewGroup, final android.view.View.OnClickListener clickListener)
    {
      super(context, alarm, viewGroup, clickListener);
    }

    @Override
    protected boolean isVitessesChecked()
    {
      return alarm.checkVitesseMoy;
    }

    @Override
    protected TypePlageVitesse getTypeTag()
    {
      return TypePlageVitesse.MOY;
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class PlagesVitessesMaxiAdapter extends PlagesVitessesAdapter
  {
    /**
     * 
     * @param context
     * @param alarm
     * @param viewGroup
     * @param clickListener
     */
    PlagesVitessesMaxiAdapter(final Context context, final BaliseAlarm alarm, final ViewGroup viewGroup, final android.view.View.OnClickListener clickListener)
    {
      super(context, alarm, viewGroup, clickListener);
    }

    @Override
    protected boolean isVitessesChecked()
    {
      return alarm.checkVitesseMaxi;
    }

    @Override
    protected TypePlageVitesse getTypeTag()
    {
      return TypePlageVitesse.MAXI;
    }
  }

  /**
   * 
   * @param position
   * @param json
   * @return
   */
  public static AlarmFragment newInstance(final int position, final String json)
  {
    final AlarmFragment fragment = new AlarmFragment();

    final Bundle arguments = new Bundle();
    arguments.putInt(ARG_POSITION, position);
    arguments.putString(ARG_JSON, json);
    fragment.setArguments(arguments);

    return fragment;
  }

  /**
   * 
   * @return
   */
  public int getCurrentPosition()
  {
    return getArguments().getInt(ARG_POSITION, -1);
  }

  @Override
  public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState)
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> onCreateView()");

    // Creation de la vue
    final View view = inflater.inflate(R.layout.alarm, null);

    // Listeners
    view.findViewById(R.id.alarm_nom_provider).setOnClickListener(this);
    view.findViewById(R.id.alarm_nom_balise).setOnClickListener(this);
    view.findViewById(R.id.alarm_notification_broadcast_help).setOnClickListener(this);
    view.findViewById(R.id.alarm_plage_vitesse_moy_add).setOnClickListener(this);
    view.findViewById(R.id.alarm_plage_vitesse_maxi_add).setOnClickListener(this);
    view.findViewById(R.id.alarm_plage_vitesse_mini_add).setOnClickListener(this);
    view.findViewById(R.id.alarm_plage_horaire_add).setOnClickListener(this);

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onCreateView() : " + view);
    return view;
  }

  @Override
  public void onCreate(final Bundle savedInstanceState)
  {
    // Super
    Log.d(getClass().getSimpleName(), ">>> onCreate()");
    super.onCreate(savedInstanceState);

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onCreate()");
  }

  @Override
  public void onStart()
  {
    // Super
    Log.d(getClass().getSimpleName(), ">>> onStart()");
    super.onStart();

    // Initialisation vues
    initViews();

    // Synchro
    final String json = getArguments().getString(ARG_JSON);
    synchronize(json);

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onStart()");
  }

  @Override
  public void onStop()
  {
    // Super
    Log.d(getClass().getSimpleName(), ">>> onStop()");
    super.onStop();

    // Sauvegarde de l'alarme
    transmitAlarm();

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onStop()");
  }

  @Override
  public void onDestroy()
  {
    // Super
    Log.d(getClass().getSimpleName(), ">>> onDestroy()");
    super.onDestroy();

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onDestroy()");
  }

  @Override
  public void onActivityResult(final int requestCode, final int resultCode, final Intent intent)
  {
    switch (requestCode)
    {
      case REQUEST_NOTIFICATION_ANDROID_SELECT_AUDIO:
        if (resultCode == Activity.RESULT_OK)
        {
          alarm.uriNotificationAndroidAudio = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
          synchronizeNotificationAndroidAudio();
        }
        break;

      default:
        super.onActivityResult(requestCode, resultCode, intent);
        break;
    }
  }

  /**
   * 
   */
  private void initViews()
  {
    // Activation
    activationAdapter = new ActivationAdapter(getActivity());

    // Plages horaires
    plagesHorairesAdapter = new PlagesHorairesAdapter(getActivity(), alarm, (ViewGroup)getView().findViewById(R.id.alarm_plages_horaires_list), this);

    // Plages vitesses
    plagesVitessesMiniAdapter = new PlagesVitessesMiniAdapter(getActivity(), alarm, (ViewGroup)getView().findViewById(R.id.alarm_plages_vitesse_mini_list), this);
    plagesVitessesMoyAdapter = new PlagesVitessesMoyAdapter(getActivity(), alarm, (ViewGroup)getView().findViewById(R.id.alarm_plages_vitesse_moy_list), this);
    plagesVitessesMaxiAdapter = new PlagesVitessesMaxiAdapter(getActivity(), alarm, (ViewGroup)getView().findViewById(R.id.alarm_plages_vitesse_maxi_list), this);
  }

  /**
   * 
   * @param jsonParam
   */
  private void synchronize(final String jsonParam)
  {
    try
    {
      // Analyse
      Log.d(getClass().getSimpleName(), ">>> synchronize(" + jsonParam + ")");
      final JSONObject json = new JSONObject(jsonParam);
      alarm.fromJSON(json);
      Log.d(getClass().getSimpleName(), "nomProvider : " + alarm.nomProvider);
      Log.d(getClass().getSimpleName(), "nomBalise : " + alarm.nomBalise);

      // Nom du provider
      synchronizeProvider();

      // Nom de la balise
      synchronizeBalise();

      // Activation
      final Spinner activationSpinner = (Spinner)getView().findViewById(R.id.alarm_activation);
      activationSpinner.setAdapter(activationAdapter);
      activationSpinner.setSelection(ActivationAdapter.getPosition(alarm.activation));
      activationSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
      {
        @Override
        public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id)
        {
          alarm.activation = Activation.values()[position];
        }

        @Override
        public void onNothingSelected(final AdapterView<?> parent)
        {
          // Nothing
        }
      });

      // Notification android
      synchronizeAndroidNotificationGroup();

      // Notification voix
      synchronizeVoiceNotificationGroup();

      // Notification broadcast
      final CheckBox broadcastCheckView = (CheckBox)getView().findViewById(R.id.alarm_check_notification_broadcast);
      final boolean broadcastNotificationChecked = alarm.notifications.contains(Notification.BROADCAST);
      broadcastCheckView.setChecked(broadcastNotificationChecked);
      final OnCheckedChangeListener checkNotificationChangeListener = new OnCheckedChangeListener()
      {
        @Override
        public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked)
        {
          // Alarme
          if (isChecked && !alarm.notifications.contains(Notification.BROADCAST))
          {
            alarm.notifications.add(Notification.BROADCAST);
          }
          else if (!isChecked && alarm.notifications.contains(Notification.BROADCAST))
          {
            alarm.notifications.remove(Notification.BROADCAST);
          }
        }
      };
      checkNotificationChangeListener.onCheckedChanged(broadcastCheckView, broadcastNotificationChecked);
      broadcastCheckView.setOnCheckedChangeListener(checkNotificationChangeListener);

      // Check secteurs
      {
        final CheckBox checkSecteursView = (CheckBox)getView().findViewById(R.id.alarm_check_secteurs);
        checkSecteursView.setChecked(alarm.checkSecteurs);
        final OnCheckedChangeListener checkSecteursChangeListener = new OnCheckedChangeListener()
        {
          @Override
          public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked)
          {
            // Alarme
            alarm.checkSecteurs = isChecked;

            // Widget secteurs
            getView().findViewById(R.id.alarm_secteurs).setVisibility(isChecked ? View.VISIBLE : View.GONE);
          }
        };
        checkSecteursChangeListener.onCheckedChanged(checkSecteursView, alarm.checkSecteurs);
        checkSecteursView.setOnCheckedChangeListener(checkSecteursChangeListener);
      }

      // Secteurs
      {
        final SectorSelectorView sectorsView = (SectorSelectorView)getView().findViewById(R.id.alarm_secteurs);
        sectorsView.setCheckedSectors(alarm.secteurs);
        final OnSectorCheckedListener sectorCheckedListener = new OnSectorCheckedListener()
        {
          @Override
          public void onSectorChecked(final Sector sector, final boolean checked)
          {
            if (!checked && alarm.secteurs.contains(sector))
            {
              alarm.secteurs.remove(sector);
            }
            else if (checked && !alarm.secteurs.contains(sector))
            {
              alarm.secteurs.add(sector);
            }
          }
        };
        sectorsView.setOnCheckedSectorListener(sectorCheckedListener);
      }

      // Check plages horaires
      {
        final CheckBox checkPlagesHorairesView = (CheckBox)getView().findViewById(R.id.alarm_check_plages_horaires);
        checkPlagesHorairesView.setChecked(alarm.checkPlagesHoraires);
        final OnCheckedChangeListener checkPlagesHorairesChangeListener = new OnCheckedChangeListener()
        {
          @Override
          public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked)
          {
            // Alarme
            alarm.checkPlagesHoraires = isChecked;

            // Liste des plages horaires
            getView().findViewById(R.id.alarm_plages_horaires_list).setVisibility(isChecked ? View.VISIBLE : View.GONE);

            // Ajout d'une plage
            if (isChecked && (alarm.plagesHoraires.size() == 0))
            {
              onPlageHoraireAddClick(getView().findViewById(R.id.alarm_plage_vitesse_mini_add));
            }
          }
        };
        checkPlagesHorairesView.setOnCheckedChangeListener(checkPlagesHorairesChangeListener);
        checkPlagesHorairesChangeListener.onCheckedChanged(checkPlagesHorairesView, alarm.checkPlagesHoraires);
      }

      // Liste des plages horaires
      plagesHorairesAdapter.setData(alarm.plagesHoraires);

      // Check plages vitesses mini
      {
        final CheckBox checkVitessesMiniView = (CheckBox)getView().findViewById(R.id.alarm_check_vitesse_mini);
        checkVitessesMiniView.setChecked(alarm.checkVitesseMini);
        final OnCheckedChangeListener checkVitessesMiniChangeListener = new OnCheckedChangeListener()
        {
          @Override
          public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked)
          {
            // Alarme
            alarm.checkVitesseMini = isChecked;

            // Liste des vitesses
            getView().findViewById(R.id.alarm_plages_vitesse_mini_list).setVisibility(isChecked ? View.VISIBLE : View.GONE);

            // Ajout d'une vitesse
            if (isChecked && (alarm.plagesVitesseMini.size() == 0))
            {
              onPlageVitesseMiniAddClick(getView().findViewById(R.id.alarm_plage_vitesse_mini_add));
            }
          }
        };
        checkVitessesMiniChangeListener.onCheckedChanged(checkVitessesMiniView, alarm.checkVitesseMini);
        checkVitessesMiniView.setOnCheckedChangeListener(checkVitessesMiniChangeListener);
      }

      // Liste des vitesses mini
      plagesVitessesMiniAdapter.setData(alarm.plagesVitesseMini);

      // Check plages vitesses moy
      {
        final CheckBox checkVitessesMoyView = (CheckBox)getView().findViewById(R.id.alarm_check_vitesse_moy);
        checkVitessesMoyView.setChecked(alarm.checkVitesseMoy);
        final OnCheckedChangeListener checkVitessesMoyChangeListener = new OnCheckedChangeListener()
        {
          @Override
          public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked)
          {
            // Alarme
            alarm.checkVitesseMoy = isChecked;

            // Liste des vitesses
            getView().findViewById(R.id.alarm_plages_vitesse_moy_list).setVisibility(isChecked ? View.VISIBLE : View.GONE);

            // Ajout d'une vitesse
            if (isChecked && (alarm.plagesVitesseMoy.size() == 0))
            {
              onPlageVitesseMoyAddClick(getView().findViewById(R.id.alarm_plage_vitesse_moy_add));
            }
          }
        };
        checkVitessesMoyChangeListener.onCheckedChanged(checkVitessesMoyView, alarm.checkVitesseMoy);
        checkVitessesMoyView.setOnCheckedChangeListener(checkVitessesMoyChangeListener);
      }

      // Liste des vitesses moy
      plagesVitessesMoyAdapter.setData(alarm.plagesVitesseMoy);

      // Check plages vitesses maxi
      {
        final CheckBox checkVitessesMaxiView = (CheckBox)getView().findViewById(R.id.alarm_check_vitesse_maxi);
        checkVitessesMaxiView.setChecked(alarm.checkVitesseMaxi);
        final OnCheckedChangeListener checkVitessesMaxiChangeListener = new OnCheckedChangeListener()
        {
          @Override
          public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked)
          {
            // Alarme
            alarm.checkVitesseMaxi = isChecked;

            // Liste des vitesses
            getView().findViewById(R.id.alarm_plages_vitesse_maxi_list).setVisibility(isChecked ? View.VISIBLE : View.GONE);

            // Ajout d'une vitesse
            if (isChecked && (alarm.plagesVitesseMaxi.size() == 0))
            {
              onPlageVitesseMaxiAddClick(getView().findViewById(R.id.alarm_plage_vitesse_maxi_add));
            }
          }
        };
        checkVitessesMaxiChangeListener.onCheckedChanged(checkVitessesMaxiView, alarm.checkVitesseMaxi);
        checkVitessesMaxiView.setOnCheckedChangeListener(checkVitessesMaxiChangeListener);
      }

      // Liste des vitesses maxi
      plagesVitessesMaxiAdapter.setData(alarm.plagesVitesseMaxi);

      Log.d(getClass().getSimpleName(), "<<< synchronize()");
    }
    catch (final JSONException jse)
    {
      throw new RuntimeException(jse);
    }
  }

  /**
   * 
   * @param idVerifiee
   * @param idNonVerifiee
   * @return
   */
  private String[] getDefaultNotificationTexts(final int idVerifiee, final int idNonVerifiee)
  {
    // Initialisations
    final IFullProvidersService providersService = getFullProvidersService();
    final Releve releve;

    // Service ?
    if (providersService == null)
    {
      releve = null;
    }
    else
    {
      // Provider ?
      final BaliseProvider provider = getFullProvidersService().getBaliseProvider(alarm.provider);
      if (provider == null)
      {
        releve = null;
      }
      else
      {
        // Releve ?
        releve = provider.getReleveById(alarm.idBalise);
      }
    }

    final String defaultVerifieePattern = getActivity().getResources().getString(idVerifiee);
    final String defaultNonVerifieePattern = getActivity().getResources().getString(idNonVerifiee);
    final String defaultVerifiee = AlarmUtils.formatAlarmText(getActivity().getApplicationContext(), defaultVerifieePattern, alarm, releve);
    final String defaultNonVerifiee = AlarmUtils.formatAlarmText(getActivity().getApplicationContext(), defaultNonVerifieePattern, alarm, releve);

    return new String[] { defaultVerifiee, defaultNonVerifiee };
  }

  /**
   * 
   */
  private void synchronizeAndroidNotificationGroup()
  {
    // Textes par defaut
    final String[] defaults = getDefaultNotificationTexts(R.string.alarm_notification_android_text_verifiee, R.string.alarm_notification_android_text_non_verifiee);

    // Synchro
    synchronizeNotificationGroup(Notification.ANDROID, R.id.alarm_check_notification_android, R.id.alarm_notification_android_group, R.id.alarm_check_notification_android_perso, FIELD_CHECK_NOTIFICATION_ANDROID_PERSO,
        R.id.alarm_text_notification_android_perso_verifiee, FIELD_TEXTE_NOTIFICATION_ANDROID_PERSO_VERIFIEE, defaults[0], R.id.alarm_text_notification_android_perso_non_verifiee, FIELD_TEXTE_NOTIFICATION_ANDROID_PERSO_NON_VERIFIEE,
        defaults[1]);

    // Check son de notification
    {
      final CheckBox checkNotificationAndroidAudioView = (CheckBox)getView().findViewById(R.id.alarm_check_notification_android_audio);
      checkNotificationAndroidAudioView.setChecked(alarm.checkNotificationAndroidAudio);
      final OnCheckedChangeListener checkNotificationAndroidAudioChangeListener = new OnCheckedChangeListener()
      {
        @Override
        public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked)
        {
          // Alarme
          alarm.checkNotificationAndroidAudio = isChecked;

          // Widget
          getView().findViewById(R.id.alarm_activation_android_audio_choix).setEnabled(isChecked);
        }
      };
      checkNotificationAndroidAudioChangeListener.onCheckedChanged(checkNotificationAndroidAudioView, alarm.checkNotificationAndroidAudio);
      checkNotificationAndroidAudioView.setOnCheckedChangeListener(checkNotificationAndroidAudioChangeListener);
    }

    // Son de notification
    synchronizeNotificationAndroidAudio();
    final TextView textAudioSelectView = (TextView)getView().findViewById(R.id.alarm_activation_android_audio_choix);
    textAudioSelectView.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(final View view)
      {
        final Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, alarm.uriNotificationAndroidAudio);
        startActivityForResult(intent, REQUEST_NOTIFICATION_ANDROID_SELECT_AUDIO);
      }
    });

    // Check vibration
    {
      final CheckBox checkNotificationAndroidVibrationView = (CheckBox)getView().findViewById(R.id.alarm_check_notification_android_vibration);
      checkNotificationAndroidVibrationView.setChecked(alarm.checkNotificationAndroidVibration);
      final OnCheckedChangeListener checkNotificationAndroidVibrationChangeListener = new OnCheckedChangeListener()
      {
        @Override
        public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked)
        {
          // Alarme
          alarm.checkNotificationAndroidVibration = isChecked;
        }
      };
      checkNotificationAndroidVibrationChangeListener.onCheckedChanged(checkNotificationAndroidVibrationView, alarm.checkNotificationAndroidVibration);
      checkNotificationAndroidVibrationView.setOnCheckedChangeListener(checkNotificationAndroidVibrationChangeListener);
    }
  }

  /**
   * 
   */
  private void synchronizeVoiceNotificationGroup()
  {
    // Textes par defaut
    final String[] defaults = getDefaultNotificationTexts(R.string.alarm_notification_voix_text_verifiee, R.string.alarm_notification_voix_text_non_verifiee);

    // Synchro
    synchronizeNotificationGroup(Notification.VOIX, R.id.alarm_check_notification_voix, R.id.alarm_notification_voix_group, R.id.alarm_check_notification_voix_perso, FIELD_CHECK_NOTIFICATION_VOIX_PERSO,
        R.id.alarm_text_notification_voix_perso_verifiee, FIELD_TEXTE_NOTIFICATION_VOIX_PERSO_VERIFIEE, defaults[0], R.id.alarm_text_notification_voix_perso_non_verifiee, FIELD_TEXTE_NOTIFICATION_VOIX_PERSO_NON_VERIFIEE, defaults[1]);
  }

  /**
   * 
   */
  private void transmitAlarm()
  {
    final AlarmsFragment alarmsFragment = (AlarmsFragment)getFragmentManager().findFragmentById(R.id.alarms_part);
    if (alarmsFragment != null)
    {
      alarmsFragment.onAlarmUpdated(getCurrentPosition(), alarm);
    }
  }

  /**
   * 
   */
  void synchronizeProvider()
  {
    // La vue
    final String nomProvider = alarm.nomProvider == null ? getResources().getString(R.string.alarm_text_choix_fournisseur) : alarm.nomProvider;
    final TextView nomProviderView = (TextView)getView().findViewById(R.id.alarm_nom_provider);
    nomProviderView.setText(nomProvider);
    nomProviderView.setTag(alarm.provider);

    // Fragment des alarmes
    transmitAlarm();
  }

  /**
   * 
   */
  void synchronizeBalise()
  {
    // La vue
    final String nomBalise = alarm.nomBalise == null ? getResources().getString(R.string.alarm_text_choix_balise) : alarm.nomBalise;
    final TextView nomBaliseView = (TextView)getView().findViewById(R.id.alarm_nom_balise);
    nomBaliseView.setText(nomBalise);
    nomBaliseView.setTag(alarm.idBalise);

    // Synchro textes de notification
    synchronizeAndroidNotificationGroup();
    synchronizeVoiceNotificationGroup();

    // Fragment des alarmes
    transmitAlarm();
  }

  /**
   * 
   * @param notification
   * @param mainCheckId
   * @param groupId
   * @param textCheckId
   * @param textCheckField
   * @param textId
   * @param textField
   */
  private void synchronizeNotificationGroup(final Notification notification, final int mainCheckId, final int groupId, final int textCheckId, final Field textCheckField, final int textVerifieeId, final Field textVerifieeField,
      final String defaultVerifieeText, final int textNonVerifieeId, final Field textNonVerifieeField, final String defaultNonVerifieeText)
  {
    try
    {
      final CheckBox mainCheckView = (CheckBox)getView().findViewById(mainCheckId);
      final boolean notificationChecked = alarm.notifications.contains(notification);
      mainCheckView.setChecked(notificationChecked);
      final OnCheckedChangeListener checkNotificationChangeListener = new OnCheckedChangeListener()
      {
        @Override
        public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked)
        {
          // Alarme
          if (isChecked && !alarm.notifications.contains(notification))
          {
            alarm.notifications.add(notification);
          }
          else if (!isChecked && alarm.notifications.contains(notification))
          {
            alarm.notifications.remove(notification);
          }

          // Groupe notification
          getView().findViewById(groupId).setVisibility(isChecked ? View.VISIBLE : View.GONE);
        }
      };
      checkNotificationChangeListener.onCheckedChanged(mainCheckView, notificationChecked);
      mainCheckView.setOnCheckedChangeListener(checkNotificationChangeListener);

      // Notification perso
      final CheckBox checkNotificationPersoView = (CheckBox)getView().findViewById(textCheckId);
      checkNotificationPersoView.setChecked(textCheckField.getBoolean(alarm));
      final OnCheckedChangeListener checkNotificationPersoChangeListener = new OnCheckedChangeListener()
      {
        @Override
        public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked)
        {
          try
          {
            // Alarme
            textCheckField.setBoolean(alarm, isChecked);

            // Texte si alarme verifiee
            final EditText textVerifieeView = (EditText)getView().findViewById(textVerifieeId);
            textVerifieeView.setEnabled(isChecked);
            textVerifieeView.setText(isChecked ? (String)textVerifieeField.get(alarm) : defaultVerifieeText);
            if (isChecked)
            {
              textVerifieeView.requestFocus();
              textVerifieeView.selectAll();
            }

            // Texte si alarme non verifiee
            final EditText textNonVerifieeView = (EditText)getView().findViewById(textNonVerifieeId);
            textNonVerifieeView.setEnabled(isChecked);
            textNonVerifieeView.setText(isChecked ? (String)textNonVerifieeField.get(alarm) : defaultNonVerifieeText);
          }
          catch (final IllegalAccessException iae)
          {
            Log.e(AlarmFragment.this.getClass().getSimpleName(), iae.getMessage(), iae);
          }
        }
      };
      checkNotificationPersoChangeListener.onCheckedChanged(checkNotificationPersoView, textCheckField.getBoolean(alarm));
      checkNotificationPersoView.setOnCheckedChangeListener(checkNotificationPersoChangeListener);

      // Texte notification perso verifiee
      final EditText textVerifieeNotificationPersoView = (EditText)getView().findViewById(textVerifieeId);
      textVerifieeNotificationPersoView.setSelectAllOnFocus(true);
      textVerifieeNotificationPersoView.addTextChangedListener(new TextWatcher()
      {
        @Override
        public void afterTextChanged(final Editable editable)
        {
          try
          {
            if (textCheckField.getBoolean(alarm))
            {
              textVerifieeField.set(alarm, editable.toString());
            }
          }
          catch (final IllegalAccessException iae)
          {
            Log.e(AlarmFragment.this.getClass().getSimpleName(), iae.getMessage(), iae);
          }
        }

        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after)
        {
          // Nothing
        }

        @Override
        public void onTextChanged(final CharSequence s, final int start, final int before, final int count)
        {
          // Nothing
        }
      });

      // Texte notification perso non verifiee
      final EditText textNonVerifieeNotificationPersoView = (EditText)getView().findViewById(textNonVerifieeId);
      textNonVerifieeNotificationPersoView.setSelectAllOnFocus(true);
      textNonVerifieeNotificationPersoView.addTextChangedListener(new TextWatcher()
      {
        @Override
        public void afterTextChanged(final Editable editable)
        {
          try
          {
            if (textCheckField.getBoolean(alarm))
            {
              textNonVerifieeField.set(alarm, editable.toString());
            }
          }
          catch (final IllegalAccessException iae)
          {
            Log.e(AlarmFragment.this.getClass().getSimpleName(), iae.getMessage(), iae);
          }
        }

        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after)
        {
          // Nothing
        }

        @Override
        public void onTextChanged(final CharSequence s, final int start, final int before, final int count)
        {
          // Nothing
        }
      });
    }
    catch (final IllegalAccessException iae)
    {
      Log.e(getClass().getSimpleName(), iae.getMessage(), iae);
    }
  }

  /**
   * 
   */
  private void synchronizeNotificationAndroidAudio()
  {
    final String texte;
    if (alarm.uriNotificationAndroidAudio == null)
    {
      texte = getResources().getString(R.string.alarm_text_notification_android_audio_aucune);
    }
    else
    {
      final Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), alarm.uriNotificationAndroidAudio);
      texte = ringtone.getTitle(getActivity());
    }
    final TextView textview = (TextView)getView().findViewById(R.id.alarm_activation_android_audio_choix);
    textview.setText(texte);
  }

  /**
   * 
   * @param view
   */
  private void onPlageHoraireDelClick(final View view)
  {
    // Recuperation de la position
    final ViewGroup group = (ViewGroup)getView().findViewById(R.id.alarm_plages_horaires_list);
    final View parentView = (View)view.getParent();
    final int position = getPositionInParent(parentView, group);

    // Suppression
    final PlageHoraire plageHoraire = alarm.plagesHoraires.get(position);
    plagesHorairesAdapter.remove(plageHoraire);
    alarm.plagesHoraires.remove(plageHoraire);

    // Plus de plage ?
    if (alarm.plagesHoraires.size() == 0)
    {
      ((CheckBox)getView().findViewById(R.id.alarm_check_plages_horaires)).setChecked(false);
    }
  }

  /**
   * 
   * @param view
   */
  @SuppressWarnings("unused")
  void onPlageHoraireAddClick(final View view)
  {
    // Ajout de la plage
    final PlageHoraire plageHoraire = new PlageHoraire(8, 0, 20, 0);
    plagesHorairesAdapter.add(plageHoraire);
    alarm.plagesHoraires.add(plageHoraire);

    // Activation de la checkbox
    alarm.checkPlagesHoraires = true;
    ((CheckBox)getView().findViewById(R.id.alarm_check_plages_horaires)).setChecked(true);
  }

  /**
   * 
   * @param view
   */
  @SuppressWarnings("unused")
  public void onProviderClick(final View view)
  {
    // Initialisations
    final IFullProvidersService providersService = getFullProvidersService();

    if (providersService != null)
    {
      // Liste des providers
      final Set<String> providersKeys = providersService.getActiveBaliseProviders();
      final String[] items = new String[providersKeys.size()];
      final String[] itemNames = new String[providersKeys.size()];
      int i = 0;
      int selectedItem = -1;
      for (final String providerKey : providersKeys)
      {
        items[i] = providerKey;
        itemNames[i] = AlarmUtils.getProviderName(providersService, providerKey);

        // Selection ?
        if (providerKey.equals(alarm.provider))
        {
          selectedItem = i;
        }

        // Next
        i++;
      }

      // Listener click
      final OnClickListener clickListener = new OnClickListener()
      {
        @Override
        public void onClick(final DialogInterface dialog, final int which)
        {
          if (which >= 0)
          {
            // Synchro provider
            final boolean providerChanged = (alarm.nomProvider == null) || !alarm.nomProvider.equals(itemNames[which]);
            alarm.provider = items[which];
            alarm.nomProvider = itemNames[which];
            synchronizeProvider();

            // Synchro balise
            if (providerChanged)
            {
              alarm.idBalise = null;
              alarm.nomBalise = null;
              synchronizeBalise();
            }

            // Fermeture
            dialog.dismiss();
          }
        }
      };

      // Dialog de choix
      final SingleChoiceDialogFragment dialog = new SingleChoiceDialogFragment(getActivity(), R.drawable.icon, getResources().getString(R.string.alarm_title_fournisseur), itemNames, selectedItem, false, null, clickListener, null, null,
          null, null, null, null, null);
      dialog.show(getFragmentManager(), "onProviderClick");
    }
  }

  /**
   * 
   * @param view
   */
  @SuppressWarnings("unused")
  public void onBaliseClick(final View view)
  {
    // Initialisations
    final IFullProvidersService providersService = getFullProvidersService();

    if ((providersService != null) && (alarm.provider != null))
    {
      final BaliseProvider provider = providersService.getBaliseProvider(alarm.provider);
      if (provider == null)
      {
        Toast.makeText(getActivity(), R.string.alarm_message_provider_inactif, Toast.LENGTH_SHORT).show();
      }
      else
      {
        // Liste des balises
        final Collection<Balise> orgBalises = provider.getBalises();
        final List<Balise> balises = new ArrayList<Balise>(orgBalises);

        // Triees par ordre alphabetique
        Collections.sort(balises, new Comparator<Balise>()
        {
          @Override
          public int compare(final Balise balise1, final Balise balise2)
          {

            return balise1.nom.compareTo(balise2.nom);
          }
        });

        // Items
        final String[] items = new String[balises.size()];
        final String[] itemNames = new String[balises.size()];
        int i = 0;
        int selectedItem = -1;
        for (final Balise balise : balises)
        {
          items[i] = balise.id;
          itemNames[i] = balise.nom;

          // Selection ?
          if ((alarm.idBalise != null) && alarm.idBalise.equals(balise.id))
          {
            selectedItem = i;
          }

          // Next
          i++;
        }

        // Listener click
        final OnClickListener clickListener = new OnClickListener()
        {
          @Override
          public void onClick(final DialogInterface dialog, final int which)
          {
            if (which >= 0)
            {
              // Synchro balise
              alarm.idBalise = items[which];
              alarm.nomBalise = itemNames[which];
              synchronizeBalise();

              // Fermeture
              dialog.dismiss();
            }
          }
        };

        // Dialog de choix
        final SingleChoiceDialogFragment dialog = new SingleChoiceDialogFragment(getActivity(), R.drawable.icon, getResources().getString(R.string.alarm_title_balise), itemNames, selectedItem, false, null, clickListener, null, null, null,
            null, null, null, null);
        dialog.show(getFragmentManager(), "onProviderClick");
      }
    }
  }

  /**
   * 
   * @param view
   */
  @SuppressWarnings("unused")
  public void onBroadcastHelpClick(final View view)
  {
    ActivityCommons.goToUrl(getActivity(), "http://www.mobibalises.net/broadcast-help.php");
  }

  /**
   * 
   * @param view
   * @param parent
   * @return
   */
  private static int getPositionInParent(final View view, final ViewGroup parent)
  {
    final int childCount = parent.getChildCount();
    for (int i = 0; i < childCount; i++)
    {
      if (view == parent.getChildAt(i))
      {
        return i;
      }
    }

    return -1;
  }

  /**
   * 
   * @param view
   */
  @SuppressWarnings("unused")
  void onPlageVitesseMiniAddClick(final View view)
  {
    // Ajout de la plage
    final PlageVitesse plageVitesse = new PlageVitesse(0, 10);
    plagesVitessesMiniAdapter.add(plageVitesse);
    alarm.plagesVitesseMini.add(plageVitesse);

    // Activation de la checkbox
    alarm.checkVitesseMini = true;
    ((CheckBox)getView().findViewById(R.id.alarm_check_vitesse_mini)).setChecked(true);
  }

  /**
   * 
   * @param view
   */
  @SuppressWarnings("unused")
  void onPlageVitesseMoyAddClick(final View view)
  {
    // Ajout de la plage
    final PlageVitesse plageVitesse = new PlageVitesse(5, 20);
    plagesVitessesMoyAdapter.add(plageVitesse);
    alarm.plagesVitesseMoy.add(plageVitesse);

    // Activation de la checkbox (qui va ajouter une plage aussi)
    alarm.checkVitesseMoy = true;
    ((CheckBox)getView().findViewById(R.id.alarm_check_vitesse_moy)).setChecked(true);
  }

  /**
   * 
   * @param view
   */
  @SuppressWarnings("unused")
  void onPlageVitesseMaxiAddClick(final View view)
  {
    // Ajout de la plage
    final PlageVitesse plageVitesse = new PlageVitesse(15, 40);
    plagesVitessesMaxiAdapter.add(plageVitesse);
    alarm.plagesVitesseMaxi.add(plageVitesse);

    // Activation de la checkbox
    alarm.checkVitesseMaxi = true;
    ((CheckBox)getView().findViewById(R.id.alarm_check_vitesse_maxi)).setChecked(true);
  }

  /**
   * 
   * @param view
   */
  private void onPlageVitesseDelClick(final View view)
  {
    final TypePlageVitesse type = (TypePlageVitesse)view.getTag(R.id.alarm_plage_vitesse_del_type);
    switch (type)
    {
      case MINI:
        onPlageVitesseDelClick(view, R.id.alarm_plages_vitesse_mini_list, R.id.alarm_check_vitesse_mini, alarm.plagesVitesseMini, plagesVitessesMiniAdapter);
        break;
      case MOY:
        onPlageVitesseDelClick(view, R.id.alarm_plages_vitesse_moy_list, R.id.alarm_check_vitesse_moy, alarm.plagesVitesseMoy, plagesVitessesMoyAdapter);
        break;
      case MAXI:
        onPlageVitesseDelClick(view, R.id.alarm_plages_vitesse_maxi_list, R.id.alarm_check_vitesse_maxi, alarm.plagesVitesseMaxi, plagesVitessesMaxiAdapter);
        break;
    }
  }

  /**
   * 
   * @param view
   * @param idViewGroup
   * @param idCheckBox
   * @param vitesses
   * @param adapter
   */
  private void onPlageVitesseDelClick(final View view, final int idViewGroup, final int idCheckBox, final List<PlageVitesse> vitesses, final PlagesVitessesAdapter adapter)
  {
    // Recuperation de la position
    final ViewGroup group = (ViewGroup)getView().findViewById(idViewGroup);
    final View parentView = (View)view.getParent();
    final int position = getPositionInParent(parentView, group);

    // Suppression
    final PlageVitesse plageVitesse = vitesses.get(position);
    adapter.remove(plageVitesse);
    vitesses.remove(plageVitesse);

    // Plus de plage ?
    if (vitesses.size() == 0)
    {
      ((CheckBox)getView().findViewById(idCheckBox)).setChecked(false);
    }
  }

  @Override
  public void onClick(final View view)
  {
    switch (view.getId())
    {
      case R.id.alarm_nom_provider:
        onProviderClick(view);
        break;
      case R.id.alarm_nom_balise:
        onBaliseClick(view);
        break;
      case R.id.alarm_notification_broadcast_help:
        onBroadcastHelpClick(view);
        break;
      case R.id.alarm_plage_vitesse_del:
        onPlageVitesseDelClick(view);
        break;
      case R.id.alarm_plage_vitesse_moy_add:
        onPlageVitesseMoyAddClick(view);
        break;
      case R.id.alarm_plage_vitesse_maxi_add:
        onPlageVitesseMaxiAddClick(view);
        break;
      case R.id.alarm_plage_vitesse_mini_add:
        onPlageVitesseMiniAddClick(view);
        break;
      case R.id.alarm_plage_horaire_del:
        onPlageHoraireDelClick(view);
        break;
      case R.id.alarm_plage_horaire_add:
        onPlageHoraireAddClick(view);
        break;
    }
  }

  /**
   * 
   * @return
   */
  private IFullProvidersService getFullProvidersService()
  {
    return (IFullProvidersService)((IProvidersServiceActivity)getActivity()).getProvidersService();
  }

  /**
   * 
   * @param progress
   * @return
   */
  static double getVitesseForProgress(final int progress)
  {
    // -1 = vitesse "infinie"
    return (progress >= VITESSES.length ? -1 : VITESSES[progress]);
  }

  /**
   * 
   * @param progress
   * @return
   */
  static String getStringVitesseForProgress(final int progress)
  {
    // Conversion dans l'unite de l'utilisateur
    final double vitesse = ActivityCommons.getFinalSpeed(getVitesseForProgress(progress));
    return (vitesse >= 0 ? VITESSES_FORMAT.format(vitesse) : STRING_VITESSE_MAXI_INFINIE);
  }

  /**
   * 
   * @param vitesse
   * @return
   */
  static int getProgressForVitesse(final double vitesse)
  {
    final int nbVitesses = VITESSES.length;
    for (int i = 0; i < nbVitesses; i++)
    {
      if (vitesse == VITESSES[i])
      {
        return i;
      }
    }

    return VITESSES.length;
  }

  /**
   * 
   * @param debutText
   * @param heures
   * @param minutes
   */
  static void formatHeure(final TextView debutText, final int heures, final int minutes)
  {
    final DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
    final Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.HOUR_OF_DAY, heures);
    calendar.set(Calendar.MINUTE, minutes);
    debutText.setText(timeFormat.format(calendar.getTime()));
  }

  @Override
  public void onProvidersServiceConnected(final IProvidersService providersService)
  {
    // Synchro de la vue pour les notifications android
    synchronizeAndroidNotificationGroup();

    // Synchro de la vue pour les notifications voix
    synchronizeVoiceNotificationGroup();
  }
}
