package org.pedro.spots.pge;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author pedro.m
 */
public final class PgeAfriqueSpotProvider extends PgeSpotProvider
{
  private final List<String> countries = new ArrayList<String>();

  /**
   * 
   */
  public PgeAfriqueSpotProvider()
  {
    super();

    countries.add("za"); // Afrique du sud
    countries.add("ma"); // Maroc
    countries.add("re"); // Reunion
    countries.add("et"); // Ethiopie
    countries.add("ao"); // Angola
    countries.add("dz");
    countries.add("cv");
    countries.add("eg");
    countries.add("gh");
    countries.add("ke");
    countries.add("mu");
    countries.add("mz");
    countries.add("na");
    countries.add("sz");
    countries.add("tz");
    countries.add("tn");
    countries.add("zw");
  }

  @Override
  public List<String> getAvailableCountries()
  {
    return countries;
  }
}
