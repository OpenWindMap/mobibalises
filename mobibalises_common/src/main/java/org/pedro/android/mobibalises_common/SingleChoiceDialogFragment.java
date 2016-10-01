package org.pedro.android.mobibalises_common;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * 
 * @author pedro.m
 */
public class SingleChoiceDialogFragment extends DialogFragment
{
  private Activity                activity;
  final private int               icon;
  final private String            title;
  final private boolean           cancelable;
  final private OnCancelListener  onCancelListener;
  final private String[]          items;
  final private int               selectedItem;
  final private OnClickListener   onClickListener;
  final private String            positiveText;
  final private OnClickListener   positiveClickListener;
  final private String            negativeText;
  final private OnClickListener   negativeClickListener;
  final private String            neutralText;
  final private OnClickListener   neutralClickListener;
  final private OnDismissListener onDismissListener;

  /**
   * 
   * @param activity
   */
  public SingleChoiceDialogFragment(final Activity activity, final int icon, final String title, final String[] items, final int selectedItem, final boolean cancelable, final OnCancelListener onCancelListener,
      final OnClickListener onClickListener, final String positiveText, final OnClickListener positiveClickListener, final String negativeText, final OnClickListener negativeClickListener, final String neutralText,
      final OnClickListener neutralClickListener, final OnDismissListener onDismissListener)
  {
    // Initialisations
    this.activity = activity;
    this.icon = icon;
    this.title = title;
    this.items = items;
    this.selectedItem = selectedItem;
    this.cancelable = cancelable;
    this.onCancelListener = onCancelListener;
    this.onClickListener = onClickListener;
    this.positiveText = positiveText;
    this.positiveClickListener = positiveClickListener;
    this.negativeText = negativeText;
    this.negativeClickListener = negativeClickListener;
    this.neutralText = neutralText;
    this.neutralClickListener = neutralClickListener;
    this.onDismissListener = onDismissListener;
  }

  @Override
  public Dialog onCreateDialog(final Bundle savedInstanceState)
  {
    final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    if (icon > 0)
    {
      builder.setIcon(icon);
    }
    builder.setTitle(title);
    builder.setCancelable(cancelable);
    if (onCancelListener != null)
    {
      builder.setOnCancelListener(onCancelListener);
    }
    builder.setSingleChoiceItems(items, selectedItem, onClickListener);
    if (positiveText != null)
    {
      builder.setPositiveButton(positiveText, positiveClickListener);
    }
    if (negativeText != null)
    {
      builder.setNegativeButton(negativeText, negativeClickListener);
    }
    if (neutralText != null)
    {
      builder.setNeutralButton(neutralText, neutralClickListener);
    }

    final AlertDialog dialog = builder.create();
    if (onDismissListener != null)
    {
      dialog.setOnDismissListener(onDismissListener);
    }

    return dialog;
  }

  @Override
  public void onCancel(final DialogInterface dialog)
  {
    super.onCancel(dialog);
    if (onCancelListener != null)
    {
      onCancelListener.onCancel(dialog);
    }
  }

  @Override
  public void onDismiss(final DialogInterface dialog)
  {
    super.onCancel(dialog);
    if (onDismissListener != null)
    {
      onDismissListener.onDismiss(dialog);
    }
  }

  @Override
  public void onDestroy()
  {
    super.onDestroy();
    activity = null;
  }
}
