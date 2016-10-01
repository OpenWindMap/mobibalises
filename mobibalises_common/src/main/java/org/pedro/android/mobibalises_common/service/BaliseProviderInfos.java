package org.pedro.android.mobibalises_common.service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.pedro.android.mobibalises_common.Strings;
import org.pedro.android.mobibalises_common.service.AbstractProvidersService.BaliseProviderMode;
import org.pedro.balises.Utils;

/**
 * 
 * @author pedro.m
 */
public class BaliseProviderInfos
{
  private static final int                                        DAY_DURATION_MILLIS        = 86400000;
  private static final DateFormat                                 DATE_FORMAT                = new SimpleDateFormat("dd/MM/yy HH:mm:ss");

  // Divers
  private String                                                  name;
  private AbstractProvidersService.BaliseProviderThread           baliseProviderThread;
  protected final Object                                          baliseProviderThreadLock   = new Object();
  protected AbstractProvidersService.BaliseProviderThreadReceiver baliseProviderThreadReceiver;

  private boolean                                                 paused                     = false;
  private boolean                                                 sleeping                   = false;
  private boolean                                                 updateInProgress           = false;
  private long                                                    nextWakeUp;
  protected final Object                                          updateFireLock             = new Object();

  // Indisponibilite
  protected boolean                                               availabilityManaged        = false;
  protected TimeZone                                              timeZone;
  protected long                                                  availabilityBegin;
  protected long                                                  availabilityEnd;

  // Modes
  protected List<BaliseProviderMode>                              activeModes;

  // Balises
  private long                                                    lastBalisesUpdateDate      = -1;
  private long                                                    lastBalisesUpdateLocalDate = -1;
  private long                                                    lastBalisesCheckLocalDate  = -1;
  private long                                                    balisesUpdatePeriod        = -1;
  private Throwable                                               balisesException;

  // Releves
  private boolean                                                 adjustRelevesUpdate;
  private boolean                                                 relevesAdjusted            = false;
  private long                                                    lastRelevesUpdateDate      = -1;
  private long                                                    previousRelevesUpdateDate  = -1;
  private long                                                    lastRelevesUpdateLocalDate = -1;
  private long                                                    lastRelevesCheckLocalDate  = -1;
  private long                                                    relevesUpdatePeriod        = -1;
  private Throwable                                               relevesException;

  /**
   * 
   * @return
   */
  public boolean isUpdateInProgress()
  {
    return !paused && !sleeping && updateInProgress;
  }

  /**
   * 
   * @return
   */
  protected boolean isAvailable()
  {
    // Initialisations
    final Calendar calendar = Calendar.getInstance(timeZone);

    // Passage dans le fuseau du provider
    final long providerNow = calendar.getTimeInMillis();
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    final long dayBegin = calendar.getTimeInMillis();
    final long begin = dayBegin + availabilityBegin;
    final long end = dayBegin + availabilityEnd;

    // Dans la plage de dispos du jour
    if ((providerNow >= begin) && (providerNow <= end))
    {
      return true;
    }

    // Calcul de la date de fin qui va servir de comparaison
    final long comparableEnd;
    if (providerNow < begin)
    {
      comparableEnd = end - DAY_DURATION_MILLIS; // Fin de dispo de la veille
    }
    else
    {
      comparableEnd = end;
    }

    // Comparaison avec la date de derniere recup serveur
    final long lastProviderUpdate = Utils.fromUTC(lastRelevesUpdateDate);
    final boolean available = (lastProviderUpdate < comparableEnd);

    return available;
  }

  @Override
  public String toString()
  {
    final StringBuilder buffer = new StringBuilder(256);

    buffer.append("adj=").append(isRelevesAdjusted());
    dateToString(", lrud", getLastRelevesUpdateDate(), buffer);
    dateToString(", prud", getPreviousRelevesUpdateDate(), buffer);
    dateToString(", lruld", getLastRelevesUpdateLocalDate(), buffer);
    dateToString(", lrcld", getLastRelevesCheckLocalDate(), buffer);
    buffer.append(", rup=").append(getRelevesUpdatePeriod());

    dateToString("  ###  lbud", getLastBalisesUpdateDate(), buffer);
    dateToString(", lbuld", getLastBalisesUpdateLocalDate(), buffer);
    dateToString(", lbcld", getLastBalisesCheckLocalDate(), buffer);
    buffer.append(", bup=").append(getBalisesUpdatePeriod());

    return buffer.toString();
  }

  /**
   * 
   * @param title
   * @param timestamp
   * @param buffer
   */
  private static void dateToString(final String title, final long timestamp, final StringBuilder buffer)
  {
    buffer.append(title);
    buffer.append(Strings.CHAR_EGAL);
    buffer.append(DATE_FORMAT.format(new Date(timestamp)));
    buffer.append(Strings.CHAR_SPACE).append(Strings.CHAR_PARENTHESE_DEB);
    buffer.append(timestamp);
    buffer.append(Strings.CHAR_PARENTHESE_FIN);
  }

  /**
   * @return the lastBalisesUpdateDate
   */
  public long getLastBalisesUpdateDate()
  {
    return lastBalisesUpdateDate;
  }

  /**
   * @return the lastRelevesUpdateDate
   */
  public long getLastRelevesUpdateDate()
  {
    return lastRelevesUpdateDate;
  }

  /**
   * @return the lastBalisesUpdateLocalDate
   */
  public long getLastBalisesUpdateLocalDate()
  {
    return lastBalisesUpdateLocalDate;
  }

  /**
   * @return the lastRelevesUpdateLocalDate
   */
  public long getLastRelevesUpdateLocalDate()
  {
    return lastRelevesUpdateLocalDate;
  }

