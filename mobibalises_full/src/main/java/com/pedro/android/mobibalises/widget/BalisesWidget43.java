package com.pedro.android.mobibalises.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.widget.RemoteViews;

import com.pedro.android.mobibalises.preferences.BalisesWidget43PreferencesActivity;

/**
 * 
 * @author pedro.m
 */
public class BalisesWidget43 extends BalisesWidgets
{
  private static final String WIDGET_TYPE_NAME = "4x3";

  /**
   * 
   * @param context
   * @param resources
   * @param sharedPreferences
   * @param appWidgetId
   * @param views
   */
  protected static void manageTextviews(final Context context, final Resources resources, final SharedPreferences sharedPreferences, final int appWidgetId, final RemoteViews views)
  {
    manageStandardHeader(context, resources, sharedPreferences, appWidgetId, views, BalisesWidget43PreferencesActivity.class);
  }

  @Override
  protected String getWidgetTypeName()
  {
    return WIDGET_TYPE_NAME;
  }
}
