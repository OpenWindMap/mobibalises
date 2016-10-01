package org.pedro.android.mobibalises_common.map;

/**
 * 
 * @author pedro.m
 */
public interface WebcamTapListener
{
  /**
   * 
   * @param webcamItem
   */
  public void onWebcamTap(final WebcamItem webcamItem);

  /**
   * 
   * @param webcamItem
   */
  public void onWebcamLongTap(final WebcamItem webcamItem);

  /**
   * 
   * @param webcamItem
   */
  public void onWebcamInfoLinkTap(final WebcamItem webcamItem);

  /**
   * 
   * @param webcamItem
   */
  public void onWebcamImageTap(final WebcamItem webcamItem);
}
