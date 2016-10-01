package org.pedro.map;

import org.pedro.utils.ThreadUtils;

/**
 * 
 * @author pedro.m
 */
public final class FadeOutMover extends Thread implements MapMover
{
  private static final long   FADE_OUT_DURATION = 750;
  private static final long   FADE_OUT_PITCH    = 50;

  private float               xSpeed;
  private float               ySpeed;

  private boolean             move;
  private final MapController controller;

  /**
   * 
   * @param controller
   */
  public FadeOutMover(final MapController controller)
  {
    super(FadeOutMover.class.getName());
    this.controller = controller;
  }

  @Override
  public void run()
  {
    while (!isInterrupted())
    {
      // Attente d'un notification et d'un debut de mouvement demande
      synchronized (this)
      {
        while (!move && !isInterrupted())
        {
          try
          {
            wait();
          }
          catch (final InterruptedException ie)
          {
            Thread.currentThread().interrupt();
            return;
          }
        }
      }

      final long moveStart = System.currentTimeMillis();
      long now = moveStart;
      long last = now;
      while (move && (now - moveStart < FADE_OUT_DURATION) && !isInterrupted())
      {
        // Pourcentage du temps restant
        now = System.currentTimeMillis();
        final float pourcent = Math.max(0, 1 - (float)(now - moveStart) / FADE_OUT_DURATION);
        final float currentXSpeed = pourcent * xSpeed * (now - last);
        final float currentYSpeed = pourcent * ySpeed * (now - last);

        // Scroll
        if (!isInterrupted())
        {
          // Translation
          controller.scrollBy((int)currentXSpeed, (int)currentYSpeed);

          // Petite attente
          try
          {
            Thread.sleep(FADE_OUT_PITCH);
          }
          catch (final InterruptedException ie)
          {
            Thread.currentThread().interrupt();
            return;
          }

          // Next
          last = now;
        }
      }

      // RAZ
      move = false;
      xSpeed = 0;
      ySpeed = 0;

      // Fin du move
      controller.onMoveInputFinished();
    }
  }

  /**
   * 
   * @param initialXSpeed
   * @param initialYSpeed
   */
  public void initialize(final float initialXSpeed, final float initialYSpeed)
  {
    // La vitesse est donnee en pixel/s, conversion en pixel/ms
    xSpeed = initialXSpeed / 1000;
    ySpeed = initialYSpeed / 1000;
  }

  @Override
  public boolean isMoveFinished()
  {
    return move;
  }

  @Override
  public void startMove()
  {
    synchronized (this)
    {
      move = true;
      notify();
    }
  }

  @Override
  public void stopMove()
  {
    move = false;
    controller.onMoveInputFinished();
  }

  @Override
  public void shutdown()
  {
    interrupt();
    ThreadUtils.join(this);
  }
}
