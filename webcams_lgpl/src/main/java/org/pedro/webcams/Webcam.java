/*******************************************************************************
 * WebcamsLib is Copyright 2014 by Pedro M.
 * 
 * This file is part of WebcamsLib.
 *
 * WebcamsLib is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * WebcamsLib is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * Commercial Distribution License
 * If you would like to distribute WebcamsLib (or portions thereof) under a
 * license other than the "GNU Lesser General Public License, version 3", please
 * contact Pedro M (pedro.pub@free.fr).
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with WebcamsLib. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.pedro.webcams;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * 
 * @author pedro.m
 */
public class Webcam
{
  public static final String                   ETAT_VALIDE               = "V";
  public static final String                   ETAT_INVALIDE             = "I";

  public static final int                      MASQUE_PRATIQUE_VOL_LIBRE = 1;
  public static final int                      MASQUE_PRATIQUE_KITE      = 2;
  public static final int                      MASQUE_PRATIQUE_SKI       = 4;
  public static final int                      MASQUE_PRATIQUE_AUTRE     = 8;

  public static final String                   STATUT_ENLIGNE_ENLIGNE    = "E";
  public static final String                   STATUT_ENLIGNE_HORSLIGNE  = "H";

  public static final String                   STATUT_VARIABLE_VARIABLE  = "V";
  public static final String                   STATUT_VARIABLE_FIGEE     = "F";

  private static final Pattern                 splitPattern              = Pattern.compile("\\{|\\}");
  private static final Map<String, String>     javaFormats               = new HashMap<String, String>();
  private static final Map<String, DateFormat> dateFormats               = new HashMap<String, DateFormat>();

  // Donnees
  public int                                   id;
  public String                                etat;
  public String                                nom;
  public String                                pays;
  public int                                   pratiques;
  public float                                 latitude;
  public float                                 longitude;
  public Integer                               altitude;
  public int                                   direction;
  public int                                   champ;
  public String                                urlImage;
  public Integer                               periodicite;
  public Integer                               decalagePeriodicite;
  public Integer                               decalageHorloge;
  public String                                fuseauHoraire;
  public String                                codeLocale;
  public Integer                               largeur;
  public Integer                               hauteur;
  public String                                urlPage;
  public String                                description;
  public String                                statutEnLigne;
  public String                                statutVariable;

  /**
   * 
   */
  static
  {
    javaFormats.put("eee", "EEE");
    javaFormats.put("eeee", "EEEE");
    javaFormats.put("ddd", "DDD");
    javaFormats.put("m", "M");
    javaFormats.put("mm", "MM");
    javaFormats.put("mmm", "MMM");
    javaFormats.put("mmmm", "MMMM");
    javaFormats.put("hh", "HH");
    javaFormats.put("h", "H");
    javaFormats.put("kk", "KK");
    javaFormats.put("k", "K");
    javaFormats.put("i", "m");
    javaFormats.put("ii", "mm");
  }

  /**
   * 
   */
  public Webcam()
  {
    // Nothing
  }

  /**
   * 
   * @param id
   */
  public Webcam(final int id)
  {
    this.id = id;
  }

  /**
   * 
   * @param b
   * @return
   */
  @Override
  public boolean equals(final Object object)
  {
    if (object == null)
    {
      return false;
    }

    if (!Webcam.class.isAssignableFrom(object.getClass()))
    {
      return false;
    }

    final Webcam webcam = (Webcam)object;

    return id == webcam.id;
  }

  @Override
  public int hashCode()
  {
    return id;
  }

  @Override
  public String toString()
  {
    return id + "/" + nom;
  }

  /**
   * 
   * @return
   */
  public String getUrlImage()
  {
    return getUrlImage(urlImage, periodicite, decalagePeriodicite, decalageHorloge, fuseauHoraire, codeLocale);
  }

  /**
   * 
   * @param key
   * @param fuseauHoraire
   * @param codeLocale
   * @return
   */
  private static DateFormat getDateFormat(final String key, final String fuseauHoraire, final String codeLocale)
  {
    // Initialisations
    final String finalKey = key + "#" + (Utils.isStringVide(fuseauHoraire) ? "" : fuseauHoraire) + "#" + (Utils.isStringVide(codeLocale) ? "" : codeLocale);

    // En cache ?
    DateFormat dateFormat = dateFormats.get(finalKey);
    if (dateFormat != null)
    {
      // Oui
      return dateFormat;
    }

    // Non, calcul de la correspondance au format java
    String javaFormat = javaFormats.get(key);
    if (javaFormat == null)
    {
      javaFormat = key;
    }

    // Locale
    final Locale locale;
    if (!Utils.isStringVide(codeLocale))
    {
      final String[] codes = codeLocale.split("-");
      if (codes.length >= 2)
      {
        locale = new Locale(codes[0], codes[1]);
      }
      else
      {
        locale = new Locale(codes[0]);
      }
    }
    else
    {
      locale = Locale.getDefault();
    }

    // Instanciation du format Java et enregistrement
    dateFormat = new SimpleDateFormat(javaFormat, locale);
    final TimeZone timeZone = (Utils.isStringVide(fuseauHoraire) ? TimeZone.getDefault() : TimeZone.getTimeZone(fuseauHoraire));
    dateFormat.setTimeZone(timeZone);
    dateFormats.put(finalKey, dateFormat);

    return dateFormat;
  }

