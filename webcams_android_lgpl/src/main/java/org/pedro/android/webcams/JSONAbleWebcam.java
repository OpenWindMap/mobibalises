package org.pedro.android.webcams;

import org.json.JSONException;
import org.json.JSONObject;
import org.pedro.android.json.JSONAble;
import org.pedro.webcams.Webcam;

/**
 * 
 * @author pedro.m
 */
public class JSONAbleWebcam extends Webcam implements JSONAble
{
  // Balises JSON
  private static final String JSON_ID                   = "id";
  private static final String JSON_ETAT                 = "etat";
  private static final String JSON_NOM                  = "nom";
  private static final String JSON_PAYS                 = "pays";
  private static final String JSON_PRATIQUES            = "pratiques";
  private static final String JSON_LATITUDE             = "latitude";
  private static final String JSON_LONGITUDE            = "longitude";
  private static final String JSON_ALTITUDE             = "altitude";
  private static final String JSON_DIRECTION            = "direction";
  private static final String JSON_CHAMP                = "champ";
  private static final String JSON_URL_IMAGE            = "url_image";
  private static final String JSON_PERIODICITE          = "periodicite";
  private static final String JSON_DECALAGE_PERIODICITE = "decalage_periodicite";
  private static final String JSON_DECALAGE_HORLOGE     = "decalage_horloge";
  private static final String JSON_FUSEAU_HORAIRE       = "fuseau_horaire";
  private static final String JSON_CODE_LOCALE          = "code_locale";
  private static final String JSON_LARGEUR              = "largeur";
  private static final String JSON_HAUTEUR              = "hauteur";
  private static final String JSON_URL_PAGE             = "url_page";
  private static final String JSON_DESCRIPTION          = "description";
  private static final String JSON_STATUT_ENLIGNE       = "statut_enligne";
  private static final String JSON_STATUT_VARIABLE      = "statut_variable";

  @Override
  public JSONObject toJSON() throws JSONException
  {
    // Initialisations
    final JSONObject json = new JSONObject();

    // Identification
    json.put(JSON_ID, id);
    json.put(JSON_ETAT, etat);
    json.put(JSON_NOM, nom);
    json.put(JSON_PAYS, pays);
    json.put(JSON_PRATIQUES, pratiques);
    json.put(JSON_LATITUDE, latitude);
    json.put(JSON_LONGITUDE, longitude);
    json.put(JSON_ALTITUDE, altitude);
    json.put(JSON_DIRECTION, direction);
    json.put(JSON_CHAMP, champ);
    json.put(JSON_URL_IMAGE, urlImage);
    json.put(JSON_PERIODICITE, periodicite);
    json.put(JSON_DECALAGE_PERIODICITE, decalagePeriodicite);
    json.put(JSON_DECALAGE_HORLOGE, decalageHorloge);
    json.put(JSON_FUSEAU_HORAIRE, fuseauHoraire);
    json.put(JSON_CODE_LOCALE, codeLocale);
    json.put(JSON_LARGEUR, largeur);
    json.put(JSON_HAUTEUR, hauteur);
    json.put(JSON_URL_PAGE, urlPage);
    json.put(JSON_DESCRIPTION, description);

    return json;
  }

  @Override
  public void fromJSON(final JSONObject json) throws JSONException
  {
    // Identification
    id = json.optInt(JSON_ID, -1);
    etat = json.getString(JSON_ETAT);
    nom = json.getString(JSON_NOM);
    pays = json.getString(JSON_PAYS);
    pratiques = json.getInt(JSON_PRATIQUES);
    latitude = (float)json.getDouble(JSON_LATITUDE);
    longitude = (float)json.getDouble(JSON_LONGITUDE);
    final int intAltitude = json.optInt(JSON_ALTITUDE, Integer.MIN_VALUE);
    altitude = (intAltitude == Integer.MIN_VALUE ? null : Integer.valueOf(intAltitude));
    direction = json.getInt(JSON_DIRECTION);
    champ = json.getInt(JSON_CHAMP);
    urlImage = json.optString(JSON_URL_IMAGE);
    final int intPeriodicite = json.optInt(JSON_PERIODICITE, Integer.MIN_VALUE);
    periodicite = (intPeriodicite == Integer.MIN_VALUE ? null : Integer.valueOf(intPeriodicite));
    final int intDecalagePeriodicite = json.optInt(JSON_DECALAGE_PERIODICITE, Integer.MIN_VALUE);
    decalagePeriodicite = (intDecalagePeriodicite == Integer.MIN_VALUE ? null : Integer.valueOf(intDecalagePeriodicite));
    final int intDecalageHorloge = json.optInt(JSON_DECALAGE_HORLOGE, Integer.MIN_VALUE);
    decalageHorloge = (intDecalageHorloge == Integer.MIN_VALUE ? null : Integer.valueOf(intDecalageHorloge));
    fuseauHoraire = json.optString(JSON_FUSEAU_HORAIRE);
    codeLocale = json.optString(JSON_CODE_LOCALE);
    final int intLargeur = json.optInt(JSON_LARGEUR, Integer.MIN_VALUE);
    largeur = (intLargeur == Integer.MIN_VALUE ? null : Integer.valueOf(intLargeur));
    final int intHauteur = json.optInt(JSON_HAUTEUR, Integer.MIN_VALUE);
    hauteur = (intHauteur == Integer.MIN_VALUE ? null : Integer.valueOf(intHauteur));
    urlPage = json.optString(JSON_URL_PAGE);
    description = json.optString(JSON_DESCRIPTION);
    statutEnLigne = json.getString(JSON_STATUT_ENLIGNE);
    statutVariable = json.getString(JSON_STATUT_VARIABLE);
  }
}
