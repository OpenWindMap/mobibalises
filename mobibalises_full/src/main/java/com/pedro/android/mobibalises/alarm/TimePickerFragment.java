package com.pedro.android.mobibalises.alarm;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.widget.TimePicker;

/**
 * 
 * @author pedro.m
 */
public class TimePickerFragment extends DialogFragment implements OnTimeSetListener
{
  private final OnTimeSetListener listener;
  private int                     hourOfDay;
  private int                     minute;

  /**
   * 
   * @param hourOfDay
   * @param minute
   */
  public TimePickerFragment(final OnTimeSetListener listener, final int hourOfDay, final int minute)
  {
    super();
    this.listener = listener;
    this.hourOfDay = hourOfDay;
    this.minute = minute;
  }

  @Override
  public Dialog onCreateDialog(final Bundle savedInstanceState)
  {
    // Dialog
    return new TimePickerDialog(getActivity(), this, hourOfDay, minute, DateFormat.is24HourFormat(getActivity()));
  }

  @Override
  public void onTimeSet(final TimePicker view, final int inHourOfDay, final int inMinute)
  {
    hourOfDay = inHourOfDay;
    minute = inMinute;
    if (listener != null)
    {
      listener.onTimeSet(view, inHourOfDay, inMinute);
    }
  }
}
