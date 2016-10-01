package org.pedro.map;

/**
 * 
 * @author pedro.m
 */
public interface MapMover
{
  /**
   * 
   */
  public void startMove();

  /**
   * 
   */
  public void stopMove();

  /**
   * 
   * @return
   */
  public boolean isMoveFinished();

  /**
   * 
   */
  public void shutdown();
}
