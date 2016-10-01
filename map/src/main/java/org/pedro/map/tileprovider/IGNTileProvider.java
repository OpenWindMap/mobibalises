package org.pedro.map.tileprovider;

import java.io.IOException;
import java.net.URL;

import org.pedro.map.Tile;

/**
 * 
 * @author pedro.m
 */
public abstract class IGNTileProvider extends NetworkTileProvider
{
  //private static final String USER_AGENT_DEBUG  = "Android";
  private static final String USER_AGENT_PROD   = "Mobibalises";
  private static final String REFERER           = "www.mobibalises.net";

  private static final String PARAM_BASEURL     = "baseUrl";
  private static final String PARAM_APIKEY      = "apikey";
  private static final String PARAM_LAYER       = "layer";                                // ORTHOIMAGERY.ORTHOPHOTOS
  private static final String PARAM_TILEMATRIX  = "tilematrix";                           // 18
  private static final String PARAM_TILEROW     = "tilerow";
  private static final String PARAM_TILECOL     = "tilecol";

  /*
  private static final String URL                = "http://gpp3-wxs.ign.fr/{" + PARAM_APIKEY + "}/geoportail/wmts?LAYER={" + PARAM_LAYER
                                                     + "}&EXCEPTIONS=text/xml&FORMAT=image/jpeg&SERVICE=WMTS&VERSION=1.0.0&REQUEST=GetTile&STYLE=normal&TILEMATRIXSET=PM&TILEMATRIX={" + PARAM_TILEMATRIX + "}&TILEROW={" + PARAM_TILEROW
                                                     + "}&TILECOL={" + PARAM_TILECOL + "}";
  */
  @SuppressWarnings("unused")
  private static final String BASEURL_DEBUG     = "gpp3-wxs.ign.fr";
  private static final String BASEURL_PROD      = "wxs.ign.fr";
  private static final String URL               = "http://{" + PARAM_BASEURL + "}/{" + PARAM_APIKEY + "}/geoportail/wmts?LAYER={" + PARAM_LAYER
                                                    + "}&EXCEPTIONS=text/xml&FORMAT=image/jpeg&SERVICE=WMTS&VERSION=1.0.0&REQUEST=GetTile&STYLE=normal&TILEMATRIXSET=PM&TILEMATRIX={" + PARAM_TILEMATRIX + "}&TILEROW={" + PARAM_TILEROW
                                                    + "}&TILECOL={" + PARAM_TILECOL + "}";

  private static final String REGEXP_BASEURL    = "\\{" + PARAM_BASEURL + "\\}";
  private static final String REGEXP_APIKEY     = "\\{" + PARAM_APIKEY + "\\}";
  private static final String REGEXP_LAYER      = "\\{" + PARAM_LAYER + "\\}";
  private static final String REGEXP_TILEMATRIX = "\\{" + PARAM_TILEMATRIX + "\\}";
  private static final String REGEXP_TILEROW    = "\\{" + PARAM_TILEROW + "\\}";
  private static final String REGEXP_TILECOL    = "\\{" + PARAM_TILECOL + "\\}";

  @SuppressWarnings("unused")
  private final boolean       debug;
  private final String        baseUrl;

  /**
   * 
   * @param key
   * @param apiKey
   * @param debug
   */
  public IGNTileProvider(final String key, final String apiKey, final boolean debug)
  {
    super(key);
    this.debug = debug;
    //this.baseUrl = URL.replaceAll(REGEXP_BASEURL, (debug ? BASEURL_DEBUG : BASEURL_PROD)).replaceAll(REGEXP_APIKEY, apiKey);
    this.baseUrl = URL.replaceAll(REGEXP_BASEURL, BASEURL_PROD).replaceAll(REGEXP_APIKEY, apiKey);
  }

  @Override
  public final int getMinZoomLevel()
  {
    return 0;
  }

  @Override
  public final URL getTileURL(final Tile tile) throws IOException
  {
    // URL de base
    String urlString = baseUrl;
    urlString = urlString.replaceAll(REGEXP_LAYER, getLayer());
    urlString = urlString.replaceAll(REGEXP_TILEMATRIX, Integer.toString(tile.zoom, 10));
    urlString = urlString.replaceAll(REGEXP_TILEROW, Long.toString(tile.y, 10));
    urlString = urlString.replaceAll(REGEXP_TILECOL, Long.toString(tile.x, 10));

    return new URL(urlString);
  }

  /**
   * 
   * @return
   */
  public abstract String getLayer();

  @Override
  public String getUserAgent()
  {
    //return debug ? USER_AGENT_DEBUG : USER_AGENT_PROD;
    return null; //TODO ? USER_AGENT_PROD;
  }

  @Override
  public String getReferer()
  {
    return REFERER;
  }
}
