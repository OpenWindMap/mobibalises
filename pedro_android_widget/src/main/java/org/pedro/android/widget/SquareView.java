package org.pedro.android.widget;

import android.content.Context;
import android.view.View;

/**
 * 
 * @author pedro.m
 */
public class SquareView extends View
{
  public static final int MODE_ADJUST_WIDTH  = 0;
  public static final int MODE_ADJUST_HEIGHT = 1;

  private int             mode;

  /**
   * 
   * @param context
   */
  public SquareView(final Context context)
  {
    super(context);
  }

  /**
   * 
   * @param mode
   */
  public void setMode(final int mode)
  {
    this.mode = mode;
  }

  @Override
  public void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec)
  {
    switch (mode)
    {
      case MODE_ADJUST_HEIGHT:
        final int width = getDefaultSize(getMeasuredWidth(), widthMeasureSpec);
        setMeasuredDimension(width, width);
        break;
      case MODE_ADJUST_WIDTH:
      default:
        final int height = getDefaultSize(getMeasuredHeight(), heightMeasureSpec);
        setMeasuredDimension(height, height);
        break;
    }
  }
}
