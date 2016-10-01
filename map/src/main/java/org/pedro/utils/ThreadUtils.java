package org.pedro.utils;

import java.util.List;

/**
 * 
 * @author pedro.m
 *
 */
public abstract class ThreadUtils
{
  /**
   * 
   * @param thread
   * @return true if joined, false if interrupted
   */
  public static boolean join(final Thread thread)
  {
    try
    {
      thread.join();
      return true;
    }
    catch (final InterruptedException ie)
    {
      Thread.currentThread().interrupt();
      thread.interrupt();
      return false;
    }
  }

  /**
   * 
   * @param threads
   * @param interrupt
   * @return
   */
  public static boolean join(final List<Thread> threads, final boolean interrupt)
  {
    synchronized (threads)
    {
      try
      {
        // Attente de la fin de chaque Thread
        for (final Thread thread : threads)
        {
          if (interrupt)
          {
            thread.interrupt();
          }
          try
          {
            thread.join();
          }
          catch (final InterruptedException ie)
          {
            thread.interrupt();
            throw ie;
          }
        }

        // Vidage
        threads.clear();

        return true;
      }
      catch (final InterruptedException ie)
      {
        Thread.currentThread().interrupt();
        return false;
      }
    }
  }

  /**
   * 
   * @param threads
   */
  public static void interrupt(final List<Thread> threads)
  {
    synchronized (threads)
    {
      // Interruption de chaque Thread
      for (final Thread thread : threads)
      {
        thread.interrupt();
      }
    }
  }
}
