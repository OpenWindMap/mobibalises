package org.pedro.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * 
 * @author pedro.m
 */
public class ReadManyWriteSingleLock implements ReadWriteLock
{
  private final ReadLock             readLock       = new ReadLock(this);
  private final WriteLock            writeLock      = new WriteLock(this);

  private final Map<Thread, Integer> readingThreads = new HashMap<Thread, Integer>();

  private int                        writeAccesses  = 0;
  private int                        writeRequests  = 0;
  private Thread                     writingThread  = null;

  /**
   * 
   * @author pedro.m
   */
  private static class ReadLock implements Lock
  {
    private final ReadManyWriteSingleLock locker;

    /**
     * 
     * @param locker
     */
    ReadLock(final ReadManyWriteSingleLock locker)
    {
      this.locker = locker;
    }

    @Override
    public void lock()
    {
      locker.lockRead();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException
    {
      locker.lockReadInterruptibly();
    }

    @Override
    public boolean tryLock()
    {
      return locker.canGrantReadAccess(Thread.currentThread());
    }

    @Override
    public boolean tryLock(final long time, final TimeUnit unit) throws InterruptedException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void unlock()
    {
      locker.unlockRead();
    }

    @Override
    public Condition newCondition()
    {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class WriteLock implements Lock
  {
    private final ReadManyWriteSingleLock locker;

    /**
     * 
     * @param locker
     */
    WriteLock(final ReadManyWriteSingleLock locker)
    {
      this.locker = locker;
    }

    @Override
    public void lock()
    {
      locker.lockWrite();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException
    {
      locker.lockWriteInterruptibly();
    }

    @Override
    public boolean tryLock()
    {
      return locker.canGrantWriteAccess(Thread.currentThread());
    }

    @Override
    public boolean tryLock(final long time, final TimeUnit unit) throws InterruptedException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void unlock()
    {
      locker.unlockWrite();
    }

    @Override
    public Condition newCondition()
    {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * 
   */
  void lockRead()
  {
    final Thread callingThread = Thread.currentThread();

    synchronized (this)
    {
      while (!canGrantReadAccess(callingThread))
      {
        try
        {
          wait();
        }
        catch (final InterruptedException ie)
        {
          Thread.currentThread().interrupt();
          ie.printStackTrace(System.err);
        }
      }

      readingThreads.put(callingThread, Integer.valueOf(getReadAccessCount(callingThread) + 1));
    }
  }

  /**
   * 
   * @throws InterruptedException
   */
  void lockReadInterruptibly() throws InterruptedException
  {
    final Thread callingThread = Thread.currentThread();

    synchronized (this)
    {
      if (callingThread.isInterrupted())
      {
        throw new InterruptedException("Can't acquire read lock, Thread is already interrupted");
      }

      while (!canGrantReadAccess(callingThread) && !callingThread.isInterrupted())
      {
        wait();
      }

      if (!callingThread.isInterrupted())
      {
        readingThreads.put(callingThread, Integer.valueOf(getReadAccessCount(callingThread) + 1));
      }
      else
      {
        throw new InterruptedException("Can't acquire read lock, Thread has been interrupted while waiting for lock");
      }
    }
  }

  /**
   * 
   * @param callingThread
   * @return
   */
  boolean canGrantReadAccess(final Thread callingThread)
  {
    if (isWriter(callingThread))
    {
      return true;
    }

    if (hasWriter())
    {
      return false;
    }

    if (isReader(callingThread))
    {
      return true;
    }

    if (hasWriteRequests())
    {
      return false;
    }

    return true;
  }

  /**
   * 
   */
  void unlockRead()
  {
    final Thread callingThread = Thread.currentThread();

    synchronized (this)
    {
      if (!isReader(callingThread))
      {
        return;
      }

      final int accessCount = getReadAccessCount(callingThread);
      if (accessCount == 1)
      {
        readingThreads.remove(callingThread);
      }
      else
      {
        readingThreads.put(callingThread, Integer.valueOf(accessCount - 1));
      }

      notifyAll();
    }
  }

  /**
   * 
   */
  void lockWrite()
  {
    final Thread callingThread = Thread.currentThread();

    synchronized (this)
    {
      writeRequests++;
      while (!canGrantWriteAccess(callingThread))
      {
        try
        {
          wait();
        }
        catch (final InterruptedException ie)
        {
          Thread.currentThread().interrupt();
          ie.printStackTrace(System.err);
        }
      }
      writeRequests--;
      writeAccesses++;
      writingThread = callingThread;
    }
  }

  /**
   * 
   * @throws InterruptedException
   */
  void lockWriteInterruptibly() throws InterruptedException
  {
    final Thread callingThread = Thread.currentThread();

    synchronized (this)
    {
      if (callingThread.isInterrupted())
      {
        throw new InterruptedException("Can't acquire write lock, Thread is already interrupted");
      }

      writeRequests++;
      while (!canGrantWriteAccess(callingThread) && !callingThread.isInterrupted())
      {
        wait();
      }

      if (!callingThread.isInterrupted())
      {
        writeRequests--;
        writeAccesses++;
        writingThread = callingThread;
      }
      else
      {
        throw new InterruptedException("Can't acquire write lock, Thread has been interrupted while waiting for lock");
      }
    }
  }

  /**
   * 
   */
  void unlockWrite()
  {
    final Thread callingThread = Thread.currentThread();

    synchronized (this)
    {
      if (!isWriter(callingThread))
      {
        return;
      }

      writeAccesses--;
      if (writeAccesses == 0)
      {
        writingThread = null;
      }

      notifyAll();
    }
  }

  /**
   * 
   * @param callingThread
   * @return
   */
  boolean canGrantWriteAccess(final Thread callingThread)
  {
    if (isOnlyReader(callingThread))
    {
      return true;
    }

    if (hasReaders())
    {
      return false;
    }

    if (writingThread == null)
    {
      return true;
    }

    if (!isWriter(callingThread))
    {
      return false;
    }

    return true;
  }

  /**
   * 
   * @param callingThread
   * @return
   */
  private int getReadAccessCount(final Thread callingThread)
  {
    final Integer accessCount = readingThreads.get(callingThread);
    if (accessCount == null)
    {
      return 0;
    }

    return accessCount.intValue();
  }

  /**
   * 
   * @return
   */
  private boolean hasReaders()
  {
    return (readingThreads.size() > 0);
  }

  /**
   * 
   * @param callingThread
   * @return
   */
  private boolean isReader(final Thread callingThread)
  {
    return (readingThreads.get(callingThread) != null);
  }

  /**
   * 
   * @param callingThread
   * @return
   */
  private boolean isOnlyReader(final Thread callingThread)
  {
    return (readingThreads.size() == 1) && (readingThreads.get(callingThread) != null);
  }

  /**
   * 
   * @return
   */
  private boolean hasWriter()
  {
    return (writingThread != null);
  }

  /**
   * 
   * @param callingThread
   * @return
   */
  private boolean isWriter(final Thread callingThread)
  {
    return (writingThread == callingThread);
  }

  /**
   * 
   * @return
   */
  private boolean hasWriteRequests()
  {
    return (this.writeRequests > 0);
  }

  @Override
  public Lock readLock()
  {
    return readLock;
  }

  @Override
  public Lock writeLock()
  {
    return writeLock;
  }
}
