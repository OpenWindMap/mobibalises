package org.pedro.balises;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 
 * @author pedro.m
 */
public class MobibalisesHistoryBaliseProviderHelper
{
  /**
   * 
   */
  private MobibalisesHistoryBaliseProviderHelper()
  {
    super();
  }

  /**
   * 
   * @param chaine
   * @param delim
   * @return
   */
  private static String[] split(final String chaine, final char delim)
  {
    final List<String> champs = new ArrayList<String>();

    int debut = 0;
    for (int i = 0; i < chaine.length(); i++)
    {
      final char c = chaine.charAt(i);
      if (c == delim)
      {
        champs.add(chaine.substring(debut, i));
        debut = i + 1;
      }
    }
    champs.add(chaine.substring(debut));

    return champs.toArray(new String[0]);
  }

  /**
   * 
   * @param ligne
   * @param id
   * @return
   */
  public static Releve parseLigneReleve(final String ligne, final String id)
  {
    // Champs
    final String[] champs = split(ligne, ';');
    if ((champs == null) || (champs.length < 9) || (champs.length > 10))
    {
      return null;
    }

    // Initialisations
    final Releve releve = new Releve();
    releve.setId(id);
    int colonne = 0;

    // Date releve
    releve.date = parseDate(champs[colonne++]);

    // Date precedent
    releve.dateRelevePrecedent = parseDate(champs[colonne++]);

    // Direction
    releve.directionMoyenne = parseInt(champs[colonne++]);

    // Vent mini
    releve.ventMini = parseDouble(champs[colonne++]);

    // Vent moyen
    releve.ventMoyen = parseDouble(champs[colonne++]);

    // Vent maxi
    releve.ventMaxi = parseDouble(champs[colonne++]);

    // Vent mini tendance
    releve.ventMiniTendance = parseDouble(champs[colonne++]);

    // Vent moyen tendance
    releve.ventMoyenTendance = parseDouble(champs[colonne++]);

    // Vent maxi tendance
    releve.ventMaxiTendance = parseDouble(champs[colonne++]);

    // Temperature
    if (champs.length >= 10)
    {
      releve.temperature = parseDouble(champs[colonne++]);
    }

    // Fin
    return releve;
  }

  /**
   * 
   * @param chaine
   * @return
   */
  private static Date parseDate(final String chaine)
  {
    if (Utils.isStringVide(chaine))
    {
      return null;
    }

    return new Date(1000 * Long.parseLong(chaine, 10));
  }

  /**
   * 
   * @param chaine
   * @return
   */
  private static int parseInt(final String chaine)
  {
    if (Utils.isStringVide(chaine))
    {
      return Integer.MIN_VALUE;
    }

    return Integer.parseInt(chaine);
  }

  /**
   * 
   * @param chaine
   * @return
   */
  private static double parseDouble(final String chaine)
  {
    if (Utils.isStringVide(chaine))
    {
      return Double.NaN;
    }

    return Double.parseDouble(chaine);
  }
}
