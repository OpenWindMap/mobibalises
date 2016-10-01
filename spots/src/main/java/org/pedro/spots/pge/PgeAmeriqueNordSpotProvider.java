package org.pedro.spots.pge;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author pedro.m
 */
public final class PgeAmeriqueNordSpotProvider extends PgeSpotProvider
{
  private final List<String> countries = new ArrayList<String>();

  /**
   * 
   */
  public PgeAmeriqueNordSpotProvider()
  {
    super();

    countries.add("us");
    countries.add("ca");
    countries.add("mx");
  }

  @Override
  public List<String> getAvailableCountries()
  {
    return countries;
  }
}
