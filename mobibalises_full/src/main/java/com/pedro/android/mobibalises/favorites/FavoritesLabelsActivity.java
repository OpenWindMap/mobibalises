package com.pedro.android.mobibalises.favorites;

import java.lang.ref.WeakReference;
import java.util.List;

import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.Strings;
import org.pedro.android.mobibalises_common.service.ProvidersServiceBinder;
import org.pedro.balises.Utils;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.pedro.android.mobibalises.R;
import com.pedro.android.mobibalises.service.IFullProvidersService;
import com.pedro.android.mobibalises.service.ProvidersService;
import com.pedro.android.mobibalises.widget.BalisesWidgets;

/**
 * 
 * @author pedro.m
 */
public class FavoritesLabelsActivity extends ListActivity
{
  private Resources                  resources;

  // Service
  IFullProvidersService              providersService;
  private ProvidersServiceConnection providersServiceConnection;

  FavoritesLabelsAdapter             labelsAdapter;

  /**
   * 
   * @author pedro.m
   */
  private static class FavoritesLabelsAdapter extends ArrayAdapter<String>
  {
    /**
     * 
     * @param labelsActivity
     */
    public FavoritesLabelsAdapter(final FavoritesLabelsActivity labelsActivity)
    {
      super(labelsActivity, R.layout.favorite_label_item, R.id.fav_label_item_text);
    }

    @Override
    public View getView(final int position, final View view, final ViewGroup parent)
    {
      final View retour = super.getView(position, view, parent);
      final Integer tag = Integer.valueOf(position);
      final TextView textView = (TextView)retour.findViewById(R.id.fav_label_item_text);
      final View delView = retour.findViewById(R.id.fav_label_item_del);
      textView.setTag(tag);
      delView.setTag(tag);

      return retour;
    }
  }

  @Override
  protected void onCreate(final Bundle savedInstanceState)
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> onCreate()");
    super.onCreate(savedInstanceState);
    setContentView(R.layout.favorites_labels);
    resources = getResources();

    // Adapteur
    labelsAdapter = new FavoritesLabelsAdapter(this);
    setListAdapter(labelsAdapter);

