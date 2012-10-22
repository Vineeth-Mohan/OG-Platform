/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.engine.test;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.opengamma.core.change.BasicChangeManager;
import com.opengamma.core.change.ChangeManager;
import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.config.impl.ConfigItem;
import com.opengamma.engine.view.ViewDefinition;
import com.opengamma.id.ObjectId;
import com.opengamma.id.UniqueId;
import com.opengamma.id.VersionCorrection;

/**
 * A mock config source for testing.
 */
public class MockConfigSource implements ConfigSource {

  /**
   * The map of data.
   */
  private final Map<ObjectId, ConfigItem<?>> _store = newHashMap();
  /**
   * The change manager.
   */
  private final ChangeManager _changeManager = new BasicChangeManager();
  
  @SuppressWarnings("unchecked")
  @Override
  public <T> ConfigItem<T> get(Class<T> clazz, String configName, VersionCorrection versionCorrection) {
    for (ConfigItem<?> configItem : _store.values()) {
      if (clazz.isAssignableFrom(configItem.getType()) && configItem.getName().equals(configName)) {
        return (ConfigItem<T>) configItem;
      }
    }
    return null;
  }

  @Override
  public ConfigItem<?> get(UniqueId uniqueId) {
    return _store.get(uniqueId.getObjectId());
  }

  @Override
  public ConfigItem<?> get(ObjectId objectId, VersionCorrection versionCorrection) {
    for (ConfigItem<?> configItem : _store.values()) {
      if (configItem.getObjectId().equals(objectId)) {
        return configItem;
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <R> Collection<ConfigItem<R>> getAll(Class<R> clazz, VersionCorrection versionCorrection) {
    List<ConfigItem<R>> list = newArrayList();
    for (ConfigItem<?> configItem : _store.values()) {
      if (clazz.isAssignableFrom(configItem.getType())) {
        list.add((ConfigItem<R>) configItem);
      }
    }
    return list;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <R> R getConfig(Class<R> clazz, UniqueId uniqueId) {
    return (R) get(uniqueId).getValue();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <R> R getConfig(Class<R> clazz, ObjectId objectId, VersionCorrection versionCorrection) {
    return (R) get(objectId, versionCorrection).getValue();
  }

  @Override
  public <R> R getConfig(Class<R> clazz, String configName, VersionCorrection versionCorrection) {
    return get(clazz, configName, versionCorrection).getValue();
  }

  @Override
  public <R> R getLatestByName(Class<R> clazz, String name) {
    return getConfig(clazz, name, VersionCorrection.LATEST);
  }

  @Override
  public ChangeManager changeManager() {
    return _changeManager;
  }

  @Override
  public Map<UniqueId, ConfigItem<?>> get(Collection<UniqueId> uniqueIds) {
    Map<UniqueId, ConfigItem<?>> map = newHashMap();
    for (UniqueId uniqueId : uniqueIds) {
      ConfigItem<?> item = get(uniqueId);
      map.put(uniqueId, item);
    }
    return map;
  }

  public ConfigItem<ViewDefinition> put(ViewDefinition viewDefinition) {
    ConfigItem<ViewDefinition> item = ConfigItem.of(viewDefinition);
    if (item.getValue().getUniqueId() == null) {
      item.getValue().setUniqueId(UniqueId.of(ViewDefinition.class.getName(), item.getValue().getName()));
    }
    _store.put(viewDefinition.getUniqueId().getObjectId(), item);
    return item;
  }

}
