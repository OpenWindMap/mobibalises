package org.pedro.android.mobibalises_common.service;

/**
 * 
 * @author pedro.m
 */
public class WebcamProviderInfos
{
  // Divers
  private boolean   paused                     = false;
  private boolean   sleeping                   = false;
  private boolean   updateInProgress           = false;
  private long      nextWakeUp;

  // Webcams
  private long      lastWebcamsUpdateLocalDate = -1;
  private long      lastWebcamsCheckLocalDate  = -1;
  private Throwable webcamsException;

  /**
   * 
   * @return
   */
  public boolean isUpdateInProgress()
  {
    return !paused && updateInProgress;
  }

  /**
   * @return the lastWebcamsUpdateLocalDate
   */
  public long getLastWebcamsUpdateLocalDate()
  {
    return lastWebcamsUpdateLocalDate;
  }

  /**
   * @return the webcamsException
   */
  public Throwable getWebcamsException()
  {
    return webcamsException;
  }

  /**
   * @param lastWebcamsUpdateLocalDate the lastWebcamsUpdateLocalDate to set
   */
  protected void setLastWebcamsUpdateLocalDate(final long lastWebcamsUpdateLocalDate)
  {
    this.lastWebcamsUpdateLocalDate = lastWebcamsUpdateLocalDate;
  }

  /**
   * @param webcamsException the webcamsException to set
   */
  protected void setWebcamsException(final Throwable webcamsException)
  {
    this.webcamsException = webcamsException;
  }

  /**
   * @return the lastWebcamsCheckLocalDate
   */
  public long getLastWebcamsCheckLocalDate()
  {
    return lastWebcamsCheckLocalDate;
  }

  /**
   * @param lastWebcamsCheckLocalDate the lastWebcamsCheckLocalDate to set
   */
  protected void setLastWebcamsCheckLocalDate(final long lastWebcamsCheckLocalDate)
  {
    this.lastWebcamsCheckLocalDate = lastWebcamsCheckLocalDate;
  }

  /**
   * @return the paused
   */
  public boolean isPaused()
  {
    return paused;
  }

  /**
   * @param paused the paused to set
   */
  protected void setPaused(final boolean paused)
  {
    this.paused = paused;
  }

  /**
   * @return the nextWakeUp
   */
  public long getNextWakeUp()
  {
    return nextWakeUp;
  }

  /**
   * @param nextWakeUp the nextWakeUp to set
   */
  public void setNextWakeUp(final long nextWakeUp)
  {
    this.nextWakeUp = nextWakeUp;
  }

  /**
   * @return the sleeping
   */
  public boolean isSleeping()
  {
    return sleeping;
  }

  /**
   * @param sleeping the sleeping to set
   */
  public void setSleeping(final boolean sleeping)
  {
    this.sleeping = sleeping;
  }

  /**
   * @param updateInProgress the updateInProgress to set
   */
  public void setUpdateInProgress(final boolean updateInProgress)
  {
    this.updateInProgress = updateInProgress;
  }
}
