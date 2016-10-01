package com.pedro.android.mobibalises.alarm;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.IProvidersServiceActivity;
import org.pedro.android.mobibalises_common.IProvidersServiceFragment;
import org.pedro.android.mobibalises_common.service.IProvidersService;
import org.pedro.android.mobibalises_lgpl.alarm.BaliseAlarm;
import org.pedro.android.mobibalises_lgpl.alarm.BaliseAlarm.Notification;
import org.pedro.android.mobibalises_lgpl.alarm.BaliseAlarm.PlageVitesse;
import org.pedro.misc.Sector;
import org.pedro.spots.Utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.pedro.android.mobibalises.R;
import com.pedro.android.mobibalises.service.IFullProvidersService;

/**
 * 
 * @author pedro.m
 *
 */
public class AlarmsFragment extends ListFragment implements IProvidersServiceFragment
{
  private static final String STRING_PROVIDER_BALISE_NULL = "---";
  private static final int    INTENT_REQUEST_ALARM        = 1;

  private boolean             dualPart;
  private int                 currentPosition             = -1;
  private AlarmFragment       alarmFragment               = null;

  ListView                    listView;
  BaliseAlarmAdapter          adapter;
  ActionBarCallback           actionBarCallback           = new ActionBarCallback(this);
  ActionMode                  actionMode;

  /**
   * 
   * @author pedro.m
   */
  private static class ActionBarCallback implements ActionMode.Callback
  {
    private AlarmsFragment fragment;

    /**
     * 
     * @param fragment
     */
    ActionBarCallback(final AlarmsFragment fragment)
    {
      this.fragment = fragment;
    }

    @Override
    public boolean onActionItemClicked(final ActionMode mode, final MenuItem item)
    {
      // Suppression d'alarme(s)
      if (item.getItemId() == R.id.item_alarms_delete)
      {
        // Suppression des alarmes
        for (int i = fragment.listView.getAdapter().getCount() - 1; i >= 0; i--)
        {
          if (fragment.listView.isItemChecked(i))
          {
            final BaliseAlarm alarm = fragment.adapter.getItem(i);
            fragment.listView.setItemChecked(i, false); // Pour mettre a jour le compte d'alarmes selectionnees, sinon il n'est pas exact
            fragment.adapter.remove(alarm);
          }
        }

        // Notification a la liste
        fragment.adapter.notifyDataSetChanged();

        // Fin du mode selection
        fragment.actionMode.finish();

        // Reselection du dernier item
        fragment.reselectCurrent();

        return true;
      }
      return false;
    }

    @Override
    public boolean onCreateActionMode(final ActionMode mode, final Menu menu)
    {
      mode.getMenuInflater().inflate(R.menu.menu_alarms_contextual, menu);
      return true;
    }

    @Override
    public void onDestroyActionMode(final ActionMode mode)
    {
      fragment.toggleChoiceMode();
    }

    @Override
    public boolean onPrepareActionMode(final ActionMode mode, final Menu menu)
    {
      return false;
    }

