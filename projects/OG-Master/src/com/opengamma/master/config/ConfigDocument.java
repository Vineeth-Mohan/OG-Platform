/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.master.config;

import java.util.Map;

import javax.time.Instant;

import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.core.config.impl.ConfigItem;
import com.opengamma.id.ObjectId;
import com.opengamma.id.UniqueId;
import com.opengamma.master.AbstractDocument;
import com.opengamma.util.PublicSPI;

/**
 * A document used to pass into and out of the config master.
 */
@PublicSPI
@BeanDefinition
public class ConfigDocument extends AbstractDocument {

  /**
   * The config object held by the document.
   */
  @PropertyDefinition(set = "manual")
  private ConfigItem<?> _object;
  /**
   * The config unique identifier.
   * This field is managed by the master but must be set for updates.
   */
  @PropertyDefinition(get = "manual", set = "manual")
  private UniqueId _uniqueId;
  /**
   * The document name.
   */
  private String _name;

  /**
   * Creates an empty document.
   * This constructor is here for automated bean construction.
   * This document is invalid until the document class gets set.
   */
  private ConfigDocument() {
  }

  /**
   * Creates an empty document.
   *
   * @param configItem  the config item
   */
  public ConfigDocument(ConfigItem<?> configItem) {
    // this method accepts a ? rather than a ConfigItem<?> for caller flexibility
    setObject(configItem);
  }

  /**
   * Creates an instance.
   * 
   * @param <T> the type
   * @param value  the value
   * @param type  the type of the value
   * @param name  the name
   * @param uid  the unique identifier
   * @param versionFrom  the version from
   * @param versionTo  the version to
   * @param correctionFrom  the correction from
   * @param correctionTo  the correction to
   */
  public <T> ConfigDocument(T value, Class<T> type, String name, UniqueId uid, Instant versionFrom, Instant versionTo, Instant correctionFrom, Instant correctionTo) {
    ConfigItem<T> item = ConfigItem.of(value);
    item.setName(name);
    setVersionFromInstant(versionFrom);
    setVersionToInstant(versionTo);
    setCorrectionFromInstant(correctionFrom);
    setCorrectionToInstant(correctionTo);
    item.setType(type);
    setObject(item);
  }

  //-------------------------------------------------------------------------
  @Override
  public UniqueId getUniqueId() {
    if (_uniqueId == null && getObject() != null && getObject().getUniqueId() != null) {
      _uniqueId = getObject().getUniqueId();
    }
    return _uniqueId;
  }

  @Override
  public void setUniqueId(UniqueId uniqueId) {
    _uniqueId = uniqueId;
    if (getObject() != null) {
      getObject().setUniqueId(uniqueId);
    }
  }

  @Override
  public ObjectId getObjectId() {
    return getObject().getObjectId();
  }
 
  /**
   * Gets the name of the config item.
   * 
   * @return the name
   */
  public String getName() {
    if (_name == null && getObject() != null && getObject().getName() != null) {
      _name = getObject().getName();
    }
    return _name;
  }

  /**
   * Sets the name of the config item.
   * 
   * @param name  the name
   */
  public void setName(String name) {
    _name = name;
    if (getObject() != null) {
      getObject().setName(_name);
    }
  }

  /**
   * Sets the config item.
   * 
   * @param object  the config item
   */
  public void setObject(ConfigItem<?> object) {
    _object = object;
    if (object != null) {
      if (_name == null && object.getName() != null) {
        _name = object.getName();
      } else if (_name != null && object.getName() == null) {
        object.setName(_name);
      }
      if (_uniqueId == null && object.getUniqueId() != null) {
        _uniqueId = object.getUniqueId();
      } else if (_uniqueId != null && object.getUniqueId() == null) {
        object.setUniqueId(_uniqueId);
      }
    }
  }

  /**
   * Gets the object type.
   * 
   * @return the type, may be null
   */
  public Class<?> getType() {
    if (getObject() != null) {
      return getObject().getType();
    } else {
      return null;
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ConfigDocument}.
   * @return the meta-bean, not null
   */
  public static ConfigDocument.Meta meta() {
    return ConfigDocument.Meta.INSTANCE;
  }
  static {
    JodaBeanUtils.registerMetaBean(ConfigDocument.Meta.INSTANCE);
  }

  @Override
  public ConfigDocument.Meta metaBean() {
    return ConfigDocument.Meta.INSTANCE;
  }

  @Override
  protected Object propertyGet(String propertyName, boolean quiet) {
    switch (propertyName.hashCode()) {
      case -1023368385:  // object
        return getObject();
      case -294460212:  // uniqueId
        return getUniqueId();
    }
    return super.propertyGet(propertyName, quiet);
  }

  @Override
  protected void propertySet(String propertyName, Object newValue, boolean quiet) {
    switch (propertyName.hashCode()) {
      case -1023368385:  // object
        setObject((ConfigItem<?>) newValue);
        return;
      case -294460212:  // uniqueId
        setUniqueId((UniqueId) newValue);
        return;
    }
    super.propertySet(propertyName, newValue, quiet);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      ConfigDocument other = (ConfigDocument) obj;
      return JodaBeanUtils.equal(getObject(), other.getObject()) &&
          JodaBeanUtils.equal(getUniqueId(), other.getUniqueId()) &&
          super.equals(obj);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash += hash * 31 + JodaBeanUtils.hashCode(getObject());
    hash += hash * 31 + JodaBeanUtils.hashCode(getUniqueId());
    return hash ^ super.hashCode();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the config object held by the document.
   * @return the value of the property
   */
  public ConfigItem<?> getObject() {
    return _object;
  }

  /**
   * Gets the the {@code object} property.
   * @return the property, not null
   */
  public final Property<ConfigItem<?>> object() {
    return metaBean().object().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the the {@code uniqueId} property.
   * This field is managed by the master but must be set for updates.
   * @return the property, not null
   */
  public final Property<UniqueId> uniqueId() {
    return metaBean().uniqueId().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ConfigDocument}.
   */
  public static class Meta extends AbstractDocument.Meta {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code object} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ConfigItem<?>> _object = DirectMetaProperty.ofReadWrite(
        this, "object", ConfigDocument.class, (Class) ConfigItem.class);
    /**
     * The meta-property for the {@code uniqueId} property.
     */
    private final MetaProperty<UniqueId> _uniqueId = DirectMetaProperty.ofReadWrite(
        this, "uniqueId", ConfigDocument.class, UniqueId.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> _metaPropertyMap$ = new DirectMetaPropertyMap(
      this, (DirectMetaPropertyMap) super.metaPropertyMap(),
        "object",
        "uniqueId");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1023368385:  // object
          return _object;
        case -294460212:  // uniqueId
          return _uniqueId;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends ConfigDocument> builder() {
      return new DirectBeanBuilder<ConfigDocument>(new ConfigDocument());
    }

    @Override
    public Class<? extends ConfigDocument> beanType() {
      return ConfigDocument.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return _metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code object} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<ConfigItem<?>> object() {
      return _object;
    }

    /**
     * The meta-property for the {@code uniqueId} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<UniqueId> uniqueId() {
      return _uniqueId;
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
