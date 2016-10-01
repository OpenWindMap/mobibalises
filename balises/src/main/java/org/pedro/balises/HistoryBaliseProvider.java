package org.pedro.balises;

import java.io.IOException;
import java.util.Collection;

/**
 * 
 * @author pedro.m
 */
public interface HistoryBaliseProvider extends BaliseProvider
{
  /**
   * 
   * @param baliseId
   * @param duree
   * @param peremption
   * @return
   * @throws IOException
   */
  public Collection<Releve> getHistoriqueBalise(final String baliseId, final int duree, final int peremption) throws IOException;
}
