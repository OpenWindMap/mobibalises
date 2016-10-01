package com.pedro.android.mobibalises.preferences;

import com.pedro.android.mobibalises.R;

/**
 * 
 * @author pedro.m
 */
public class BalisesWidget33PreferencesActivity extends AbstractBalisesWidgetPreferencesActivity
{
  @Override
  public int getWidgetType()
  {
    return WIDGET_TYPE_33;
  }

  @Override
  protected int getDefautListValue()
  {
    return R.string.config_widget_label_value_around;
  }
}
