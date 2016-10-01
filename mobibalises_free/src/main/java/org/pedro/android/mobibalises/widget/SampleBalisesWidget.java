package org.pedro.android.mobibalises.widget;

import org.pedro.android.mobibalises.R;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

/**
 * 
 * @author pedro.m
 */
public class SampleBalisesWidget extends AppWidgetProvider
{
  @Override
  public void onDisabled(final Context context)
  {
    // Nothing
  }

  @Override
  public void onEnabled(final Context context)
  {
    // Nothing
  }

  @Override
  public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds)
  {
    setOnClick(context, appWidgetIds);
  }

  /**
   * 
   * @param context
   */
  private static void setOnClick(final Context context, final int[] appWidgetIds)
  {
    final Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.parse("market://details?id=com.pedro.android.mobibalises"));
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

    for (final int appWidgetId : appWidgetIds)
    {
      final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.balises_widget_sample_layout);
      views.setOnClickPendingIntent(R.id.widget_image, pendingIntent);
      views.setOnClickPendingIntent(R.id.widget_text, pendingIntent);

      AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views);
    }
  }
}
