package org.pedro.android.mobibalises_common.search;

import java.util.List;

import org.pedro.android.mobibalises_common.R;
import org.pedro.android.mobibalises_common.Strings;
import org.pedro.android.mobibalises_common.map.AbstractBalisesMapActivity;
import org.pedro.android.mobibalises_common.search.SearchDatabaseHelper.SearchItem;
import org.pedro.android.mobibalises_common.webcam.WebcamDatabaseHelper;
import org.pedro.spots.TypeSpot;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * 
 * @author pedro.m
 */
public abstract class AbstractSearchActivity extends ListActivity
{
  private static String lastQuery = null;

  SearchDatabaseHelper  helper;
  WebcamDatabaseHelper  webcamHelper;
  SearchAdapter         adapter;

  /**
   * 
   * @author pedro.m
   */
  private static class SearchAdapter extends ArrayAdapter<SearchItem>
  {
    private final LayoutInflater inflater;

    /**
     * 
     * @param context
     */
    public SearchAdapter(final Context context)
    {
      super(context, android.R.layout.simple_list_item_1);
      inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    /**
     * 
     * @param data
     */
    public void setData(final List<SearchItem> data)
    {
      clear();
      if (data != null)
      {
        // Utilisable seulement a partir de l'API 11 : addAll(data);
        for (final SearchItem item : data)
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
        view = inflater.inflate(R.layout.search_list_item, parent, false);
      }
      else
      {
        view = convertView;
      }

      // Item selectionne
      final SearchItem item = getItem(position);

      // Icone et provider
      final String provider;
      final int iconId;

      // Balise
      if (SearchDatabaseHelper.ITEM_TYPE_BALISE.equals(item.type))
      {
        provider = item.provider;
        iconId = R.drawable.icon_search_balise;
      }

      // Spot
      else if (SearchDatabaseHelper.ITEM_TYPE_SPOT.equals(item.type))
      {
        provider = item.provider;
        // Atterro
        if (TypeSpot.ATTERRISSAGE.getKey().equals(item.subtype))
        {
          iconId = R.drawable.icon_search_spot_atterro;
        }
        // Deco
        else if (TypeSpot.DECOLLAGE.getKey().equals(item.subtype))
        {
          iconId = R.drawable.icon_search_spot_deco;
        }
        //Spot
        else
        {
          iconId = R.drawable.icon_search_spot;
        }
      }

      // Webcam
      else if (SearchDatabaseHelper.ITEM_TYPE_WEBCAM.equals(item.type))
      {
        provider = Strings.VIDE;
        iconId = R.drawable.icon_search_webcam;
      }

      // Type inconnu
      else
      {
        provider = item.provider;
        iconId = R.drawable.ic_menu_help;
      }

      // MAJ de la vue
      ((ImageView)view.findViewById(R.id.search_item_icon)).setImageDrawable(getContext().getResources().getDrawable(iconId));
      final TextView providerView = (TextView)view.findViewById(R.id.search_item_provider);
      providerView.setText(provider);
      providerView.setTag(R.id.tag_search_type, Integer.valueOf(item.type));
      providerView.setTag(R.id.tag_search_provider, item.provider);
      providerView.setTag(R.id.tag_search_id, item.id);
      providerView.setTag(R.id.tag_search_latitude, Double.valueOf(item.latitude));
      providerView.setTag(R.id.tag_search_longitude, Double.valueOf(item.longitude));
      ((TextView)view.findViewById(R.id.search_item_name)).setText(item.name);

      return view;
    }
  }

  @Override
  public void onCreate(final Bundle savedInstanceState)
  {
    // Super
    Log.d(getClass().getSimpleName(), ">>> onCreate()");
    super.onCreate(savedInstanceState);

    // Vue custom
    setContentView(R.layout.search);

    // Adapter
    adapter = new SearchAdapter(this);
    setListAdapter(adapter);

    // Init
    helper = new SearchDatabaseHelper(getApplicationContext());
    webcamHelper = WebcamDatabaseHelper.newInstance(getApplicationContext());

    // Intent
    final Intent intent = getIntent();
    if (Intent.ACTION_SEARCH.equals(intent.getAction()))
    {
      final String query = intent.getStringExtra(SearchManager.QUERY).trim();
      Log.d(getClass().getSimpleName(), "query : " + query);
      doSearch(query);
    }

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onCreate()");
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

  /**
   * 
   * @param query
   */
  private void doSearch(final String query)
  {
    // Sauvegarde du motif
    lastQuery = query;

    // Recherche
    final AsyncTask<Void, Void, List<SearchItem>> task = new AsyncTask<Void, Void, List<SearchItem>>()
    {
      @Override
      protected List<SearchItem> doInBackground(final Void... params)
      {
        // Dans la base de données de recherche pour les balises et les spots
        final SQLiteDatabase database = helper.getReadableDatabase();
        List<SearchItem> items;
        try
        {
          items = helper.searchItems(database, query);
        }
        finally
        {
          database.close();
        }

        // Dans la base de données spécifique pour les webcams
        final SQLiteDatabase webcamDatabase = webcamHelper.getReadableDatabase();
        try
        {
          final List<SearchItem> webcams = webcamHelper.searchItems(webcamDatabase, query);
          items.addAll(webcams);
        }
        finally
        {
          webcamDatabase.close();
        }
        return items;
      }

      @Override
      protected void onPostExecute(final List<SearchItem> items)
      {
        ((TextView)getListView().getEmptyView()).setText(R.string.message_search_no_result);
        adapter.setData(items);
      }
    };
    task.execute();
  }

  /**
   * 
   * @param view
   */
  public void onItemClick(final View view)
  {
    // Couleur de fond de l'item (en rouge que ça se voit bien !)
    view.setBackgroundColor(Color.RED);

    // Recuperation des infos de l'item clique
    final View providerView = view.findViewById(R.id.search_item_provider);
    final Integer type = (Integer)providerView.getTag(R.id.tag_search_type);
    final String provider = (String)providerView.getTag(R.id.tag_search_provider);
    final String id = (String)providerView.getTag(R.id.tag_search_id);
    final Double latitude = (Double)providerView.getTag(R.id.tag_search_latitude);
    final Double longitude = (Double)providerView.getTag(R.id.tag_search_longitude);

    // Intent de retour
    final Intent intent = new Intent(AbstractBalisesMapActivity.ACTION_ITEM);
    intent.putExtra(AbstractBalisesMapActivity.ACTION_ITEM_TYPE, type.intValue());
    intent.putExtra(AbstractBalisesMapActivity.ACTION_ITEM_PROVIDER, provider);
    intent.putExtra(AbstractBalisesMapActivity.ACTION_ITEM_ID, id);
    intent.putExtra(AbstractBalisesMapActivity.ACTION_ITEM_LATITUDE, latitude);
    intent.putExtra(AbstractBalisesMapActivity.ACTION_ITEM_LONGITUDE, longitude);
    sendBroadcast(intent);

    // Fin
    finish();
  }

  /**
   * 
   * @return
   */
  public static String getLastQuery()
  {
    return lastQuery;
  }

  /**
   * 
   * @param inLastQuery
   */
  public static void setLastQuery(final String inLastQuery)
  {
    lastQuery = inLastQuery;
  }
}