  /**
   * 
   * @param url
   * @param periodicite
   * @param decalagePeriodicite
   * @param decalageHorloge
   * @param fuseauHoraire
   * @param codeLocale
   * @return
   */
  public static String getUrlImage(final String url, final Integer periodicite, final Integer decalagePeriodicite, final Integer decalageHorloge, final String fuseauHoraire, final String codeLocale)
  {
    // Initialisations
    final StringBuilder buffer = new StringBuilder();
    final int finalPeriodicite = (periodicite == null ? 0 : periodicite.intValue());
    final int finalDecalagePeriodicite = (decalagePeriodicite == null ? 0 : decalagePeriodicite.intValue());
    final int finalDecalageHorloge = (decalageHorloge == null ? 0 : decalageHorloge.intValue());

    // Timestamp de base
    final long finalTs;
    {
      // Début (avec décalage horloge)
      final long now = (System.currentTimeMillis() + 60000L * finalDecalageHorloge);
      long ts = now;

      // Périodicité et décalage (en minutes)
      if (finalPeriodicite > 0)
      {
        final long periodiciteLong = 60000L * finalPeriodicite;
        ts = ts - (ts % periodiciteLong);
        if (finalDecalagePeriodicite > 0)
        {
          final long decalagePeriodiciteLong = 60000L * finalDecalagePeriodicite;
          ts += decalagePeriodiciteLong;
          if (ts > now)
          {
            ts -= periodiciteLong;
          }
        }
      }

      // Fin
      finalTs = ts;
    }

    // Séparation
    final String[] splitted = splitPattern.split(url);
    for (int i = 0; i < splitted.length; i++)
    {
      if ((i % 2) == 0)
      {
        buffer.append(splitted[i]);
      }
      else
      {
        final DateFormat dateFormat = getDateFormat(splitted[i], fuseauHoraire, codeLocale);
        buffer.append(dateFormat.format(new Date(finalTs)));
      }
    }

    return buffer.toString();
  }

  /**
   * 
   * @param webcam
   */
  public void copyFrom(final Webcam webcam)
  {
    id = webcam.id;
    etat = webcam.etat;
    nom = webcam.nom;
    pays = webcam.pays;
    pratiques = webcam.pratiques;
    latitude = webcam.latitude;
    longitude = webcam.longitude;
    altitude = webcam.altitude;
    direction = webcam.direction;
    champ = webcam.champ;
    urlImage = webcam.urlImage;
    periodicite = webcam.periodicite;
    decalagePeriodicite = webcam.decalagePeriodicite;
    decalageHorloge = webcam.decalageHorloge;
    fuseauHoraire = webcam.fuseauHoraire;
    codeLocale = webcam.codeLocale;
    largeur = webcam.largeur;
    hauteur = webcam.hauteur;
    urlPage = webcam.urlPage;
    description = webcam.description;
    statutEnLigne = webcam.statutEnLigne;
    statutVariable = webcam.statutVariable;
  }

  /**
   * 
   * @param url
   * @param periodicite
   * @param decalagePeriodicite
   * @param decalageHorloge
   * @param fuseauHoraire
   * @param codeLocale
   */
  /* TODO
  private static void doIt(final String url, final int periodicite, final int decalagePeriodicite, final int decalageHorloge, final String fuseauHoraire, final String codeLocale)
  {
    //System.out.println("initiale (" + periodicite + "/" + decalagePeriodicite + "/" + decalageHorloge + "/" + fuseauHoraire + "/" + codeLocale + ") : " + url);
    final String finale = getUrlImage(url, Integer.valueOf(periodicite), Integer.valueOf(decalagePeriodicite), Integer.valueOf(decalageHorloge), fuseauHoraire, codeLocale);
    System.out.println("   => finale : " + finale);
  }
  */

  /**
   * 
   * @param args
   */
  /* TODO
  public static void main(final String[] args)
  {
    try
    {
      doIt("http://www.prevol.com/webcam_{eeee}_{yyyy}_{mm}_{dd}-{hh}_{ii}_{ss}.jpg", 0, 0, 0, null, null);
      doIt("http://www.prevol.com/webcam_{eeee}_{yyyy}_{mm}_{dd}-{hh}_{ii}_{ss}.jpg", 5, 0, 0, null, "en");
      doIt("http://www.prevol.com/webcam_{eeee}_{yyyy}_{mm}_{dd}-{hh}_{ii}_{ss}.jpg", 5, 0, 0, "America/Los_Angeles", "de-at");
      doIt("http://www.prevol.com/webcam_{eeee}_{yyyy}_{mm}_{dd}-{hh}_{ii}_{ss}.jpg", 5, 0, 0, "Europe/Paris", null);
      doIt("http://www.prevol.com/webcam_{eeee}_{yyyy}_{mm}_{dd}-{hh}_{ii}_{ss}.jpg", 5, 1, 0, null, null);
      doIt("http://www.prevol.com/webcam_{eeee}_{yyyy}_{mm}_{dd}-{hh}_{ii}_{ss}.jpg", 5, 2, 0, null, null);
      doIt("http://www.prevol.com/webcam_{eeee}_{yyyy}_{mm}_{dd}-{hh}_{ii}_{ss}.jpg", 3, 0, 0, null, null);
      doIt("http://www.prevol.com/webcam_{eeee}_{yyyy}_{mm}_{dd}-{hh}_{ii}_{ss}.jpg", 3, 2, 0, null, null);
      doIt("http://www.prevol.com/webcam_{eeee}_{yyyy}_{mm}_{dd}-{hh}_{ii}_{ss}.jpg", 3, 2, -15, null, null);
    }
    catch (final Throwable th)
    {
      th.printStackTrace();
    }
  }
  */
}
