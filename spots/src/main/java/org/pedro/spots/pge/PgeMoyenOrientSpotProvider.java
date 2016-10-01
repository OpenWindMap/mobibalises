package org.pedro.spots.pge;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author pedro.m
 */
public final class PgeMoyenOrientSpotProvider extends PgeSpotProvider
{
  private final List<String> countries = new ArrayList<String>();

  /**
   * 
   */
  public PgeMoyenOrientSpotProvider()
  {
    super();

    countries.add("am");
    countries.add("az");
    countries.add("cy");
    countries.add("ge");
    countries.add("ir");
    countries.add("il");
    countries.add("jo");
    countries.add("kz");
    countries.add("lb");
    countries.add("qa");
    countries.add("sa");
    countries.add("tr");
    countries.add("ae");
  }

  @Override
  public List<String> getAvailableCountries()
  {
    return countries;
  }
}
