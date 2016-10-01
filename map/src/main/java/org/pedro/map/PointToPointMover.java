package org.pedro.map;

import org.pedro.utils.ThreadUtils;

/**
 * 
 * @author pedro.m
 */
public final class PointToPointMover extends Thread implements MapMover
{
  private static final long   DURATION     = 1500;
  private static final long   PITCH        = 50;
  private static final float  MAX_SPEED    = 2f;         // px/ms

  private final Point         currentPoint = new Point();
  private final Point         endPoint     = new Point();
  private float               maxSpeed;
  private float               currentSpeed;

  private boolean             move;
  private final MapController controller;
  private final Projection    projection;

  /**
   * 
   * @param controller
   */
  public PointToPointMover(final MapController controller, final Projection projection)
  {
    super(PointToPointMover.class.getName());
    this.controller = controller;
    this.projection = projection;
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
      int remainingX = 1;
      int remainingY = 1;
      while (move && ((remainingX != 0) || (remainingY != 0)) && !isInterrupted())
      {
        // Petite attente
        try
        {
          Thread.sleep(PITCH);
        }
        catch (final InterruptedException ie)
        {
          Thread.currentThread().interrupt();
          return;
        }

        // Calculs
        remainingX = endPoint.x - currentPoint.x;
        remainingY = endPoint.y - currentPoint.y;
        final double remainingDistance = Math.sqrt(remainingX * remainingX + remainingY * remainingY);

        final float xFactor = (float)(remainingX / remainingDistance);
        final float yFactor = (float)(remainingY / remainingDistance);

        now = System.currentTimeMillis();
        final double distance = currentSpeed * (now - last);
        if (distance < remainingDistance / 4)
        {
          // Acceleration
          currentSpeed = Math.min(maxSpeed, currentSpeed * 2);
        }
        else
        {
          // Decceleration
          currentSpeed = Math.max(0, currentSpeed / 2);
        }
        final int deltaX = Math.round(currentSpeed * (now - last) * xFactor);
        final int deltaY = Math.round(currentSpeed * (now - last) * yFactor);
        if ((deltaX == 0) && (deltaY == 0))
        {
          break;
        }

        if (!isInterrupted())
        {
          // Scroll
          controller.scrollBy(-deltaX, -deltaY);
        }

        // Next
        last = now;
        currentPoint.translate(deltaX, deltaY);
      }

      // RAZ
      move = false;

      // Fin du move
      controller.onMoveInputFinished();
    }
  }

  /**
   * 
   * @param startGeoPoint
   * @param endGeoPoint
   */
  public void initialize(final GeoPoint startGeoPoint, final GeoPoint endGeoPoint)
  {
    projection.toPixels(startGeoPoint, currentPoint);
    projection.toPixels(endGeoPoint, endPoint);
    final int dx = endPoint.x - currentPoint.x;
    final int dy = endPoint.y - currentPoint.y;
    final double distance = Math.sqrt(dx * dx + dy * dy);
    maxSpeed = Math.min(MAX_SPEED, 2 * (float)(distance / DURATION));
    currentSpeed = 0.01f;
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
