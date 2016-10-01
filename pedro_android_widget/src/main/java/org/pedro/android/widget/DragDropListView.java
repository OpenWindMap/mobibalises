package org.pedro.android.widget;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.WrapperListAdapter;

/**
 * 
 * @author pedro.m
 */
public class DragDropListView<ItemType> extends ListView implements OnItemLongClickListener
{
  // Constantes
  private static final String                     METHOD_IS_SCROLLBAR_FADING_ENABLED = "isScrollbarFadingEnabled";
  private static final String                     METHOD_AWAKEN_SCROLL_BARS          = "awakenScrollBars";
  private static final int                        BUILD_VERSION_CODES_FROYO          = 8;

  // DragView
  private final WindowManager                     windowManager;
  private Bitmap                                  dragBitmap;
  private ImageView                               dragView;
  private int                                     dragViewWidth;
  private int                                     dragViewHeight;
  private static final WindowManager.LayoutParams initialLayoutParams;
  private ItemType                                dragItem;

  // Infos graphiques
  private int                                     dividerHeight;
  private int                                     listTop;
  private int                                     listBottom;
  private int                                     appliTop;

  // Indicateurs
  private boolean                                 dragging                           = false;
  private boolean                                 draggable                          = true;
  private int                                     dragPosition;
  private int                                     dropPosition                       = INVALID_POSITION;

  // Positions
  private int                                     downX                              = Integer.MIN_VALUE;
  private int                                     lastX                              = Integer.MIN_VALUE;
  private int                                     lastY                              = Integer.MIN_VALUE;
  private int                                     dragDeltaY                         = 0;
  private View                                    lastOverView                       = null;
  private int                                     lastPaddingTop                     = 0;

  // Gestion du scroll
  private int                                     scrollDelta                        = 0;
  private int                                     scrollFirst                        = -1;
  private int                                     scrollDecal                        = Integer.MIN_VALUE;
  private final ScrollThread                      scrollThread;
  final ScrollHandler                             scrollHandler;

  // Client
  private DragDropListener                        dragDropListener;

  // Introspection
  private Method                                  isScrollbarFadingEnabledMethod;
  private Method                                  awakenScrollBarsMethod;

  /**
   * 
   * @author pedro.m
   */
  private static class ScrollThread extends Thread
  {
    private static final int SCROLL_THREAD_SLEEP = 50;

    private ScrollHandler    threadScrollHandler;
    private int              scrollAmount        = 0;
    private final Object     scrollLock          = new Object();

    /**
     * 
     * @param scrollHandler
     */
    ScrollThread(final ScrollHandler scrollHandler)
    {
      super(ScrollThread.class.getName());
      this.threadScrollHandler = scrollHandler;
    }

    @Override
    public void run()
    {
      // Tant que le thread n'est pas interrompu
      while (!isInterrupted())
      {
        // Tant que le scroll demande est null
        synchronized (this)
        {
          while (!isInterrupted() && (scrollAmount == 0))
          {
            try
            {
              wait();
            }
            catch (final InterruptedException ie)
            {
              interrupt();
            }
          }
        }

        // Si non interrompu
        while (!isInterrupted())
        {
          synchronized (scrollLock)
          {
            if (scrollAmount == 0)
            {
              break;
            }

            final Message message = new Message();
            message.arg1 = scrollAmount;
            threadScrollHandler.sendMessage(message);
          }
          try
          {
            Thread.sleep(SCROLL_THREAD_SLEEP);
          }
          catch (final InterruptedException ie)
          {
            interrupt();
          }
        }
      }

      // Fin
      threadScrollHandler = null;
    }

    /**
     * 
     * @param amount
     */
    void setScrollAmount(final int amount)
    {
      synchronized (scrollLock)
      {
        scrollAmount = amount;
      }
      synchronized (this)
      {
        this.notify();
      }
    }
  }

  /**
   * 
   * @author pedro.m
   */
  public interface DragDropListener
  {
    /**
     * 
     * @param from
     */
    public void onDrag(final int from);

    /**
     * 
     * @param from
     * @param to
     */
    public void onDrop(final int from, final int to);

    /**
     * 
     * @param index
     */
    public void onDelete(final int index);
  }

  /**
   * 
   * @author pedro.m
   */
  private static class ScrollHandler extends Handler
  {
    private DragDropListView<?> dragDropListView;

    /**
     * 
     * @param dragDropListView
     */
    ScrollHandler(final DragDropListView<?> dragDropListView)
    {
      this.dragDropListView = dragDropListView;
    }

