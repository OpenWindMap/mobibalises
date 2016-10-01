package org.pedro.spots.pge;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author pedro.m
 */
public final class PgeOceanieSpotProvider extends PgeSpotProvider
{
  private final List<String> countries = new ArrayList<String>();

  /**
   * 
   */
  public PgeOceanieSpotProvider()
  {
    super();

    countries.add("au");
    countries.add("pf");
    countries.add("nc");
    countries.add("nz");
    countries.add("um");
  }

  @Override
  public List<String> getAvailableCountries()
  {
    return countries;
  }
}
