package org.pedro.map;

import org.pedro.map.tileprovider.BingAerialLabelsTileProvider;
import org.pedro.map.tileprovider.BingAerialTileProvider;
import org.pedro.map.tileprovider.BingRoadTileProvider;
import org.pedro.map.tileprovider.GoogleHybridTileProvider;
import org.pedro.map.tileprovider.GoogleRoadmapTileProvider;
import org.pedro.map.tileprovider.GoogleSatelliteTileProvider;
import org.pedro.map.tileprovider.GoogleTerrainTileProvider;
import org.pedro.map.tileprovider.HikeBikeEuropeTileProvider;
import org.pedro.map.tileprovider.HikingEuropeTileProvider;
import org.pedro.map.tileprovider.IGNMapTileProvider;
import org.pedro.map.tileprovider.IGNSatelliteTileProvider;
import org.pedro.map.tileprovider.MRIReliefTileProvider;
import org.pedro.map.tileprovider.MapQuestOSMTileProvider;
import org.pedro.map.tileprovider.MapQuestOpenAerialTileProvider;
import org.pedro.map.tileprovider.MapboxTileProvider;
import org.pedro.map.tileprovider.MapnikTileProvider;
import org.pedro.map.tileprovider.OpenCycleMapTileProvider;

/**
 * 
 * @author pedro.m
 */
public enum BuiltInTileProvider
{
  OSM_MAPNIK(MapnikTileProvider.KEY), OSM_OCM(OpenCycleMapTileProvider.KEY), IGN_SATELLITE(IGNSatelliteTileProvider.KEY), IGN_MAP(IGNMapTileProvider.KEY), GOOGLE_TERRAIN(GoogleTerrainTileProvider.KEY), GOOGLE_SATELLITE(
      GoogleSatelliteTileProvider.KEY), GOOGLE_ROADMAP(GoogleRoadmapTileProvider.KEY), GOOGLE_HYBRID(GoogleHybridTileProvider.KEY), HIKING_EUROPE(HikingEuropeTileProvider.KEY), MRI_RELIEF(MRIReliefTileProvider.KEY), HIKE_BIKE_EUROPE(
      HikeBikeEuropeTileProvider.KEY), MAPQUEST_OSM(MapQuestOSMTileProvider.KEY), MAPQUEST_OPEN_AERIAL(MapQuestOpenAerialTileProvider.KEY), MAPBOX(MapboxTileProvider.KEY), BING_ROAD(BingRoadTileProvider.KEY), BING_AERIAL(
      BingAerialTileProvider.KEY), BING_AERIAL_LABELS(BingAerialLabelsTileProvider.KEY), BLANK_LIGHT_GRAY("blank_light_gray"), BLANK_BLACK("blank_black"), CHECKERED_LIGHT_GRAY("checkered_light_gray"), CHECKERED_BLACK("checkered_black");

  private String key;

  /**
   * 
   * @param key
   */
  private BuiltInTileProvider(final String key)
  {
    this.key = key;
  }

  /**
   * @return the key
   */
  public String getKey()
  {
    return key;
  }

  /**
   * params[0] : cloudMadeApiKey
   * params[1] : cloudMadeToken
   * params[2] : bingApiKey
   * params[3] : googleApiKey
   * params[4] : ignApiKey
   * params[5] : debug
   * 
   * @param params
   * @return
   */
  public TileProvider getNewTileProvider(final Object... params)
  {
    // Debug ?
    final boolean debug = (params[5] == null ? false : ((Boolean)params[5]).booleanValue());

    // Selon la carte demandee
    switch (this)
    {
      case OSM_MAPNIK:
        return new MapnikTileProvider();
      case OSM_OCM:
        return new OpenCycleMapTileProvider();
        /*
        case OSM_TAH:
          return new TahOsmTileProvider();
        */
      case IGN_SATELLITE:
        return new IGNSatelliteTileProvider((String)params[4], debug);
      case IGN_MAP:
        return new IGNMapTileProvider((String)params[4], debug);
      case GOOGLE_HYBRID:
        return new GoogleHybridTileProvider((String)params[3]);
      case GOOGLE_ROADMAP:
        return new GoogleRoadmapTileProvider((String)params[3]);
      case GOOGLE_SATELLITE:
        return new GoogleSatelliteTileProvider((String)params[3]);
      case GOOGLE_TERRAIN:
        return new GoogleTerrainTileProvider((String)params[3]);
      case HIKING_EUROPE:
        return new HikingEuropeTileProvider();
      case MRI_RELIEF:
        return new MRIReliefTileProvider();
      case HIKE_BIKE_EUROPE:
        return new HikeBikeEuropeTileProvider();
        /*
        case CLOUDMADE_ORIGINAL:
          return new CloudMadeOriginalTileProvider((String)params[0], (String)params[1]);
        case CLOUDMADE_FINE_LINE:
          return new CloudMadeFineLineTileProvider((String)params[0], (String)params[1]);
        case CLOUDMADE_FRESH:
          return new CloudMadeFreshTileProvider((String)params[0], (String)params[1]);
        */
      case MAPQUEST_OSM:
        return new MapQuestOSMTileProvider();
      case MAPQUEST_OPEN_AERIAL:
        return new MapQuestOpenAerialTileProvider();
      case MAPBOX:
        return new MapboxTileProvider();
      case BING_ROAD:
        return new BingRoadTileProvider((String)params[2]);
      case BING_AERIAL:
        return new BingAerialTileProvider((String)params[2]);
      case BING_AERIAL_LABELS:
        return new BingAerialLabelsTileProvider((String)params[2]);
      default:
        return null;
    }
  }
}