    @Override
    public void handleMessage(final Message msg)
    {
      dragDropListView.scrollY(msg.arg1);
    }

    /**
     * 
     */
    void onShutdown()
    {
      dragDropListView = null;
    }
  }

  /**
   * 
   */
  static
  {
    // Divers
    initialLayoutParams = new WindowManager.LayoutParams();
    initialLayoutParams.gravity = Gravity.TOP | Gravity.LEFT;
    initialLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
    initialLayoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
    initialLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
    initialLayoutParams.format = PixelFormat.TRANSLUCENT;
    initialLayoutParams.windowAnimations = 0;
  }

  /**
   * 
   * @param context
   * @param attrs
   */
  public DragDropListView(final Context context, final AttributeSet attrs)
  {
    this(context, attrs, 0);
  }

  /**
   * 
   * @param context
   * @param attrs
   * @param defStyle
   */
  public DragDropListView(final Context context, final AttributeSet attrs, final int defStyle)
  {
    super(context, attrs, defStyle);

    this.scrollHandler = new ScrollHandler(this);
    this.scrollThread = new ScrollThread(scrollHandler);

    super.setOnItemLongClickListener(this);
    windowManager = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);

    try
    {
      isScrollbarFadingEnabledMethod = View.class.getMethod(METHOD_IS_SCROLLBAR_FADING_ENABLED);
      awakenScrollBarsMethod = View.class.getDeclaredMethod(METHOD_AWAKEN_SCROLL_BARS);
    }
    catch (final SecurityException se)
    {
      // Nothing
    }
    catch (final NoSuchMethodException nsme)
    {
      // Nothing
    }

