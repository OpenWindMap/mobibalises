package com.pedro.android.mobibalises.favorites;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pedro.android.mobibalises.R;

/**
 * 
 * @author pedro.m
 */
public class FavoritesLabelsFragment extends ListFragment
{
  @Override
  public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState)
  {
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_favorites_labels, container, false);
  }
}
