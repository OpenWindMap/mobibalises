package com.pedro.android.mobibalises.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.view.View;
import android.widget.RemoteViews;

import com.pedro.android.mobibalises.R;
import com.pedro.android.mobibalises.preferences.BalisesWidget33PreferencesActivity;

/**
 * 
 * @author pedro.m
 */
public class BalisesWidget33 extends BalisesWidgets
{
  private static final String WIDGET_TYPE_NAME = "3x3";

  /**
   * 
   * @param context
   * @param resources
   * @param sharedPreferences
   * @param appWidgetId
   * @param views
   */
  protected static void manageHeader(final Context context, final Resources resources, final SharedPreferences sharedPreferences, final int appWidgetId, final RemoteViews views)
  {
    // Header
    manageStandardHeader(context, resources, sharedPreferences, appWidgetId, views, BalisesWidget33PreferencesActivity.class);

    // La visibilite de la rose des vents
    views.setViewVisibility(R.id.widget_background_image, (BalisesWidgets.isAroundWidget(appWidgetId, resources, sharedPreferences) ? View.VISIBLE : View.INVISIBLE));
  }

  @Override
  protected String getWidgetTypeName()
  {
    return WIDGET_TYPE_NAME;
  }
}
