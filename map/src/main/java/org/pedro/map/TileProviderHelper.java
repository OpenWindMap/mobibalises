package org.pedro.map;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.pedro.map.tileprovider.CustomTileProvider;
import org.pedro.map.tileprovider.StackCompositeTileProvider;
import org.pedro.map.tileprovider.ZoomCompositeTileProvider;

/**
 * 
 * @author pedro.m
 */
public class TileProviderHelper
{
  private static final String                          NAME_HIDDEN                = "hidden";

  private static final String                          TYPE_LOCAL                 = "local";
  private static final String                          TYPE_NETWORK               = "network";
  private static final String                          TYPE_ZOOM_COMPOSITE        = "zoom-composite";
  private static final String                          TYPE_STACK_COMPOSITE       = "stack-composite";
  private static final String                          TYPE_MBTILES               = "mbtiles";
  private static final String                          TYPE_MBTILES_DIR           = "mbtilesdir";

  private static final String                          STRING_COMPONENT_SEPARATOR = ":";
  private static final String                          STRING_CONFIG_SEPARATOR    = "\\|";

  private final List<TileProviderConfiguration>        configurationList          = new ArrayList<TileProviderConfiguration>();
  private final List<TileProviderConfiguration>        visibleConfigurationList   = new ArrayList<TileProviderConfiguration>();
  private final Map<String, TileProviderConfiguration> configurations             = new HashMap<String, TileProviderConfiguration>();
  private final Map<String, TileProvider>              providers                  = new HashMap<String, TileProvider>();

  /**
   * 
   * @author pedro.m
   */
  private static class CustomTileProviderConfiguration extends TileProviderConfiguration
  {
    private final int     minZoom;
    private final int     maxZoom;
    private final boolean needsCache;
    private final String  tileMask;

    /**
     * 
     * @param key
     * @param params
     */
    CustomTileProviderConfiguration(final String key, final String[] params)
    {
      this.key = key;
      this.name = params[0];
      this.needsCache = TYPE_NETWORK.equalsIgnoreCase(params[1]);
      this.minZoom = Integer.parseInt(params[2], 10);
      this.maxZoom = Integer.parseInt(params[3], 10);
      this.tileMask = params[4];
    }

