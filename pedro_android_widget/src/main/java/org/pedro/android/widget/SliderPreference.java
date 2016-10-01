package org.pedro.android.widget;

import java.text.MessageFormat;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * 
 * @author pedro.m
 */
public final class SliderPreference extends Preference implements SeekBar.OnSeekBarChangeListener
{
  private static final int DEFAULT_DEFAULT_VALUE = 0;
  private static final int DEFAULT_MIN_VALUE     = 0;
  private static final int DEFAULT_MAX_VALUE     = 10;

  private int              currentProgress;
  private int              defValue;
  private int              minValue;
  private int              maxValue;

  private Integer          displayValue;
  private Integer          displayDefValue;
  private Integer          displayMinValue;
  private Integer          displayMaxValue;

  /**
   * 
   * @param context
   */
  public SliderPreference(final Context context)
  {
    this(context, null, 0);
  }

  /**
   * 
   * @param context
   * @param attrs
   */
  public SliderPreference(final Context context, final AttributeSet attrs)
  {
    this(context, attrs, 0);
  }

  /**
   * 
   * @param context
   * @param attrs
   * @param defStyle
   */
  public SliderPreference(final Context context, final AttributeSet attrs, final int defStyle)
  {
    super(context, attrs, defStyle);
    initAttrs(attrs, defStyle);
  }

  /**
   * 
   * @param attrs
   * @param defStyle
   */
  private void initAttrs(final AttributeSet attrs, final int defStyle)
  {
    final TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.SliderPreference, defStyle, 0);
    defValue = array.getInt(R.styleable.SliderPreference_defValue, DEFAULT_DEFAULT_VALUE);
    minValue = array.getInt(R.styleable.SliderPreference_minValue, DEFAULT_MIN_VALUE);
    maxValue = array.getInt(R.styleable.SliderPreference_maxValue, DEFAULT_MAX_VALUE);
    array.recycle();

    // Valeur par defaut
    setDefaultValue(Integer.valueOf(defValue));

