package org.pedro.android.mobibalises_common.view;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.pedro.android.map.MapView;
import org.pedro.android.mobibalises_common.R;
import org.pedro.balises.BaliseProvider;

import android.content.Context;
import android.content.res.Resources;

/**
 * 
 * @author pedro.m
 */
public final class BaliseDrawableHelper
{
  private static Constructor<? extends BaliseDrawable> constructor;

  /**
   * 
   * @param resources
   */
  @SuppressWarnings("unchecked")
  public static void initialize(final Context context)
  {
    // Initialisations
    final Resources resources = context.getResources();

    try
    {
      // Nom de la classe
      final String className = resources.getString(R.string.balise_drawable_class);

      // Classe
      final Class<? extends BaliseDrawable> baliseDrawableClass = (Class<? extends BaliseDrawable>)Class.forName(className);

      // Constructeur
      constructor = baliseDrawableClass.getDeclaredConstructor(String.class, BaliseProvider.class, MapView.class, String.class);
    }
    catch (final NoSuchMethodException nsme)
    {
      throw new RuntimeException(nsme);
    }
    catch (final ClassNotFoundException cnfe)
    {
      throw new RuntimeException(cnfe);
    }
  }

  /**
   * 
   * @param context
   * @param idBalise
   * @param provider
   * @param mapView
   * @return
   */
  public static BaliseDrawable newBaliseDrawable(final Context context, final String idBalise, final BaliseProvider provider, final MapView mapView)
  {
    try
    {
      final String touchableTitle = context.getResources().getString(R.string.menu_context_map_historique_balise);
      return constructor.newInstance(idBalise, provider, mapView, touchableTitle);
    }
    catch (final InstantiationException ie)
    {
      throw new RuntimeException(ie);
    }
    catch (final IllegalAccessException iae)
    {
      throw new RuntimeException(iae);
    }
    catch (final InvocationTargetException ite)
    {
      throw new RuntimeException(ite);
    }
  }
}
