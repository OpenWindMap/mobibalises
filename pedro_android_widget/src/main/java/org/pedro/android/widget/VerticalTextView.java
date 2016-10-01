package org.pedro.android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.TextView;

/**
 * 
 * @author pedro.m
 */
public class VerticalTextView extends TextView
{
  private final boolean topDown;

  /**
   * 
   * @param context
   * @param attrs
   */
  public VerticalTextView(final Context context, final AttributeSet attrs)
  {
    super(context, attrs);
    final int gravity = getGravity();
    if (Gravity.isVertical(gravity) && (gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM)
    {
      setGravity((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) | Gravity.TOP);
      topDown = false;
    }
    else
    {
      topDown = true;
    }
  }

  @Override
  protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec)
  {
    super.onMeasure(heightMeasureSpec, widthMeasureSpec);
    setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
  }

  @Override
  protected void onDraw(final Canvas canvas)
  {
    final TextPaint textPaint = getPaint();
    textPaint.setColor(getCurrentTextColor());
    textPaint.drawableState = getDrawableState();

    canvas.save();

    if (topDown)
    {
      canvas.translate(getWidth(), 0);
      canvas.rotate(90);
    }
    else
    {
      canvas.translate(0, getHeight());
      canvas.rotate(-90);
    }

    canvas.translate(getCompoundPaddingLeft(), getExtendedPaddingTop());

    getLayout().draw(canvas);
    canvas.restore();
  }
}