    // Pour l'affichage
    displayDefValue = Integer.valueOf(defValue);
    displayMinValue = Integer.valueOf(minValue);
    displayMaxValue = Integer.valueOf(maxValue);
  }

  @Override
  protected View onCreateView(final ViewGroup parent)
  {
    // Initialisation
    final View view = super.onCreateView(parent);

    // Ajout du listener pour la barre de progression
    final SeekBar seekBar = (SeekBar)view.findViewById(R.id.seekbar);
    seekBar.setOnSeekBarChangeListener(null);
    seekBar.setMax(maxValue - minValue);
    seekBar.setOnSeekBarChangeListener(this);

    return view;
  }

  @Override
  protected void onBindView(final View view)
  {
    // Initialisation
    super.onBindView(view);

    // Gestion de la barre de progression
    final View seekBarView = view.findViewById(R.id.seekbar);
    if ((seekBarView != null) && (seekBarView instanceof SeekBar))
    {
      final SeekBar seekBar = (SeekBar)seekBarView;
      seekBar.setProgress(currentProgress);
    }
  }

  /**
   * 
   * @param view
   */
  private void updateTextViews(final View view)
  {
    final TextView textMessage = (TextView)view.findViewById(android.R.id.title);
    if (textMessage != null)
    {
      textMessage.setText(getTitle());
    }

    final TextView textSummary = (TextView)view.findViewById(android.R.id.summary);
    if (textSummary != null)
    {
      textSummary.setText(getSummary());
    }
  }

  /**
   * 
   * @param progress
   * @param persist
   */
  private void setProgress(final int progress, final boolean persist)
  {
    currentProgress = progress;
    displayValue = Integer.valueOf(fromProgress(currentProgress));
    if (persist)
    {
      persistInt(fromProgress(progress));
      notifyChanged();
    }
  }

  /**
   * 
   * @param newValue
   */
  public void setValue(final int newValue)
  {
    setProgress(toProgress(newValue), true);
  }

  /**
   * 
   * @return
   */
  public int getValue()
  {
    return (displayValue == null ? Integer.MIN_VALUE : displayValue.intValue());
  }

  @Override
  public CharSequence getTitle()
  {
    return MessageFormat.format(super.getTitle().toString(), displayValue, displayDefValue, displayMinValue, displayMaxValue);
  }

  @Override
  public CharSequence getSummary()
  {
    if (super.getSummary() == null)
    {
      return null;
    }

    return MessageFormat.format(super.getSummary().toString(), displayValue, displayDefValue, displayMinValue, displayMaxValue);
  }

  @Override
  public void onProgressChanged(final SeekBar inSeekBar, final int progress, final boolean fromUser)
  {
    setProgress(progress, false);
    updateTextViews((View)inSeekBar.getParent().getParent());
  }

  @Override
  public void onStartTrackingTouch(final SeekBar inSeekBar)
  {
    // Nothing
  }

  @Override
  public void onStopTrackingTouch(final SeekBar inSeekBar)
  {
    if (!callChangeListener(Integer.valueOf(fromProgress(inSeekBar.getProgress()))))
    {
      return;
    }

    setProgress(inSeekBar.getProgress(), true);
  }

  @Override
  protected Object onGetDefaultValue(final TypedArray ta, final int index)
  {
    return Integer.valueOf(ta.getInt(index, defValue));
  }

  @Override
  protected void onSetInitialValue(final boolean restoreValue, final Object defaultInitialValue)
  {
    setProgress(toProgress(restoreValue ? getPersistedInt(defValue) : defValue), true);
  }

  /**
   * 
   * @param progress
   * @return
   */
  private int fromProgress(final int progress)
  {
    return progress + minValue;
  }

  /**
   * 
   * @param value
   * @return
   */
  private int toProgress(final int value)
  {
    return value - minValue;
  }

  @Override
  protected Parcelable onSaveInstanceState()
  {
    final Parcelable superState = super.onSaveInstanceState();
    if (isPersistent())
    {
      // No need to save instance state since it's persistent
      return superState;
    }

    final SavedState myState = new SavedState(superState);
    myState.progressState = currentProgress;
    return myState;
  }

  @Override
  protected void onRestoreInstanceState(final Parcelable state)
  {
    if (state == null || !state.getClass().equals(SavedState.class))
    {
      // Didn't save state for us in onSaveInstanceState
      super.onRestoreInstanceState(state);
      return;
    }

    final SavedState myState = (SavedState)state;
    super.onRestoreInstanceState(myState.getSuperState());
    setProgress(myState.progressState, true);
  }

  /**
   * 
   * @author pedro.m
   */
  private static class SavedState extends BaseSavedState
  {
    int progressState;

    public SavedState(final Parcelable superState)
    {
      super(superState);
    }

    public SavedState(final Parcel source)
    {
      super(source);
      progressState = source.readInt();
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags)
    {
      super.writeToParcel(dest, flags);
      dest.writeInt(progressState);
    }

    @SuppressWarnings({ "hiding", "unused" })
    public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>()
                                                               {
                                                                 @Override
                                                                 public SavedState createFromParcel(final Parcel in)
                                                                 {
                                                                   return new SavedState(in);
                                                                 }

                                                                 @Override
                                                                 public SavedState[] newArray(final int size)
                                                                 {
                                                                   return new SavedState[size];
                                                                 }
                                                               };
  }

  /**
   * @param minValue the minValue to set
   */
  public void setMinValue(final int minValue)
  {
    this.minValue = minValue;
  }

  /**
   * @return the minValue
   */
  public int getMinValue()
  {
    return minValue;
  }

  /**
   * @param maxValue the maxValue to set
   */
  public void setMaxValue(final int maxValue)
  {
    this.maxValue = maxValue;
  }

  /**
   * @return the maxValue
   */
  public int getMaxValue()
  {
    return maxValue;
  }

  /**
   * @return the defValue
   */
  public int getDefValue()
  {
    return defValue;
  }

  /**
   * @param defValue the defValue to set
   */
  public void setDefValue(final int defValue)
  {
    this.defValue = defValue;
  }
}