    // Demarrage du thread de scroll
    scrollThread.start();
  }

  @Override
  public void onDetachedFromWindow()
  {
    // Super
    super.onDetachedFromWindow();

    // Arret du Thread
    scrollThread.interrupt();
    scrollHandler.onShutdown();
  }

  @Override
  public final void setOnItemLongClickListener(final OnItemLongClickListener listener)
  {
    throw new RuntimeException("OnItemLongClickListener is not supported with DragDropListView");
  }

  @Override
  public boolean onItemLongClick(final AdapterView<?> parentView, final View view, final int position, final long id)
  {
    // Verification du mode et plusieurs elements (si 1 seul element, le drag & drop n'a pas de sens)
    if (!draggable || (getAdapter().getCount() <= 0))
    {
      return true;
    }

    // Debut du drag
    startDragging(view, position);

    return true;
  }

  /**
   * 
   * @param y
   * @return
   */
  boolean scrollY(final int y)
  {
    // Verification
    final View childView = getChildAt(0);
    if (childView == null)
    {
      return false;
    }

    // Scrolling
    final int first = getFirstVisiblePosition();
    final int decal = childView.getTop();
    setSelectionFromTop(first, decal - y);

    // Changement ?
    final boolean scrolled = ((scrollFirst != first) || (scrollDecal != decal));

    // Affichage des scrollbars si necessaire
    // Code en instrospection pour compatibilite avec SDK 4 (1.6)
    if (Build.VERSION.SDK_INT >= BUILD_VERSION_CODES_FROYO)
    {
      /* Code "normal"
      if (isScrollbarFadingEnabled() && scrolled)
      {
        awakenScrollBars();
      }
      */
      try
      {
        if (isScrollbarFadingEnabledMethod != null)
        {
          final Boolean isScrollbarFadingEnabledBoolean = (Boolean)isScrollbarFadingEnabledMethod.invoke(this);
          final boolean isScrollbarFadingEnabled = (isScrollbarFadingEnabledBoolean == null ? false : isScrollbarFadingEnabledBoolean.booleanValue());
          if (isScrollbarFadingEnabled && scrolled && (awakenScrollBarsMethod != null))
          {
            awakenScrollBarsMethod.invoke(this);
          }
        }
      }
      catch (final IllegalArgumentException iae)
      {
        //Nothing
      }
      catch (final IllegalAccessException iae)
      {
        //Nothing
      }
      catch (final InvocationTargetException ite)
      {
        //Nothing
      }
    }

    // Sauvegarde
    scrollFirst = first;
    scrollDecal = decal;

    return scrolled;
  }

  @Override
  public boolean onInterceptTouchEvent(final MotionEvent ev)
  {
    return true;
  }

  @Override
  public boolean onTouchEvent(final MotionEvent ev)
  {
    switch (ev.getAction())
    {
      case MotionEvent.ACTION_DOWN:
        lastX = (int)ev.getX();
        downX = lastX;
        lastY = (int)ev.getY();
        break;
      case MotionEvent.ACTION_UP:
        if (dragging)
        {
          stopDragging();
          drop();
        }
        break;
      case MotionEvent.ACTION_MOVE:
        if (dragging)
        {
          continueDragging((int)ev.getX(), (int)ev.getY());
        }
        break;
    }

    return super.onTouchEvent(ev);
  }

  /**
   * 
   * @param bitmap
   */
  private void startDragging(final View view, final int position)
  {
    // Nettoyage eventuel
    stopDragging();

    // Verifications
    final ArrayAdapter<ItemType> adapter = getArrayAdapter();
    if ((position >= adapter.getCount()) || (position < 0))
    {
      return;
    }

    // Sauvegarde indicateurs et donnees
    dragging = true;
    dragPosition = position;
    appliTop = ((View)getParent()).getTop(); // Haut de l'appli r/r a l'ecran
    listTop = getTop(); // Haut de la liste r/r a l'ecran
    listBottom = getBottom(); // Bas de la liste r/r a l'ecran
    dragDeltaY = lastY - view.getTop(); // Position relative du touch r/r a l'item
    dividerHeight = getDividerHeight();

    // Capture de l'image
    view.setDrawingCacheEnabled(true);
    dragBitmap = Bitmap.createBitmap(view.getDrawingCache());
    dragViewWidth = dragBitmap.getWidth();
    dragViewHeight = dragBitmap.getHeight();

    // Creation et ajout de la vue
    final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
    layoutParams.copyFrom(initialLayoutParams);
    layoutParams.x = 0;
    layoutParams.y = getAbsoluteWindowY(lastY);
    dragView = new ImageView(getContext());
    dragView.setImageBitmap(dragBitmap);
    windowManager.addView(dragView, layoutParams);

    // Retrait de l'objet "source"
    dragItem = adapter.getItem(dragPosition);
    adapter.remove(dragItem);

    // Notification
    if (dragDropListener != null)
    {
      dragDropListener.onDrag(position);
    }

    // Expansion de l'element suivant (= l'indice du courant qui vient d'etre retire)
    dropPosition = position;
    expand();
  }

  /**
   * 
   * @param x
   * @param y
   */
  private void continueDragging(final int x, final int y)
  {
    // Au meme endroit => rien a faire
    if ((x == lastX) && (y == lastY))
    {
      return;
    }

    // Sauvegarde de la position
    lastX = x;
    lastY = y;

    // Ajustement de la vue de l'item drag
    final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
    layoutParams.copyFrom(initialLayoutParams);
    layoutParams.x = getAbsoluteWindowX(lastX);
    final int newY = getAbsoluteWindowY(lastY);
    layoutParams.y = Math.min(Math.max(newY, listTop + 1), listBottom + 20 - dragViewHeight);
    windowManager.updateViewLayout(dragView, layoutParams);

    // Gestion de l'item survole
    manageOver(x, y, layoutParams.y);

    // Gestion du scroll
    manageScroll(newY - layoutParams.y);

    // Gestion de l'item survole
    manageOver(x, y, layoutParams.y);
  }

  /**
   * 
   * @param x
   * @return
   */
  private int getAbsoluteWindowX(final int x)
  {
    if (isDeleteMode(x))
    {
      return (x > downX ? 1 : -1) * 3 * dragViewWidth / 4;
    }

    return 0;
  }

  /**
   * 
   * @param x
   * @return
   */
  private boolean isDeleteMode(final int x)
  {
    if (x > downX + dragViewWidth / 3)
    {
      return true;
    }
    else if (x < downX - dragViewWidth / 3)
    {
      return true;
    }

    return false;
  }

  /**
   * 
   * @param y
   * @return
   */
  private int getAbsoluteWindowY(final int y)
  {
    return appliTop + y - dragDeltaY + dividerHeight;
  }

  /**
   * 
   * @param x
   * @param y
   * @param dragViewTop
   */
  private void manageOver(final int x, final int y, final int dragViewTop)
  {
    // Suppression ?
    if (isDeleteMode(x))
    {
      dropPosition = INVALID_POSITION;
      collapse();
      return;
    }

    // Item survole
    final int overPosition = getOverPosition(x, y, dragViewTop);
    if (overPosition == INVALID_POSITION)
    {
      return;
    }

    // Item survole inchange => retour
    if (dropPosition == overPosition)
    {
      return;
    }

    // Gestion de l'espacement
    dropPosition = overPosition;
    expand();
  }

  /**
   * 
   * @param x
   * @param y
   * @param dragViewTop
   * @return
   */
  private int getOverPosition(final int x, final int y, final int dragViewTop)
  {
    int overPosition = pointToPosition(x, y + dragViewHeight - dragDeltaY);
    if (overPosition == INVALID_POSITION)
    {
      // Est-on a la fin ?
      if ((getChildCount() > 0) && (dragViewTop > getChildAt(getChildCount() - 1).getTop() + listTop + dividerHeight))
      {
        overPosition = getAdapter().getCount();
      }
    }

    return overPosition;
  }

  /**
   * 
   */
  private void expand()
  {
    // Reduction prealable de l'item etendu
    collapse();

    // Extension de la vue
    final int expandPosition = dropPosition - getFirstVisiblePosition();
    if (expandPosition < (getChildCount() - getFooterViewsCount()))
    {
      final View overView = getChildAt(expandPosition);
      if (overView != null)
      {
        lastOverView = overView;
        final int padding = dragViewHeight + dividerHeight;
        lastPaddingTop = overView.getPaddingTop();
        lastOverView.setPadding(getPaddingLeft(), padding, getPaddingRight(), getPaddingBottom());
      }
    }
  }

  /**
   * 
   */
  private void collapse()
  {
    if (lastOverView != null)
    {
      lastOverView.setPadding(getPaddingLeft(), lastPaddingTop, getPaddingRight(), getPaddingBottom());
      lastOverView = null;
    }
  }

  /**
   * 
   */
  private void stopDragging()
  {
    // Reduction
    collapse();

    // Arret du scroll
    manageScroll(0);

    // Arret du drag
    dragging = false;

    // Liberation resources
    if (dragView != null)
    {
      windowManager.removeView(dragView);
      dragView.setImageDrawable(null);
      dragView = null;
    }
    if (dragBitmap != null)
    {
      dragBitmap.recycle();
      dragBitmap = null;
    }
  }

  /**
   * 
   */
  private void drop()
  {
    // Mode suppression ?
    if (isDeleteMode(lastX))
    {
      if (dragDropListener != null)
      {
        dragDropListener.onDelete(dragPosition);
      }
      return;
    }

    // Initialisations
    final ArrayAdapter<ItemType> adapter = getArrayAdapter();

    // Insertion de l'objet a sa nouvelle position
    if ((dropPosition >= adapter.getCount()) || (dropPosition == -1))
    {
      adapter.add(dragItem);
    }
    else
    {
      adapter.insert(dragItem, dropPosition);
    }

    // Listener
    if (dragDropListener != null)
    {
      dragDropListener.onDrop(dragPosition, dropPosition);
    }

    // Fin
    dropPosition = INVALID_POSITION;
  }

  /**
   * 
   * @return
   */
  @SuppressWarnings("unchecked")
  private ArrayAdapter<ItemType> getArrayAdapter()
  {
    // Initialisations
    final Adapter adapter = getAdapter();

    // Si null
    if (adapter == null)
    {
      return null;
    }

    // Si wrapper, on retourne l'adapter wrappe
    if (WrapperListAdapter.class.isAssignableFrom(adapter.getClass()))
    {
      return (ArrayAdapter<ItemType>)((WrapperListAdapter)adapter).getWrappedAdapter();
    }

    // Sinon directement l'adapter
    return (ArrayAdapter<ItemType>)getAdapter();
  }

  /**
   * 
   * @param delta
   */
  private void manageScroll(final int delta)
  {
    if (delta == scrollDelta)
    {
      return;
    }
    scrollDelta = delta;

    int scrollAmount = delta;
    if (delta > 0)
    {
      scrollAmount = 5 * (int)Math.floor((float)delta / 5);
    }
    else if (delta < 0)
    {
      scrollAmount = -5 * (int)Math.floor((float)(-delta) / 5);
    }
    scrollThread.setScrollAmount(scrollAmount);
  }

  /**
   * @param draggable the draggable to set
   */
  public void setDraggable(final boolean draggable)
  {
    this.draggable = draggable;
  }

  /**
   * @return the draggable
   */
  public boolean isDraggable()
  {
    return draggable;
  }

  /**
   * @return the dragDropListener
   */
  public DragDropListener getDragDropListener()
  {
    return dragDropListener;
  }

  /**
   * @param dragDropListener the dragDropListener to set
   */
  public void setDragDropListener(final DragDropListener dragDropListener)
  {
    this.dragDropListener = dragDropListener;
  }
}