    // Initialisation
    initProvidersService();

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onCreate()");
  }

  @Override
  protected void onDestroy()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> onDestroy()");
    super.onDestroy();

    // Notification au service
    providersServiceConnection.privateOnServiceDisconnected();

    // MAJ des widgets
    BalisesWidgets.synchronizeWidgets(getApplicationContext(), providersService, null);

    // Deconnexion du service
    unbindService(providersServiceConnection);

    // Liberation vues
    ActivityCommons.unbindDrawables(findViewById(android.R.id.content));

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onDestroy()");
  }

  /**
   * 
   */
  void initLabels()
  {
    // Liste des labels
    for (final String label : providersService.getFavoritesService().getLabels())
    {
      // Creation
      labelsAdapter.add(label);
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class ProvidersServiceConnection implements ServiceConnection
  {
    private final WeakReference<FavoritesLabelsActivity> favLabelsActivity;

    /**
     * 
     * @param favLabelsActivity
     */
    ProvidersServiceConnection(final FavoritesLabelsActivity favLabelsActivity)
    {
      this.favLabelsActivity = new WeakReference<FavoritesLabelsActivity>(favLabelsActivity);
    }

    @Override
    public void onServiceConnected(final ComponentName name, final IBinder inBinder)
    {
      // Recuperation du service
      favLabelsActivity.get().providersService = (IFullProvidersService)((ProvidersServiceBinder)inBinder).getService();

      // Init labels
      favLabelsActivity.get().initLabels();
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
      if (favLabelsActivity.get().providersService != null)
      {
        if (favLabelsActivity.get().providersService.getFavoritesService() != null)
        {
          // Sauvegarde des labels
          favLabelsActivity.get().providersService.getFavoritesService().saveLabels();

          // Sauvegarde des favoris
          favLabelsActivity.get().providersService.getFavoritesService().saveBalisesFavorites();
        }
      }
    }
  }

  /**
   * 
   */
  private void initProvidersService()
  {
    // Initialisation service
    providersServiceConnection = new ProvidersServiceConnection(this);

    // Connexion au service
    final Intent providersServiceIntent = new Intent(getApplicationContext(), ProvidersService.class);
    bindService(providersServiceIntent, providersServiceConnection, Context.BIND_AUTO_CREATE);
  }

  /**
   * 
   * @param view
   */
  @SuppressWarnings("unused")
  public void onClickAdd(final View view)
  {
    // Ajout
    final EditText editText = (EditText)findViewById(R.id.fav_labels_addtext);
    final String label = editText.getText().toString();
    if (!Utils.isStringVide(label))
    {
      if (labelsAdapter.getPosition(label) < 0)
      {
        // Ajout
        providersService.getFavoritesService().addLabel(label);
        labelsAdapter.add(label);
        editText.setText(Strings.VIDE);
      }
      else
      {
        Toast.makeText(getApplicationContext(), resources.getString(R.string.message_label_already_exists, label), Toast.LENGTH_SHORT).show();
      }
    }
  }

  /**
   * 
   * @param label
   */
  void deleteLabelItem(final String label)
  {
    // Suppression
    providersService.getFavoritesService().removeLabel(label);
    labelsAdapter.remove(label);
  }

  /**
   * 
   * @param view
   */
  public void onClickDel(final View view)
  {
    // Initialisations
    final int clicked = ((Integer)view.getTag()).intValue();
    final String label = labelsAdapter.getItem(clicked);
    final List<BaliseFavorite> favorites = providersService.getFavoritesService().getBalisesForLabel(label);
    final int nbFavorites = (favorites == null ? 0 : favorites.size());

    if (nbFavorites == 0)
    {
      deleteLabelItem(label);
    }
    else
    {
      // Si au moins 1 favori => demande de confirmation
      final String title = resources.getString(R.string.message_delete_label_title);
      final String message = resources.getString(R.string.message_delete_label_message, label, Integer.valueOf(nbFavorites));
      ActivityCommons.confirmDialog(this, ActivityCommons.CONFIRM_DIALOG_FAV_LABEL_DELETE, title, message, new OnClickListener()
      {
        @Override
        public void onClick(final DialogInterface dialog, final int which)
        {
          deleteLabelItem(label);
        }
      }, null, true, null, -1, -1, null, null, null);
    }
  }

  /**
   * 
   * @param view
   */
  public void onClickText(final View view)
  {
    // Initialisations
    final LayoutInflater inflater = getLayoutInflater();
    final int clicked = ((Integer)view.getTag()).intValue();
    final View itemView = labelsAdapter.getView(clicked, null, null);
    final TextView textView = (TextView)itemView.findViewById(R.id.fav_label_item_text);
    final AlertDialog.Builder builder = new AlertDialog.Builder(this);

    // Builder
    builder.setCancelable(true);
    builder.setIcon(resources.getDrawable(R.drawable.icon));
    final EditText editText = (EditText)inflater.inflate(R.layout.favorite_label_item_edit_dialog, getListView(), false);
    final String originalText = textView.getText().toString();
    editText.setText(textView.getText());
    editText.setWidth(20000);
    builder.setView(editText);
    builder.setTitle(resources.getString(R.string.label_title_favorite_label_update));
    builder.setPositiveButton(resources.getString(R.string.button_ok), new OnClickListener()
    {
      @Override
      public void onClick(final DialogInterface dialog, final int which)
      {
        final String label = editText.getText().toString();
        if (!Utils.isStringVide(label) && (labelsAdapter.getPosition(label) < 0) && !label.equals(originalText))
        {
          labelsAdapter.remove(originalText);
          labelsAdapter.insert(label, clicked);
          textView.setText(label);
          providersService.getFavoritesService().renameLabel(originalText, label);
        }

        dialog.dismiss();
      }
    });
    builder.setNegativeButton(resources.getString(R.string.button_cancel), new OnClickListener()
    {
      @Override
      public void onClick(final DialogInterface dialog, final int which)
      {
        dialog.cancel();
      }
    });

    // Dialog
    final AlertDialog alertDialog = builder.create();
    alertDialog.show();
    ActivityCommons.registerAlertDialog(ActivityCommons.ALERT_DIALOG_FAV_LABEL_EDIT, alertDialog, null);
  }
}
