package org.pedro.map;

/**
 * 
 * @author pedro.m
 */
public final class MercatorProjection implements Projection
{
  protected static final int    TILE_BASE_SIZE              = 256;

  private static final double   RAYON_TERRE_KM              = 6371;

  private static final long     TILE_BASE_SIZE_LONG         = TILE_BASE_SIZE;
  private static final double   CONSTANTE_GROUND_RESOLUTION = 40075016.686;
  private static final double   CST_180_SUR_PI              = 180 / Math.PI;
  private static final double   PI_SUR_180                  = Math.PI / 180;
  private static final double   DEUX_PI                     = Math.PI * 2;
  private static final double   QUATRE_PI                   = Math.PI * 4;
  private static final long     TROIS_CENT_SOIXANTE         = 360;
  private static final long     CENT_QUATRE_VINGT           = 180;
  private static final long     QUATRE_VINGT_DIX            = 90;
  private static final double   ZERO_CINQ                   = 0.5;
  private static final long     ZERO                        = 0;
  private static final long     UN                          = 1;
  private static final long     DEUX                        = 2;

  private MapDisplayer<?, ?, ?> mapDisplayer;

  /**
   * 
   * @param latitude
   * @param zoom
   * @return
   */
  public static double latitudeToPixelY(final double latitude, final int zoom)
  {
    final double sinLatitude = Math.sin(latitude * PI_SUR_180);
    return ((ZERO_CINQ - Math.log((UN + sinLatitude) / (UN - sinLatitude)) / QUATRE_PI) * (TILE_BASE_SIZE_LONG * Math.pow(2, zoom)));
  }

  /**
   * 
   * @param latitude
   * @param zoom
   * @param tileSize
   * @return
   */
  public static long latitudeToTileY(final double latitude, final int zoom, final int tileSize)
  {
    return pixelYToTileY(latitudeToPixelY(latitude, zoom), zoom, tileSize);
  }

  /**
   * 
   * @param longitude
   * @param zoom
   * @return
   */
  public static double longitudeToPixelX(final double longitude, final int zoom)
  {
    return ((longitude + CENT_QUATRE_VINGT) / TROIS_CENT_SOIXANTE * (TILE_BASE_SIZE_LONG * Math.pow(2, zoom)));
  }

  /**
   * 
   * @param longitude
   * @param zoom
   * @param tileSize
   * @return
   */
  public static long longitudeToTileX(final double longitude, final int zoom, final int tileSize)
  {
    return pixelXToTileX(longitudeToPixelX(longitude, zoom), zoom, tileSize);
  }

  /**
   * 
   * @param pixelX
   * @param zoom
   * @return
   */
  public static double pixelXToLongitude(final double pixelX, final int zoom)
  {
    return TROIS_CENT_SOIXANTE * ((pixelX / (TILE_BASE_SIZE_LONG * Math.pow(2, zoom))) - ZERO_CINQ);
  }

  /**
   * 
   * @param pixelX
   * @param zoom
   * @param tileSize
   * @return
   */
  public static long pixelXToTileX(final double pixelX, final int zoom, final int tileSize)
  {
    return (long)Math.min(Math.max((pixelX / tileSize), ZERO), Math.pow(DEUX, zoom) - UN);
  }

  /**
   * 
   * @param pixelY
   * @param zoom
   * @return
   */
  public static double pixelYToLatitude(final double pixelY, final int zoom)
  {
    final double y = ZERO_CINQ - (pixelY / (TILE_BASE_SIZE_LONG * Math.pow(2, zoom)));
    return QUATRE_VINGT_DIX - TROIS_CENT_SOIXANTE * Math.atan(Math.exp(-y * DEUX_PI)) / Math.PI;
  }

  /**
   * 
   * @param pixelY
   * @param zoom
   * @param tileSize
   * @return
   */
  public static long pixelYToTileY(final double pixelY, final int zoom, final int tileSize)
  {
    return (long)Math.min(Math.max((pixelY / tileSize), ZERO), Math.pow(DEUX, zoom) - UN);
  }

  /**
   * 
   * @param tileX
   * @param zoom
   * @param tileSize
   * @return
   */
  public static double tileXToLongitude(final long tileX, final int zoom, final int tileSize)
  {
    return pixelXToLongitude(tileX * tileSize, zoom);
  }

  /**
   * 
   * @param tileY
   * @param zoom
   * @param tileSize
   * @return
   */
  public static double tileYToLatitude(final long tileY, final int zoom, final int tileSize)
  {
    return pixelYToLatitude(tileY * tileSize, zoom);
  }

  /**
   * 
   * @param latitude
   * @param zoom
   * @return
   */
  private static double calculateGroundResolution(final double latitude, final int zoom)
  {
    return Math.cos(latitude * PI_SUR_180) * CONSTANTE_GROUND_RESOLUTION / (TILE_BASE_SIZE_LONG * Math.pow(2, zoom));
  }

  /**
   * 
   * @param display
   */
  public MercatorProjection(final MapDisplayer<?, ?, ?> mapDisplayer)
  {
    this.mapDisplayer = mapDisplayer;
  }

