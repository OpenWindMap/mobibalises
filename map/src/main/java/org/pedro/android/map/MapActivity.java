package org.pedro.android.map;

import org.pedro.map.TileCache;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

/**
 * 
 * @author pedro.m
 */
public abstract class MapActivity extends Activity implements IMapActivity, TileCache.CacheNotAvailableListener
{
  // Mode graphique
  protected boolean graphicsLightMode;

  @Override
  protected void onCreate(final Bundle savedInstanceState)
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> MapActivity.onCreate");

    // Super
    super.onCreate(savedInstanceState);

    // Initialisation mapView
    setGraphicsMode();

    // Fin
    Log.d(getClass().getSimpleName(), "<<< MapActivity.onCreate");
  }

  @Override
  protected void onDestroy()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> MapActivity.onDestroy");

    // Super
    super.onDestroy();

    // Fermeture vue
    getMapView().shutdown();

    // Fin
    Log.d(getClass().getSimpleName(), "<<< MapActivity.onDestroy");
  }

  @Override
  public void setGraphicsMode()
  {
    // Reglage profondeur pixels
    final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
    layoutParams.copyFrom(getWindow().getAttributes());
    layoutParams.format = (isGraphicsLightMode() ? PixelFormat.RGBA_4444 : PixelFormat.RGBA_8888);
    getWindow().setAttributes(layoutParams);
  }
}