    /**
     * 
     */
    void onShutdown()
    {
      fragment = null;
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class BaliseAlarmAdapter extends ArrayAdapter<BaliseAlarm>
  {
    private final LayoutInflater inflater;
    AlarmsFragment               fragment;

    /**
     * 
     * @param fragment
     */
    public BaliseAlarmAdapter(final AlarmsFragment fragment)
    {
      super(fragment.getActivity(), R.layout.alarm_item);
      this.fragment = fragment;
      inflater = (LayoutInflater)fragment.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    /**
     * 
     * @param data
     */
    public void setData(final List<BaliseAlarm> data)
    {
      // Vidage
      clear();

      // Remplissage
      if (data != null)
      {
        // Utilisable seulement a partir de l'API 11 : addAll(data);
        for (final BaliseAlarm item : data)
        {
          add(item);
        }
      }
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent)
    {
      // Recyclage eventuel de la vue
      final View view;
      if (convertView == null)
      {
        view = inflater.inflate(R.layout.alarm_item, parent, false);
      }
      else
      {
        view = convertView;
      }

      // Item selectionne
      final BaliseAlarm alarm = getItem(position);

      // MAJ de la vue
      ((TextView)view.findViewById(R.id.alarm_item_nom_provider)).setText(alarm.nomProvider == null ? STRING_PROVIDER_BALISE_NULL : alarm.nomProvider);
      ((TextView)view.findViewById(R.id.alarm_item_nom_balise)).setText(alarm.nomBalise == null ? STRING_PROVIDER_BALISE_NULL : alarm.nomBalise);
      final CompoundButton switcher = (CompoundButton)view.findViewById(R.id.alarm_item_active);
      switcher.setOnCheckedChangeListener(null);
      switcher.setChecked(alarm.active);
      switcher.setOnCheckedChangeListener(new OnCheckedChangeListener()
      {
        @Override
        public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked)
        {
          // Etat de l'alarme
          alarm.active = isChecked;
          fragment.saveAlarm(position, null);

          // Notification du service (pour reveiller/endormir eventuellement les threads de providers de balise)
          final IFullProvidersService providersService = (IFullProvidersService)((IProvidersServiceActivity)getContext()).getProvidersService();
          providersService.updateBaliseProviders(true);

          // Notification du service
          providersService.onAlarmActivationChanged(alarm);
        }
      });

      return view;
    }

    /**
     * 
     */
    void onShutdown()
    {
      fragment = null;
    }
  }

  @Override
  public void onCreate(final Bundle savedInstanceState)
  {
    // Super
    Log.d(getClass().getSimpleName(), ">>> onCreate()");
    super.onCreate(savedInstanceState);

    // Divers
    setHasOptionsMenu(true);

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onCreate()");
  }

  @Override
  public void onDestroy()
  {
    // Super
    Log.d(getClass().getSimpleName(), ">>> onDestroy()");
    super.onDestroy();

    // Sauvegarde des alarmes
    saveAlarms();

    // Liberations
    actionBarCallback.onShutdown();
    adapter.onShutdown();

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onDestroy()");
  }

  @Override
  public void onCreateOptionsMenu(final Menu inMenu, final MenuInflater inflater)
  {
    // Creation
    inflater.inflate(R.menu.menu_alarms, inMenu);
  }

  @Override
  public boolean onOptionsItemSelected(final MenuItem item)
  {
    switch (item.getItemId())
    {
      case R.id.item_alarms_add:
        addAlarm();
        return true;

      case android.R.id.home:
        // Fin de l'activite
        getActivity().finish();
        return true;

        // Aucun
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  /**
   * 
   */
  void addAlarm()
  {
    // Creation Alarme avec valeurs par défaut
    final BaliseAlarm alarm = new BaliseAlarm();
    alarm.setId(String.valueOf(System.currentTimeMillis()));
    alarm.notifications.add(Notification.ANDROID);
    alarm.checkSecteurs = true;
    alarm.secteurs.add(Sector.N_NNE);
    alarm.secteurs.add(Sector.NNO_N);
    alarm.checkVitesseMoy = true;
    alarm.plagesVitesseMoy.add(new PlageVitesse(5, 15));

    if (dualPart)
    {
      // Ajout Alarme
      adapter.add(alarm);

      // MAJ liste
      final int newPosition = adapter.getCount() - 1;
      listView.setItemChecked(newPosition, true);
      onAlarmClick(newPosition);
    }
    else
    {
      // Edition
      editAlarm(adapter.getCount(), alarm);
    }
  }

  @Override
  public void onActivityCreated(final Bundle savedInstanceState)
  {
    // Super
    Log.d(getClass().getSimpleName(), ">>> onActivityCreated()");
    super.onActivityCreated(savedInstanceState);

    // En 2 parties ?
    final View alarmPart = getActivity().findViewById(R.id.alarm_part);
    dualPart = (alarmPart != null) && (alarmPart.getVisibility() == View.VISIBLE);
    Log.d(getClass().getSimpleName(), "dualPart : " + dualPart);

    // Initialisation ListView
    initListView();

    // Remplissage
    loadAlarms();

    // Position de depart
    final int intentPosition = getActivity().getIntent().getIntExtra(AlarmsFragmentActivity.PARAM_POSITION, -1);
    final int position;
    if ((intentPosition >= 0) && (adapter.getCount() > intentPosition))
    {
      position = intentPosition;
    }
    else if (dualPart && (adapter.getCount() > 0))
    {
      position = 0;
    }
    else
    {
      position = -1;
    }

    // Selection
    if (position >= 0)
    {
      listView.setItemChecked(position, true);
      onAlarmClick(position);
    }
  }

  @Override
  public void onActivityResult(final int requestCode, final int resultCode, final Intent data)
  {
    switch (requestCode)
    {
      case INTENT_REQUEST_ALARM:
        try
        {
          // Chaine JSON
          final String json = (data != null ? data.getStringExtra(AlarmFragmentActivity.INTENT_ALARM_JSON) : null);

          if (json != null)
          {
            // Analyse
            final BaliseAlarm returnAlarm = new BaliseAlarm();
            returnAlarm.fromJSON(new JSONObject(json));

            // Recherche parmi les alarmes existantes
            final int position = adapter.getPosition(returnAlarm);
            if (position >= 0)
            {
              final BaliseAlarm alarm = adapter.getItem(position);
              alarm.copyFrom(returnAlarm, false);
              listView.setItemChecked(position, false);
            }
            else
            {
              if (!Utils.isStringVide(returnAlarm.provider) && !Utils.isStringVide(returnAlarm.idBalise))
              {
                // Ajout seulement si provider et balise ont ete choisis
                adapter.add(returnAlarm);
              }
              else
              {
                // Message d'info
                Toast.makeText(getActivity(), R.string.alarm_message_non_creee, Toast.LENGTH_SHORT).show();
              }
            }

            // Notification listView
            adapter.notifyDataSetChanged();
          }
        }
        catch (final JSONException jse)
        {
          throw new RuntimeException(jse);
        }
        break;

      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  /**
   * 
   */
  private void initListView()
  {
    // Initialisations
    adapter = new BaliseAlarmAdapter(this);
    setListAdapter(adapter);
    listView = getListView();
    listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

    // Vue quand liste vide
    final View emptyView = getActivity().getLayoutInflater().inflate(R.layout.alarms_empty_view, null, false);
    emptyView.setOnClickListener(new android.view.View.OnClickListener()
    {

      @Override
      public void onClick(final View view)
      {
        addAlarm();
      }
    });
    getActivity().addContentView(emptyView, new LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
    listView.setEmptyView(emptyView);

    // Ecoute sur click
    listView.setOnItemClickListener(new OnItemClickListener()
    {
      @Override
      public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id)
      {
        onAlarmClick(position);
      }
    });

    // Ecoute sur click long
    listView.setOnItemLongClickListener(new OnItemLongClickListener()
    {
      @Override
      public boolean onItemLongClick(final AdapterView<?> parent, final View view, final int position, final long id)
      {
        final boolean wasMultiple = isMultipleMode();
        if (wasMultiple)
        {
          // Deja en selection multiple, on ne fait rien
          return false;
        }

        // Sinon passage en mode multiple
        toggleChoiceMode();
        listView.setItemChecked(position, true);
        updateActionBarSubtitle();

        return true;
      }
    });
  }

  /**
   * 
   * @param position
   */
  void onAlarmClick(final int position)
  {
    // Position valide ?
    if ((position < 0) || (position >= adapter.getCount()))
    {
      return;
    }

    // MAJ
    if (isMultipleMode())
    {
      // Sous-titre
      updateActionBarSubtitle();

      // Plus aucun item selectionne => fin du mode multi-selection
      if (getCheckedItemCount() <= 0)
      {
        actionMode.finish();
      }
    }
    else
    {
      // En mode single, mise en couleur de l'alarme selectionnee
      if (dualPart)
      {
        // 2 panneaux
        currentPosition = position;
      }

      // Edition de l'alarme
      editAlarm(position, adapter.getItem(position));
    }
  }

  /**
   * 
   * @param position
   * @param alarm
   */
  protected void onAlarmUpdated(final int position, final BaliseAlarm alarm)
  {
    if ((position >= 0) && (position < adapter.getCount()))
    {
      // Recuperation de l'alarme cible
      final BaliseAlarm destAlarm = adapter.getItem(position);

      // Copie
      destAlarm.copyFrom(alarm, false);

      // Notification a la liste
      adapter.notifyDataSetChanged();
    }
  }

  /**
   * 
   * @return
   */
  boolean isMultipleMode()
  {
    return (listView.getChoiceMode() == AbsListView.CHOICE_MODE_MULTIPLE);
  }

  /**
   * 
   */
  void toggleChoiceMode()
  {
    // Initialisations
    final boolean multiple = isMultipleMode();

    // Deselection de tous les items
    setAllItemsChecked(false);

    // Mode de la liste
    if (multiple)
    {
      // Passage en mode single
      listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

      // Reselection du dernier item
      reselectCurrent();
    }
    else
    {
      // Passage en mode multiple
      listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
      actionMode = ((ActionBarActivity)getActivity()).startSupportActionMode(actionBarCallback);
      actionMode.setTitle(R.string.alarm_title_selection);
      updateActionBarSubtitle();
    }
  }

  /**
   * 
   */
  void reselectCurrent()
  {
    // S'il n'y a plus aucune alarme
    if (adapter.getCount() <= 0)
    {
      // Si mode dual, écran vide à droite
      if (dualPart && alarmFragment != null)
      {
        // Mise a jour
        final FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.remove(alarmFragment);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        transaction.commit();
      }

      // Pas de sélection
      return;
    }

    // Sinon sélection de l'item précédemment sélectionné
    if (currentPosition >= 0)
    {
      final int newPosition = (currentPosition >= adapter.getCount() ? adapter.getCount() - 1 : currentPosition);
      listView.setItemChecked(newPosition, true);
      onAlarmClick(newPosition);
    }
  }

  /**
   * 
   * @param checked
   */
  void setAllItemsChecked(final boolean checked)
  {
    final SparseBooleanArray positions = listView.getCheckedItemPositions();
    final int nbAlarms = listView.getCount();
    for (int i = 0; i < nbAlarms; i++)
    {
      if ((positions == null) || (positions.get(i) != checked))
      {
        listView.setItemChecked(i, checked);
      }
    }
  }

  /**
   * 
   */
  void updateActionBarSubtitle()
  {
    final int nbAlarmes = getCheckedItemCount();
    final int messageId = (nbAlarmes > 1 ? R.string.alarm_title_selection_sous_titre_pluriel : R.string.alarm_title_selection_sous_titre_singulier);
    final String subtitle = getResources().getString(messageId, Integer.toString(getCheckedItemCount(), 10));
    actionMode.setSubtitle(subtitle);
  }

  /**
   * 
   * @return
   */
  private int getCheckedItemCount()
  {
    final SparseBooleanArray itemPositions = listView.getCheckedItemPositions();
    final int nbItems = itemPositions.size();
    int count = 0;
    for (int i = 0; i < nbItems; i++)
    {
      if (itemPositions.valueAt(i))
      {
        count++;
      }
    }

    return count;
  }

  /**
   * 
   * @param position
   * @param alarm
   */
  void editAlarm(final int position, final BaliseAlarm alarm)
  {
    Log.d(getClass().getSimpleName(), "editAlarm(" + position + ", " + alarm + ")");
    try
    {
      if (dualPart)
      {
        // En mode dual
        // Recherche du fragment actuellement affiche
        alarmFragment = (AlarmFragment)getFragmentManager().findFragmentById(R.id.alarm_part);
        if ((alarmFragment == null) || (alarmFragment.getCurrentPosition() != position))
        {
          // Il faut creer un nouveau fragment
          alarmFragment = AlarmFragment.newInstance(position, alarm.toJSON().toString());

          // Mise a jour
          final FragmentTransaction transaction = getFragmentManager().beginTransaction();
          transaction.replace(R.id.alarm_part, alarmFragment);
          transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
          transaction.commit();
        }
      }
      else
      {
        // Demarrage de l'activite externe
        final Intent intent = new Intent(getActivity().getApplicationContext().getString(R.string.intent_alarm_action));
        intent.putExtra(AlarmFragmentActivity.INTENT_ALARM_JSON, alarm.toJSON().toString());
        startActivityForResult(intent, INTENT_REQUEST_ALARM);
      }
    }
    catch (final JSONException jse)
    {
      throw new RuntimeException(jse);
    }
  }

  /**
   * 
   */
  private void saveAlarms()
  {
    // Initialisations
    final SharedPreferences preferences = ActivityCommons.getSharedPreferences(getActivity().getApplicationContext());
    final Editor editor = preferences.edit();

    // Nombre d'alarmes
    final int nbAlarmes = adapter.getCount();
    editor.putInt(AlarmUtils.PREFS_NB_ALARMS, adapter.getCount());

    // Alarmes
    for (int i = 0; i < nbAlarmes; i++)
    {
      saveAlarm(i, editor);
    }

    // Fin
    editor.commit();
  }

  /**
   * 
   * @param position
   * @param inEditor
   */
  void saveAlarm(final int position, final Editor inEditor)
  {
    // Initialisations
    final Editor editor;
    if (inEditor != null)
    {
      editor = inEditor;
    }
    else
    {
      final SharedPreferences preferences = ActivityCommons.getSharedPreferences(getActivity().getApplicationContext());
      editor = preferences.edit();
    }

    // Sauvegarde
    try
    {
      AlarmUtils.saveAlarm(getActivity().getApplicationContext(), adapter.getItem(position), position, inEditor);
    }
    catch (final JSONException jse)
    {
      throw new RuntimeException(jse);
    }

    // Fin
    if (inEditor == null)
    {
      editor.commit();
    }
  }

  /**
   * 
   */
  private void loadAlarms()
  {
    // Initialisations
    final List<BaliseAlarm> alarms = AlarmUtils.loadAlarms(getActivity().getApplicationContext());
    Log.d(getClass().getSimpleName(), "loaded alarms : " + alarms);
    adapter.setData(alarms);
  }

  @Override
  public void onProvidersServiceConnected(final IProvidersService providersService)
  {
    if (dualPart && (alarmFragment != null))
    {
      alarmFragment.onProvidersServiceConnected(providersService);
    }
  }
}