  /**
   * @return the balisesException
   */
  public Throwable getBalisesException()
  {
    return balisesException;
  }

  /**
   * @return the relevesException
   */
  public Throwable getRelevesException()
  {
    return relevesException;
  }

  /**
   * @return the name
   */
  public String getName()
  {
    return name;
  }

  /**
   * @param name the name to set
   */
  protected void setName(final String name)
  {
    this.name = name;
  }

  /**
   * @return the baliseProviderThread
   */
  protected AbstractProvidersService.BaliseProviderThread getBaliseProviderThread()
  {
    return baliseProviderThread;
  }

  /**
   * @param baliseProviderThread the baliseProviderThread to set
   */
  protected void setBaliseProviderThread(final AbstractProvidersService.BaliseProviderThread baliseProviderThread)
  {
    this.baliseProviderThread = baliseProviderThread;
  }

  /**
   * @return the balisesUpdatePeriod
   */
  protected long getBalisesUpdatePeriod()
  {
    return balisesUpdatePeriod;
  }

  /**
   * @param balisesUpdatePeriod the balisesUpdatePeriod to set
   */
  protected void setBalisesUpdatePeriod(final long balisesUpdatePeriod)
  {
    this.balisesUpdatePeriod = balisesUpdatePeriod;
  }

  /**
   * @return the adjustRelevesUpdate
   */
  protected boolean isAdjustRelevesUpdate()
  {
    return adjustRelevesUpdate;
  }

  /**
   * @param adjustRelevesUpdate the adjustRelevesUpdate to set
   */
  protected void setAdjustRelevesUpdate(final boolean adjustRelevesUpdate)
  {
    this.adjustRelevesUpdate = adjustRelevesUpdate;
  }

  /**
   * @return the relevesUpdatePeriod
   */
  protected long getRelevesUpdatePeriod()
  {
    return relevesUpdatePeriod;
  }

  /**
   * @param relevesUpdatePeriod the relevesUpdatePeriod to set
   */
  protected void setRelevesUpdatePeriod(final long relevesUpdatePeriod)
  {
    this.relevesUpdatePeriod = relevesUpdatePeriod;
  }

  /**
   * @param lastBalisesUpdateDate the lastBalisesUpdateDate to set
   */
  protected void setLastBalisesUpdateDate(final long lastBalisesUpdateDate)
  {
    this.lastBalisesUpdateDate = lastBalisesUpdateDate;
  }

  /**
   * @param lastBalisesUpdateLocalDate the lastBalisesUpdateLocalDate to set
   */
  protected void setLastBalisesUpdateLocalDate(final long lastBalisesUpdateLocalDate)
  {
    this.lastBalisesUpdateLocalDate = lastBalisesUpdateLocalDate;
  }

  /**
   * @param balisesException the balisesException to set
   */
  protected void setBalisesException(final Throwable balisesException)
  {
    this.balisesException = balisesException;
  }

  /**
   * @param lastRelevesUpdateDate the lastRelevesUpdateDate to set
   */
  protected void setLastRelevesUpdateDate(final long lastRelevesUpdateDate)
  {
    this.lastRelevesUpdateDate = lastRelevesUpdateDate;
  }

  /**
   * @param lastRelevesUpdateLocalDate the lastRelevesUpdateLocalDate to set
   */
  protected void setLastRelevesUpdateLocalDate(final long lastRelevesUpdateLocalDate)
  {
    this.lastRelevesUpdateLocalDate = lastRelevesUpdateLocalDate;
  }

  /**
   * @param relevesException the relevesException to set
   */
  protected void setRelevesException(final Throwable relevesException)
  {
    this.relevesException = relevesException;
  }

  /**
   * @return the lastBalisesCheckLocalDate
   */
  public long getLastBalisesCheckLocalDate()
  {
    return lastBalisesCheckLocalDate;
  }

  /**
   * @param lastBalisesCheckLocalDate the lastBalisesCheckLocalDate to set
   */
  protected void setLastBalisesCheckLocalDate(final long lastBalisesCheckLocalDate)
  {
    this.lastBalisesCheckLocalDate = lastBalisesCheckLocalDate;
  }

  /**
   * @return the lastRelevesCheckLocalDate
   */
  public long getLastRelevesCheckLocalDate()
  {
    return lastRelevesCheckLocalDate;
  }

  /**
   * @param lastRelevesCheckLocalDate the lastRelevesCheckLocalDate to set
   */
  protected void setLastRelevesCheckLocalDate(final long lastRelevesCheckLocalDate)
  {
    this.lastRelevesCheckLocalDate = lastRelevesCheckLocalDate;
  }

  /**
   * @param relevesAdjusted the relevesAdjusted to set
   */
  protected void setRelevesAdjusted(final boolean relevesAdjusted)
  {
    this.relevesAdjusted = relevesAdjusted;
  }

  /**
   * @return the relevesAdjusted
   */
  protected boolean isRelevesAdjusted()
  {
    return relevesAdjusted;
  }

  /**
   * @param previousRelevesUpdateDate the previousRelevesUpdateDate to set
   */
  protected void setPreviousRelevesUpdateDate(final long previousRelevesUpdateDate)
  {
    this.previousRelevesUpdateDate = previousRelevesUpdateDate;
  }

  /**
   * @return the previousRelevesUpdateDate
   */
  protected long getPreviousRelevesUpdateDate()
  {
    return previousRelevesUpdateDate;
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

  /**
   * @return the activeModes
   */
  public List<BaliseProviderMode> getActiveModes()
  {
    return activeModes;
  }
}