    @Override
    protected TileProvider getNewTileProvider(final Object... params)
    {
      return new CustomTileProvider(key, minZoom, maxZoom, tileMask, needsCache);
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class MBTilesTileProviderConfiguration extends TileProviderConfiguration
  {
    private final String filename;

    /**
     * 
     * @param key
     * @param params
     */
    MBTilesTileProviderConfiguration(final String key, final String[] params)
    {
      this.key = key;
      this.name = params[0];
      this.filename = params[2];
    }

    @Override
    protected TileProvider getNewTileProvider(final Object... params)
    {
      try
      {
        // Par introspection (car n'est implemente que pour android)
        final Class<?> classe = Class.forName("org.pedro.android.map.tileprovider.MBTilesTileProvider");
        final Constructor<?> constructeur = classe.getConstructor(String.class, String.class);

        return (TileProvider)constructeur.newInstance(key, filename);
      }
      catch (final Throwable th)
      {
        th.printStackTrace(System.out);
        return null;
      }
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class MBTilesDirectoryTileProviderConfiguration extends TileProviderConfiguration
  {
    private final String dirname;

    /**
     * 
     * @param key
     * @param params
     */
    MBTilesDirectoryTileProviderConfiguration(final String key, final String[] params)
    {
      this.key = key;
      this.name = params[0];
      this.dirname = params[2];
    }

    @Override
    protected TileProvider getNewTileProvider(final Object... params)
    {
      try
      {
        // Par introspection (car n'est implemente que pour android)
        final Class<?> classe = Class.forName("org.pedro.android.map.tileprovider.MBTilesDirectoryTileProvider");
        final Constructor<?> constructeur = classe.getConstructor(String.class, String.class);

        return (TileProvider)constructeur.newInstance(key, dirname);
      }
      catch (final Throwable th)
      {
        th.printStackTrace(System.out);
        return null;
      }
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class ZoomComponentTileProviderConfiguration
  {
    final String key;
    final int    beginZoom;
    final int    endZoom;

    /**
     * 
     * @param params
     */
    ZoomComponentTileProviderConfiguration(final String[] params)
    {
      this.key = params[0];
      this.beginZoom = Integer.parseInt(params[1], 10);
      this.endZoom = Integer.parseInt(params[2], 10);
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class ZoomCompositeTileProviderConfiguration extends TileProviderConfiguration
  {
    private TileProviderHelper                                 tileProviderHelper;
    private final List<ZoomComponentTileProviderConfiguration> components = new ArrayList<ZoomComponentTileProviderConfiguration>();

    /**
     *
     * @param tileProviderHelper
     * @param key
     * @param params
     */
    ZoomCompositeTileProviderConfiguration(final TileProviderHelper tileProviderHelper, final String key, final String[] params)
    {
      this.tileProviderHelper = tileProviderHelper;
      this.key = key;
      this.name = params[0];

      // Pour chaque composant
      for (int i = 2; i < params.length; i++)
      {
        final String[] componentParams = params[i].split(STRING_COMPONENT_SEPARATOR);
        final ZoomComponentTileProviderConfiguration componentConfiguration = new ZoomComponentTileProviderConfiguration(componentParams);
        components.add(componentConfiguration);
      }
    }

    @Override
    protected TileProvider getNewTileProvider(final Object... params)
    {
      // Composant composite
      final ZoomCompositeTileProvider tileProvider = new ZoomCompositeTileProvider(key);

      // Pour chaque composant
      for (final ZoomComponentTileProviderConfiguration componentConfig : components)
      {
        // Recuperation du provider (dans le cache ou creation)
        tileProvider.addTileProvider(componentConfig.beginZoom, componentConfig.endZoom, tileProviderHelper.getTileProvider(componentConfig.key, params));
      }

      return tileProvider;
    }

    @Override
    protected void onShutdown()
    {
      // Super
      super.onShutdown();

      // Divers
      tileProviderHelper = null;
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class StackComponentTileProviderConfiguration
  {
    final String key;

    /**
     * 
     * @param params
     */
    StackComponentTileProviderConfiguration(final String key)
    {
      this.key = key;
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class StackCompositeTileProviderConfiguration extends TileProviderConfiguration
  {
    private TileProviderHelper                                  tileProviderHelper;
    private final List<StackComponentTileProviderConfiguration> components = new ArrayList<StackComponentTileProviderConfiguration>();

    /**
     *
     * @param tileProviderHelper
     * @param key
     * @param params
     */
    StackCompositeTileProviderConfiguration(final TileProviderHelper tileProviderHelper, final String key, final String[] params)
    {
      this.tileProviderHelper = tileProviderHelper;
      this.key = key;
      this.name = params[0];

      // Pour chaque composant
      for (int i = 2; i < params.length; i++)
      {
        final StackComponentTileProviderConfiguration componentConfiguration = new StackComponentTileProviderConfiguration(params[i]);
        components.add(componentConfiguration);
      }
    }

    @Override
    protected TileProvider getNewTileProvider(final Object... params)
    {
      // Composant composite
      final StackCompositeTileProvider tileProvider = new StackCompositeTileProvider(key);

      // Pour chaque composant
      for (final StackComponentTileProviderConfiguration componentConfig : components)
      {
        // Recuperation du provider (dans le cache ou creation)
        tileProvider.addTileProvider(tileProviderHelper.getTileProvider(componentConfig.key, params));
      }

      return tileProvider;
    }

    @Override
    protected void onShutdown()
    {
      // Super
      super.onShutdown();

      // Divers
      tileProviderHelper = null;
    }
  }

  /**
   * 
   * @param url
   * @return
   * @throws IOException
   */
  private static Map<String, String> readProperties(final URL url) throws IOException
  {
    // Initialisations
    final Map<String, String> properties = new LinkedHashMap<String, String>();
    BufferedReader br = null;
    InputStreamReader isr = null;

    try
    {
      // Ouvertures
      isr = new InputStreamReader(url.openStream());
      br = new BufferedReader(isr);

      // Lecture
      String line = br.readLine();
      while (line != null)
      {
        final String trimed = line.trim();
        if (trimed.length() == 0)
        {
          // Ligne vide
        }
        else if (trimed.startsWith("#"))
        {
          // Commentaire
        }
        else
        {
          // Ligne pleine
          final int egal = trimed.indexOf('=');
          if (egal > 0)
          {
            // Ligne valide
            properties.put(trimed.substring(0, egal).trim(), trimed.substring(egal + 1).trim());
          }
        }

        // Next
        line = br.readLine();
      }
    }
    finally
    {
      if (br != null)
      {
        br.close();
      }
    }

    return properties;
  }

  /**
   * 
   * @param os
   * @param properties
   * @throws IOException
   */
  public static void saveConfiguration(final OutputStream os, final Map<String, String> properties) throws IOException
  {
    // Initialisations
    BufferedWriter bw = null;
    OutputStreamWriter osw = null;

    try
    {
      // Ouvertures
      osw = new OutputStreamWriter(os);
      bw = new BufferedWriter(osw);

      // Ecriture
      for (final String key : properties.keySet())
      {
        bw.write(key);
        bw.write('=');
        bw.write(properties.get(key));
        bw.write('\n');
      }
    }
    finally
    {
      // Fermetures
      if (bw != null)
      {
        bw.close();
      }
      if (osw != null)
      {
        osw.close();
      }
      os.close();
    }
  }

  /**
   * 
   * @param url
   * @throws IOException
   */
  public Map<String, String> readConfiguration(final URL url) throws IOException
  {
    // Initialisation
    configurations.clear();
    configurationList.clear();
    visibleConfigurationList.clear();

    // Lecture
    final Map<String, String> properties = readProperties(url);

    // Pour chaque ligne
    for (final String key : properties.keySet())
    {
      // Initialisations
      final String value = properties.get(key);
      final String[] params = value.split(STRING_CONFIG_SEPARATOR);
      final String name = params[0];

      // Selon le type
      final TileProviderConfiguration configuration;
      if (TYPE_LOCAL.equalsIgnoreCase(params[1]) || TYPE_NETWORK.equalsIgnoreCase(params[1]))
      {
        // Local ou network
        configuration = new CustomTileProviderConfiguration(key, params);
      }
      else if (TYPE_ZOOM_COMPOSITE.equalsIgnoreCase(params[1]))
      {
        // Composite
        configuration = new ZoomCompositeTileProviderConfiguration(this, key, params);
      }
      else if (TYPE_STACK_COMPOSITE.equalsIgnoreCase(params[1]))
      {
        // Composite
        configuration = new StackCompositeTileProviderConfiguration(this, key, params);
      }
      else if (TYPE_MBTILES.equalsIgnoreCase(params[1]))
      {
        // Composite
        configuration = new MBTilesTileProviderConfiguration(key, params);
      }
      else if (TYPE_MBTILES_DIR.equalsIgnoreCase(params[1]))
      {
        // Composite
        configuration = new MBTilesDirectoryTileProviderConfiguration(key, params);
      }
      else
      {
        // Inconnu
        throw new RuntimeException("Type de provider inconnu : " + params[1]);
      }

      // Enregistrement
      configurations.put(key, configuration);
      configurationList.add(configuration);
      if (!NAME_HIDDEN.equalsIgnoreCase(name))
      {
        visibleConfigurationList.add(configuration);
      }
    }

    return properties;
  }

  /**
   * 
   * @param key
   * @param params
   * @return
   */
  public TileProvider getTileProvider(final String key, final Object... params)
  {
    // Dans le cache
    TileProvider provider = providers.get(key);
    if (provider != null)
    {
      return provider;
    }

    // Creation
    provider = getNewTileProvider(key, params);

    // Sauvegarde
    providers.put(key, provider);

    // Fin
    return provider;
  }

  /**
   * 
   * @param key
   * @param params
   * @return
   */
  private TileProvider getNewTileProvider(final String key, final Object... params)
  {
    // Providers standard
    // Bing aerial
    if (BuiltInTileProvider.BING_AERIAL.getKey().equals(key))
    {
      return BuiltInTileProvider.BING_AERIAL.getNewTileProvider(params);
    }

    // Bing aerial lables
    else if (BuiltInTileProvider.BING_AERIAL_LABELS.getKey().equals(key))
    {
      return BuiltInTileProvider.BING_AERIAL_LABELS.getNewTileProvider(params);
    }

    // Bing road
    else if (BuiltInTileProvider.BING_ROAD.getKey().equals(key))
    {
      return BuiltInTileProvider.BING_ROAD.getNewTileProvider(params);
    }

    /*
    // Cloudmade fine
    else if (BuiltInTileProvider.CLOUDMADE_FINE_LINE.getKey().equals(key))
    {
      return BuiltInTileProvider.CLOUDMADE_FINE_LINE.getNewTileProvider(params);
    }

    // Cloudmade fresh
    else if (BuiltInTileProvider.CLOUDMADE_FRESH.getKey().equals(key))
    {
      return BuiltInTileProvider.CLOUDMADE_FRESH.getNewTileProvider(params);
    }

    // Cloudmade original
    else if (BuiltInTileProvider.CLOUDMADE_ORIGINAL.getKey().equals(key))
    {
      return BuiltInTileProvider.CLOUDMADE_ORIGINAL.getNewTileProvider(params);
    }
    */

    // MapQuest OSM
    else if (BuiltInTileProvider.MAPQUEST_OSM.getKey().equals(key))
    {
      return BuiltInTileProvider.MAPQUEST_OSM.getNewTileProvider(params);
    }

    // MapQuest Open Aerial
    else if (BuiltInTileProvider.MAPQUEST_OPEN_AERIAL.getKey().equals(key))
    {
      return BuiltInTileProvider.MAPQUEST_OPEN_AERIAL.getNewTileProvider(params);
    }

    // Mapbox
    else if (BuiltInTileProvider.MAPBOX.getKey().equals(key))
    {
      return BuiltInTileProvider.MAPBOX.getNewTileProvider(params);
    }

    // Google hybrid
    else if (BuiltInTileProvider.GOOGLE_HYBRID.getKey().equals(key))
    {
      return BuiltInTileProvider.GOOGLE_HYBRID.getNewTileProvider(params);
    }

    // Google roadmap
    else if (BuiltInTileProvider.GOOGLE_ROADMAP.getKey().equals(key))
    {
      return BuiltInTileProvider.GOOGLE_ROADMAP.getNewTileProvider(params);
    }

    // Google satellite
    else if (BuiltInTileProvider.GOOGLE_SATELLITE.getKey().equals(key))
    {
      return BuiltInTileProvider.GOOGLE_SATELLITE.getNewTileProvider(params);
    }

    // Google terrain
    else if (BuiltInTileProvider.GOOGLE_TERRAIN.getKey().equals(key))
    {
      return BuiltInTileProvider.GOOGLE_TERRAIN.getNewTileProvider(params);
    }

    // Hike & bike europe
    else if (BuiltInTileProvider.HIKE_BIKE_EUROPE.getKey().equals(key))
    {
      return BuiltInTileProvider.HIKE_BIKE_EUROPE.getNewTileProvider(params);
    }

    // Hiking Europe
    else if (BuiltInTileProvider.HIKING_EUROPE.getKey().equals(key))
    {
      return BuiltInTileProvider.HIKING_EUROPE.getNewTileProvider(params);
    }

    // MRI Relief
    else if (BuiltInTileProvider.MRI_RELIEF.getKey().equals(key))
    {
      return BuiltInTileProvider.MRI_RELIEF.getNewTileProvider(params);
    }

    // OSM Mapnik
    else if (BuiltInTileProvider.OSM_MAPNIK.getKey().equals(key))
    {
      return BuiltInTileProvider.OSM_MAPNIK.getNewTileProvider(params);
    }

    // OSM OCM
    else if (BuiltInTileProvider.OSM_OCM.getKey().equals(key))
    {
      return BuiltInTileProvider.OSM_OCM.getNewTileProvider(params);
    }

    // IGN Satellite
    else if (BuiltInTileProvider.IGN_SATELLITE.getKey().equals(key))
    {
      return BuiltInTileProvider.IGN_SATELLITE.getNewTileProvider(params);
    }

    // IGN Carte
    else if (BuiltInTileProvider.IGN_MAP.getKey().equals(key))
    {
      return BuiltInTileProvider.IGN_MAP.getNewTileProvider(params);
    }

    // Configuration Custom
    else
    {
      final TileProviderConfiguration configuration = configurations.get(key);
      if (configuration != null)
      {
        return configuration.getNewTileProvider(params);
      }
    }

    // Inconnu
    return null;
  }

  /**
   * 
   * @param index
   * @return
   */
  public String getCustomTileProviderName(final int index)
  {
    return visibleConfigurationList.get(index).name;
  }

  /**
   * 
   * @param index
   * @return
   */
  public String getCustomTileProviderKey(final int index)
  {
    return visibleConfigurationList.get(index).key;
  }

  /**
   * 
   * @return
   */
  public int getCustomTileProvidersCount()
  {
    return visibleConfigurationList.size();
  }

  /**
   * 
   */
  public void shutdown()
  {
    // Arrets
    for (final TileProvider provider : providers.values())
    {
      if (provider != null)
      {
        provider.shutdown();
      }
    }

    // Liberation des configurations
    for (final TileProviderConfiguration config : configurationList)
    {
      config.onShutdown();
    }

    // Liberation
    configurationList.clear();
    visibleConfigurationList.clear();
    configurations.clear();
    providers.clear();
  }

  /**
   * 
   * @param args
   */
  /*
  public static void main(final String[] args)
  {
    try
    {
      final TileProviderHelper tileProviderHelper = new TileProviderHelper();
      tileProviderHelper.readConfiguration(new URL("file:/D:/_workspaces/perso/mobibalises/mobibalises_full/divers/custom_maps.properties"));
      TileProvider provider = tileProviderHelper.getNewTileProvider("gg_terrain_sat", (Object[])null);
      System.out.println("provider : " + provider);
    }
    catch (IOException ioe)
    {
      ioe.printStackTrace();
    }
  }
  */
}
