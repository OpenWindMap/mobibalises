package org.pedro.balises;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 
 * @author pedro.m
 */
public final class BidonProvider extends AbstractBaliseProvider
{
  private static final String       ID_1      = "Bidon01";
  private static final String       ID_2      = "Bidon02";

  private static final List<String> COUNTRIES = new ArrayList<String>();

  private final Balise              balise2;

  /**
   * 
   */
  static
  {
    COUNTRIES.add(Locale.FRANCE.getCountry());
  }

  /**
   * 
   * @param name
   */
  public BidonProvider(final String name)
  {
    super(name, Locale.FRANCE.getCountry(), null, 8);

    final Balise balise1 = new Balise();
    balise1.setId(ID_1);
    balise1.nom = "Bidon 1";
    balise1.altitude = 1150;
    balise1.latitude = 45.60288056;
    balise1.longitude = 5.706277778;
    balise1.active = Utils.BOOLEAN_TRUE;
    getBalisesMap().put(ID_1, balise1);

    final Releve releve1 = new Releve();
    randomReleve(releve1);
    getRelevesMap().put(ID_1, releve1);

    balise2 = new Balise();
    balise2.setId(ID_2);
    balise2.nom = "Bidon 2";
    balise2.altitude = 1150;
    balise2.latitude = 45.60288056;
    balise2.longitude = 5.606277778;
    balise2.active = Utils.BOOLEAN_TRUE;
  }

  @Override
  public boolean isAvailable()
  {
    return true;
  }

  @Override
  public boolean updateBalises() throws IOException
  {
    if (getBalisesMap().get(ID_2) == null)
    {
      getBalisesMap().put(ID_2, balise2);
    }
    else
    {
      getBalisesMap().remove(ID_2);
      getRelevesMap().remove(ID_2);
    }

    return true;
  }

  /**
   * 
   * @param releve
   */
  private static void randomReleve(final Releve releve)
  {
    releve.date = new Date();
    releve.directionInstantanee = (int)(360 * Math.random());
    releve.directionMoyenne = (int)(360 * Math.random());
    releve.temperature = 60 * Math.random() - 20;
    releve.ventMoyen = 70 * Math.random();
    releve.ventMini = 10 * Math.random();
    releve.ventMini = Double.NaN;
    releve.ventMaxi = 100 * Math.random();
  }

  @Override
  public boolean updateReleves() throws IOException
  {
    Releve releve = getReleveById(ID_1);
    randomReleve(releve);

    releve = getReleveById(ID_2);
    if (releve == null)
    {
      releve = new Releve();
      getRelevesMap().put(ID_2, releve);
    }
    randomReleve(releve);

    return true;
  }

  @Override
  public String getBaliseDetailUrl(final String id)
  {
    return "http://www.google.fr/search?q=bidon";
  }

  @Override
  public String getBaliseHistoriqueUrl(final String id)
  {
    return null;
  }

  @Override
  public long getBalisesUpdateDate() throws IOException
  {
    return Utils.toUTC(System.currentTimeMillis());
  }

  @Override
  public long getRelevesUpdateDate() throws IOException
  {
    return Utils.toUTC(System.currentTimeMillis());
  }

  @Override
  public boolean updateBalisesUpdateDate() throws IOException
  {
    return true;
  }

  @Override
  public boolean updateRelevesUpdateDate() throws IOException
  {
    return true;
  }

  @Override
  public Balise newBalise()
  {
    return new Balise();
  }

  @Override
  public Releve newReleve()
  {
    return new Releve();
  }

  /**
   * 
   * @return
   */
  public static List<String> getAvailableCountries()
  {
    return COUNTRIES;
  }

  @Override
  public boolean isMultiCountries()
  {
    return false;
  }

  @Override
  public int getDefaultDeltaReleves()
  {
    return 20;
  }
}
