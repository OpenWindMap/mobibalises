package com.pedro.android.mobibalises.favorites;

import org.pedro.map.GeoPoint;

/**
 * 
 * @author pedro.m
 *
 */
public abstract class Favorite
{
  private final String id;

  /**
   * 
   * @param id
   */
  public Favorite(final String id)
  {
    this.id = id;
  }

  /**
   * 
   * @return
   */
  public abstract String getName();

  /**
   * 
   * @return
   */
  public abstract GeoPoint getCenter();

  @Override
  public abstract int hashCode();

  @Override
  public abstract boolean equals(final Object object);

  /**
   * @return the id
   */
  public String getId()
  {
    return id;
  }
}
