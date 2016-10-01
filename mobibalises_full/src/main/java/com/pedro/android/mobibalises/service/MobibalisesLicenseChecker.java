package com.pedro.android.mobibalises.service;

import android.content.Context;

import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.Policy;

/**
 * 
 * @author pedro.m
 */
public class MobibalisesLicenseChecker extends LicenseChecker
{
  private final LicenseCheckerCallback callback;
  private int                          tries = 0;

  /**
   * 
   * @param context
   * @param policy
   * @param encodedPublicKey
   * @param callback
   */
  public MobibalisesLicenseChecker(final Context context, final Policy policy, final String encodedPublicKey, final LicenseCheckerCallback callback)
  {
    super(context, policy, encodedPublicKey);
    this.callback = callback;
  }

  /**
   * 
   * @return
   */
  public LicenseCheckerCallback getCallback()
  {
    return callback;
  }

  /**
   * 
   */
  public void checkAccess()
  {
    tries++;
    checkAccess(callback);
  }

  /**
   * 
   * @return
   */
  public int getTries()
  {
    return tries;
  }
}
