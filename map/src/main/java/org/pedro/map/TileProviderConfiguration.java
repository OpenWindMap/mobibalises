package org.pedro.map;

/**
 * 
 * @author pedro.m
 */
public abstract class TileProviderConfiguration
{
  protected String key;
  protected String name;

  /**
   * 
   * @param params
   * @return
   */
  protected abstract TileProvider getNewTileProvider(final Object... params);

  /**
   * 
   */
  protected void onShutdown()
  {
    //Nothing
  }
}
