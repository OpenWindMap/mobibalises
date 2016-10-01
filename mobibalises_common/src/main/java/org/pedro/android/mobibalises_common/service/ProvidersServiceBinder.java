package org.pedro.android.mobibalises_common.service;

import java.lang.ref.WeakReference;

import android.os.Binder;

/**
 * 
 * @author pedro.m
 */
public final class ProvidersServiceBinder extends Binder
{
  private final WeakReference<IProvidersService> service;

  /**
   * 
   */
  @SuppressWarnings("unused")
  private ProvidersServiceBinder()
  {
    service = null;
  }

  /**
   * 
   * @param service
   */
  public ProvidersServiceBinder(final IProvidersService service)
  {
    super();
    this.service = new WeakReference<IProvidersService>(service);
  }

  /**
   * 
   * @return
   */
  public IProvidersService getService()
  {
    return service.get();
  }
}
