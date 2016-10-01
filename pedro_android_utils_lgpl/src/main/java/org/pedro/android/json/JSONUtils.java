/*******************************************************************************
 * PedroAndroidUtilsLib is Copyright 2014 by Pedro M.
 * 
 * This file is part of PedroAndroidUtilsLib.
 *
 * PedroAndroidUtilsLib is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * PedroAndroidUtilsLib is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * Commercial Distribution License
 * If you would like to distribute PedroAndroidUtilsLib (or portions thereof)
 * under a license other than the "GNU Lesser General Public License, version
 * 3", please contact Pedro M (pedro.pub@free.fr).
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with PedroAndroidUtilsLib. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.pedro.android.json;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * @author pedro.m
 */
public abstract class JSONUtils
{
  /**
   * 
   * @param json
   * @param name
   * @param list
   * @param objectClass
   * @throws JSONException
   * @throws InstantiationException
   * @throws IllegalAccessException
   */
  public static <T extends JSONAble> void getArray(final JSONObject json, final String name, final List<T> list, final Class<T> objectClass) throws JSONException, InstantiationException, IllegalAccessException
  {
    final JSONArray jsonArray = json.getJSONArray(name);
    list.clear();
    final int nb = jsonArray.length();
    for (int i = 0; i < nb; i++)
    {
      final T object = objectClass.newInstance();
      object.fromJSON(jsonArray.getJSONObject(i));
      list.add(object);
    }
  }

  /**
   * 
   * @param json
   * @param name
   * @param list
   * @throws JSONException
   */
  public static <T extends JSONAble> void putArray(final JSONObject json, final String name, final List<T> list) throws JSONException
  {
    final JSONArray array = new JSONArray();
    for (final T jsonable : list)
    {
      array.put(jsonable.toJSON());
    }
    json.put(name, array);
  }

  /**
   * 
   * @param json
   * @param name
   * @param list
   * @param enumClass
   * @param defaultValue
   * @throws JSONException
   */
  @SuppressWarnings("static-access")
  public static <T extends Enum<T>> void getEnumArray(final JSONObject json, final String name, final List<T> list, final Class<T> enumClass, final Enum<T> defaultValue) throws JSONException
  {
    final JSONArray jsonArray = json.getJSONArray(name);
    list.clear();
    final int nb = jsonArray.length();
    for (int i = 0; i < nb; i++)
    {
      final T value = defaultValue.valueOf(enumClass, jsonArray.getString(i));
      list.add(value);
    }
  }

  /**
   * 
   * @param json
   * @param name
   * @param list
   * @throws JSONException
   */
  public static <T extends Enum<?>> void putEnumArray(final JSONObject json, final String name, final List<T> list) throws JSONException
  {
    final JSONArray array = new JSONArray();
    for (final T jsonable : list)
    {
      array.put(jsonable);
    }
    json.put(name, array);
  }

  /**
   * 
   * @param json
   * @param name
   * @param list
   * @throws JSONException
   * @throws InstantiationException
   * @throws IllegalAccessException
   */
  public static void getObjectArray(final JSONObject json, final String name, final List<Object> list) throws JSONException, InstantiationException, IllegalAccessException
  {
    final JSONArray jsonArray = json.getJSONArray(name);
    list.clear();
    final int nb = jsonArray.length();
    for (int i = 0; i < nb; i++)
    {
      list.add(jsonArray.get(i));
    }
  }

  /**
   * 
   * @param json
   * @param name
   * @param list
   * @throws JSONException
   */
  public static void putObjectArray(final JSONObject json, final String name, final List<Object> list) throws JSONException
  {
    final JSONArray array = new JSONArray();
    for (final Object object : list)
    {
      array.put(object);
    }
    json.put(name, array);
  }
}
