package org.pedro.map.tileprovider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.UUID;

import org.pedro.map.MercatorProjection;
import org.pedro.map.Tile;

/**
 * 
 * @author pedro.m
 */
@Deprecated
public abstract class CloudMadeTileProvider extends NetworkTileProvider
{
  private static final String UTF_8                            = "UTF-8";

  private static final String CLOUDMADE_USER_PARAMETER         = "USER_ID";
  private static final String CLOUDMADE_USER_PARAMETER_GROUP   = "\\{" + CLOUDMADE_USER_PARAMETER + "\\}";
  private static final String CLOUDMADE_DEVICE_PARAMETER       = "DEVICE_ID";
  private static final String CLOUDMADE_DEVICE_PARAMETER_GROUP = "\\{" + CLOUDMADE_DEVICE_PARAMETER + "\\}";
  private static final String CLOUDMADE_API_PARAMETER          = "API_KEY";
  private static final String CLOUDMADE_API_PARAMETER_GROUP    = "\\{" + CLOUDMADE_API_PARAMETER + "\\}";
  private static final String CLOUDMADE_TOKEN_URL              = "http://auth.cloudmade.com/token/{" + CLOUDMADE_API_PARAMETER + "}";
  private static final String CLOUDMADE_TOKEN_PARAMETERS       = "userid={" + CLOUDMADE_USER_PARAMETER + "}&deviceid={" + CLOUDMADE_DEVICE_PARAMETER + "}";

  private static final String PARAM_TOKEN                      = "token";
  private static final String PARAM_STYLEID                    = "styleid";
  private static final String PARAM_LATITUDE                   = "latitude";
  private static final String PARAM_LONGITUDE                  = "longitude";
  private static final String PARAM_ZOOM                       = "zoom";

  private static final String URL                              = "http://staticmaps.cloudmade.com/{" + CLOUDMADE_API_PARAMETER + "}/staticmap?token={" + PARAM_TOKEN + "}&size=" + TILE_SIZE + "x" + TILE_SIZE + "&styleid={" + PARAM_STYLEID
                                                                   + "}&center={" + PARAM_LATITUDE + "},{" + PARAM_LONGITUDE + "}&zoom={" + PARAM_ZOOM + "}&format=png";

  private static final String REGEXP_TOKEN                     = "\\{" + PARAM_TOKEN + "\\}";
  private static final String REGEXP_STYLEID                   = "\\{" + PARAM_STYLEID + "\\}";
  private static final String REGEXP_LATITUDE                  = "\\{" + PARAM_LATITUDE + "\\}";
  private static final String REGEXP_LONGITUDE                 = "\\{" + PARAM_LONGITUDE + "\\}";
  private static final String REGEXP_ZOOM                      = "\\{" + PARAM_ZOOM + "\\}";

  private final String        apiKey;
  private final String        token;

  /**
   * 
   * @param key
   * @param apiKey
   * @param token
   */
  public CloudMadeTileProvider(final String key, final String apiKey, final String token)
  {
    super(key);
    this.apiKey = apiKey;
    this.token = token;
  }

  /**
   * 
   * @param apiKey
   * @param userId
   * @param deviceId
   * @return
   */
  public static String requestCloudMadeToken(final String apiKey, final String userId, final String deviceId) throws IOException
  {
    // Calcul d'un UUID
    final UUID uuid = new UUID(userId.hashCode(), deviceId.hashCode());

    // URL d'acces a la demande de token
    final String finalUrl = CLOUDMADE_TOKEN_URL.replaceAll(CLOUDMADE_API_PARAMETER_GROUP, apiKey);
    String finalData = CLOUDMADE_TOKEN_PARAMETERS.replaceAll(CLOUDMADE_USER_PARAMETER_GROUP, URLEncoder.encode(uuid.toString(), UTF_8));
    finalData = finalData.replaceAll(CLOUDMADE_DEVICE_PARAMETER_GROUP, URLEncoder.encode(deviceId, UTF_8));

    // Requette HTTP
    return postHttpRequest(finalUrl, finalData);
  }

  /**
   * 
   * @param url
   * @param data
   * @throws IOException
   */
  private static String postHttpRequest(final String url, final String data) throws IOException
  {
    // Initialisations
    HttpURLConnection connection = null;
    BufferedReader br = null;

    try
    {
      // Initialisation de la connexion
      final URL finalUrl = new URL(url);
      connection = (HttpURLConnection)finalUrl.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      connection.setRequestProperty("Content-Length", Integer.toString(data.getBytes().length));
      connection.setUseCaches(false);
      connection.setDoInput(true);
      connection.setDoOutput(true);

      // Envoi
      final OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
      wr.write(data);
      wr.flush();
      wr.close();

      // Reponse
      final InputStream is = connection.getInputStream();
      br = new BufferedReader(new InputStreamReader(is));
      String line;
      final StringBuilder response = new StringBuilder();
      while ((line = br.readLine()) != null)
      {
        response.append(line);
      }

      return response.toString();
    }
    finally
    {
      if (br != null)
      {
        br.close();
      }
      if (connection != null)
      {
        connection.disconnect();
      }
    }
  }

  @Override
  public final int getMinZoomLevel()
  {
    return 0;
  }

  @Override
  public final int getMaxZoomLevel()
  {
    return 19;
  }

  @Override
  public final URL getTileURL(final Tile tile) throws IOException
  {
    // Longitude et latitude du centre de la tuile
    final double longitude = MercatorProjection.pixelXToLongitude(tile.x * TILE_SIZE + TILE_SIZE / 2, tile.zoom);
    final double latitude = MercatorProjection.pixelYToLatitude(tile.y * TILE_SIZE + TILE_SIZE / 2, tile.zoom);

    String urlString = URL;
    urlString = urlString.replaceAll(REGEXP_ZOOM, Integer.toString(tile.zoom));
    urlString = urlString.replaceAll(REGEXP_LONGITUDE, Double.toString(longitude));
    urlString = urlString.replaceAll(REGEXP_LATITUDE, Double.toString(latitude));
    urlString = urlString.replaceAll(REGEXP_STYLEID, getStyleId());
    urlString = urlString.replaceAll(REGEXP_TOKEN, token);
    urlString = urlString.replaceAll(CLOUDMADE_API_PARAMETER_GROUP, apiKey);

    return new URL(urlString);
  }

  /**
   * 
   * @return
   */
  public abstract String getStyleId();
}
