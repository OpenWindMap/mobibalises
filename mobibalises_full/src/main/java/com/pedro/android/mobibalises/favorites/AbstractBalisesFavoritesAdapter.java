package com.pedro.android.mobibalises.favorites;

import java.util.ArrayList;
import java.util.List;

import org.pedro.android.mobibalises_common.ActivityCommons;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import com.pedro.android.mobibalises.R;

/**
 * 
 * @author pedro.m
 */
public abstract class AbstractBalisesFavoritesAdapter extends ArrayAdapter<BaliseFavorite>
{
  protected List<BaliseFavorite>       currentNonFilteredBalisesFavorites;
  protected final List<BaliseFavorite> filteredBalisesFavorites = new ArrayList<BaliseFavorite>();
  protected final Filter               filter;
  protected final int                  itemId;
  protected final Resources            resources;
  protected final SharedPreferences    sharedPreferences;

  /**
   * 
   * @param context
   * @param itemId
   */
  public AbstractBalisesFavoritesAdapter(final Context context, final int itemId)
  {
    super(context, itemId);
    this.itemId = itemId;
    resources = context.getResources();
    sharedPreferences = ActivityCommons.getSharedPreferences(context);

    // Creation du filtre
    filter = new Filter()
    {
      @Override
      protected FilterResults performFiltering(final CharSequence constraint)
      {
        try
        {
          // Initialisations
          filteredBalisesFavorites.clear();
          if (currentNonFilteredBalisesFavorites != null)
          {
            // Preferences
            final boolean displayInactive = !sharedPreferences.getBoolean(resources.getString(R.string.config_map_balise_hide_inactive_key), Boolean.parseBoolean(resources.getString(R.string.config_map_balise_hide_inactive_default)));

            // Pour chaque balise
            for (final BaliseFavorite favorite : currentNonFilteredBalisesFavorites)
            {
              if (displayInactive || favorite.isDrawable())
              {
                if ((constraint == null) || ((favorite.getName() != null) && favorite.getName().toUpperCase().contains(constraint.toString().toUpperCase())))
                {
                  synchronized (filteredBalisesFavorites)
                  {
                    filteredBalisesFavorites.add(favorite);
                  }
                }
              }
            }
          }

          final FilterResults results = new FilterResults();
          results.count = filteredBalisesFavorites.size();
          results.values = filteredBalisesFavorites;

          return results;
        }
        catch (final Throwable th)
        {
          th.printStackTrace(System.err);
          throw new RuntimeException(th);
        }
      }

      @Override
      @SuppressWarnings("unchecked")
      protected void publishResults(final CharSequence constraint, final FilterResults results)
      {
        // Initialisations
        final List<BaliseFavorite> favorites = (results == null ? null : (List<BaliseFavorite>)results.values);

        // Changement du message de liste vide
        final boolean proximityMode = ((FavoritesActivity)context).isProximityMode();
        final TextView emptyTextview = (TextView)((FavoritesActivity)context).findViewById(R.id.favorites_empty_textview);
        emptyTextview.setText(proximityMode ? R.string.message_empty_location : R.string.message_no_favorites);
        final View emptyFooterButton = ((FavoritesActivity)context).findViewById(R.id.favorites_empty_footer_button);
        emptyFooterButton.setVisibility(proximityMode ? View.GONE : View.VISIBLE);

        // Publication
        if (favorites != null)
        {
          AbstractBalisesFavoritesAdapter.this.publishResults(favorites);
        }
      }
    };
  }

  @Override
  public final Filter getFilter()
  {
    return filter;
  }

  /**
   * 
   * @param currentNonFilteredBalisesFavorites 
   */
  public void setCurrentNonFilteredBalisesFavorites(final List<BaliseFavorite> currentNonFilteredBalisesFavorites)
  {
    this.currentNonFilteredBalisesFavorites = currentNonFilteredBalisesFavorites;
  }

  /**
   * 
   * @param results
   */
  public abstract void publishResults(final List<BaliseFavorite> results);
}