  @Override
  public float metersToPixels(final float meters)
  {
    final MapController controller = mapDisplayer.getController();
    return metersToPixels(meters, controller.getCenter(), controller.getZoom());
  }

  @Override
  public float metersToPixels(final float meters, final GeoPoint center, final int zoom)
  {
    return (float)(meters * (UN / calculateGroundResolution(center.getLatitude(), zoom)));
  }

  @Override
  public GeoPoint fromPixels(final int x, final int y, final GeoPoint point)
  {
    // Verification des dimensions
    if ((mapDisplayer.getPixelWidth() <= ZERO) || (mapDisplayer.getPixelHeight() <= ZERO))
    {
      return null;
    }

    // Initialisations
    final GeoPoint retour = (point == null ? new GeoPoint() : point);
    final GeoPoint center = mapDisplayer.getController().getCenter();
    final int zoom = mapDisplayer.getController().getZoom();
    final int width = mapDisplayer.getPixelWidth();
    final int height = mapDisplayer.getPixelHeight();

    // Coordonnees Pixel du coin superieur gauche
    final double pixelX = longitudeToPixelX(center.getLongitude(), zoom) - (width / 2);
    final double pixelY = latitudeToPixelY(center.getLatitude(), zoom) - (height / 2);

    // Conversion en Longitude/Latitude
    retour.set(pixelYToLatitude(pixelY + y, zoom), pixelXToLongitude(pixelX + x, zoom));

    return retour;
  }

  @Override
  public Point toPixels(final GeoPoint geoPoint, final Point point)
  {
    return toPixels(geoPoint.getLatitude(), geoPoint.getLongitude(), point);
  }

  @Override
  public Point toPixels(final double latitude, final double longitude, final Point point)
  {
    // Verification des dimensions
    if ((mapDisplayer.getPixelWidth() <= ZERO) || (mapDisplayer.getPixelHeight() <= ZERO))
    {
      return null;
    }

    // Initialisations
    final Point retour = (point == null ? new Point() : point);
    final GeoPoint center = mapDisplayer.getController().getCenter();
    final int zoom = mapDisplayer.getController().getZoom();
    final int width = mapDisplayer.getPixelWidth();
    final int height = mapDisplayer.getPixelHeight();

    // Coordonnees Pixel du coin superieur gauche
    final double pixelX = longitudeToPixelX(center.getLongitude(), zoom) - (width / 2);
    final double pixelY = latitudeToPixelY(center.getLatitude(), zoom) - (height / 2);

    retour.set((int)Math.round(longitudeToPixelX(longitude, zoom) - pixelX), (int)Math.round(latitudeToPixelY(latitude, zoom) - pixelY));

    return retour;
  }

  /**
   * 
   * @param point1
   * @param point2
   * @return
   */
  public static double calculateDistance(final GeoPoint point1, final GeoPoint point2)
  {
    final double lat1Rad = point1.getLatitude() * PI_SUR_180;
    final double lat2Rad = point2.getLatitude() * PI_SUR_180;
    double squareSinDeltaLat = Math.sin((lat2Rad - lat1Rad) / 2);
    squareSinDeltaLat *= squareSinDeltaLat;
    double squareSinDeltaLon = Math.sin((point2.getLongitude() - point1.getLongitude()) / 2 * PI_SUR_180);
    squareSinDeltaLon *= squareSinDeltaLon;
    final double a = squareSinDeltaLat + Math.cos(lat1Rad) * Math.cos(lat2Rad) * squareSinDeltaLon;
    final double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return RAYON_TERRE_KM * c;
  }

  /**
   * 
   * @param degrees
   * @return
   */
  private static double toRad(final double degrees)
  {
    return degrees * PI_SUR_180;
  }

  /**
   * 
   * @param rads
   * @return
   */
  private static double toDegrees(final double rads)
  {
    return rads * CST_180_SUR_PI;
  }

  /**
   * 
   * @param point1
   * @param point2
   * @return
   */
  public static double calculateBearing(final GeoPoint point1, final GeoPoint point2)
  {
    final double lat1 = toRad(point1.getLatitude());
    final double lat2 = toRad(point2.getLatitude());
    final double lon1 = toRad(point1.getLongitude());
    final double lon2 = toRad(point2.getLongitude());
    final double deltaLon = lon2 - lon1;
    final double y = Math.sin(deltaLon) * Math.cos(lat2);
    final double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLon);

    final double bearing = (toDegrees(Math.atan2(y, x)) + TROIS_CENT_SOIXANTE) % TROIS_CENT_SOIXANTE;

    return bearing;
  }

  /**
   * 
   */
  public void onShutdown()
  {
    // Divers
    mapDisplayer = null;
  }

  /**
   * 
   * @param args
   */
  /*
  public static void main(String[] args)
  {
    GeoPoint sthil = new GeoPoint(45.307316, 5.887016);
    GeoPoint gdratz = new GeoPoint(45.329298, 5.635856);

    double distance1 = calculateDistance(sthil, gdratz);
    double distance2 = calculateDistance(sthil, gdratz);

    System.out.println("distance : " + distance1 + "/" + distance2);
  }
  */
}
