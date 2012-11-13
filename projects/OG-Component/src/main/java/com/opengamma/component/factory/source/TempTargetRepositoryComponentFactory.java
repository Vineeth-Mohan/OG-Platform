/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.component.factory.source;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.io.Files;
import com.opengamma.component.ComponentInfo;
import com.opengamma.component.ComponentRepository;
import com.opengamma.component.factory.AbstractComponentFactory;
import com.opengamma.component.factory.ComponentInfoAttributes;
import com.opengamma.financial.temptarget.BerkeleyDBTempTargetRepository;
import com.opengamma.financial.temptarget.TempTargetRepository;
import com.opengamma.financial.temptarget.TempTargetSource;
import com.opengamma.financial.temptarget.rest.DataTempTargetRepositoryResource;
import com.opengamma.financial.temptarget.rest.DataTempTargetSourceResource;
import com.opengamma.financial.temptarget.rest.RemoteTempTargetRepository;
import com.opengamma.financial.temptarget.rest.RemoteTempTargetSource;

/**
 * Component factory for the temporary target repository
 */
@BeanDefinition
public class TempTargetRepositoryComponentFactory extends AbstractComponentFactory {

  /**
   * The classifier that the factory should publish under.
   */
  @PropertyDefinition(validate = "notNull")
  private String _classifier;

  /**
   * The flag determining whether the component should be published by REST (default true).
   */
  @PropertyDefinition
  private boolean _publishRest = true;

  protected void registerSource(final ComponentRepository repo, final TempTargetSource instance) {
    final ComponentInfo info = new ComponentInfo(TempTargetSource.class, getClassifier());
    info.addAttribute(ComponentInfoAttributes.LEVEL, 1);
    info.addAttribute(ComponentInfoAttributes.REMOTE_CLIENT_JAVA, RemoteTempTargetSource.class);
    repo.registerComponent(info, instance);
    if (isPublishRest()) {
      repo.getRestComponents().publish(info, new DataTempTargetSourceResource(instance));
    }
  }

  protected void registerRepository(final ComponentRepository repo, final TempTargetRepository instance) {
    final ComponentInfo info = new ComponentInfo(TempTargetRepository.class, getClassifier());
    info.addAttribute(ComponentInfoAttributes.LEVEL, 1);
    info.addAttribute(ComponentInfoAttributes.REMOTE_CLIENT_JAVA, RemoteTempTargetRepository.class);
    repo.registerComponent(info, instance);
    if (isPublishRest()) {
      repo.getRestComponents().publish(info, new DataTempTargetRepositoryResource(instance));
    }
  }

  protected TempTargetRepository createRepository(final ComponentRepository repo, final LinkedHashMap<String, String> configuration) {
    final File tmp = Files.createTempDir();
    return new BerkeleyDBTempTargetRepository(tmp);
  }

  @Override
  public void init(final ComponentRepository repo, final LinkedHashMap<String, String> configuration) {
    final TempTargetRepository tempTargets = createRepository(repo, configuration);
    registerSource(repo, tempTargets);
    registerRepository(repo, tempTargets);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code TempTargetRepositoryComponentFactory}.
   * @return the meta-bean, not null
   */
  public static TempTargetRepositoryComponentFactory.Meta meta() {
    return TempTargetRepositoryComponentFactory.Meta.INSTANCE;
  }
  static {
    JodaBeanUtils.registerMetaBean(TempTargetRepositoryComponentFactory.Meta.INSTANCE);
  }

  @Override
  public TempTargetRepositoryComponentFactory.Meta metaBean() {
    return TempTargetRepositoryComponentFactory.Meta.INSTANCE;
  }

  @Override
  protected Object propertyGet(final String propertyName, final boolean quiet) {
    switch (propertyName.hashCode()) {
      case -281470431:  // classifier
        return getClassifier();
      case -614707837:  // publishRest
        return isPublishRest();
    }
    return super.propertyGet(propertyName, quiet);
  }

  @Override
  protected void propertySet(final String propertyName, final Object newValue, final boolean quiet) {
    switch (propertyName.hashCode()) {
      case -281470431:  // classifier
        setClassifier((String) newValue);
        return;
      case -614707837:  // publishRest
        setPublishRest((Boolean) newValue);
        return;
    }
    super.propertySet(propertyName, newValue, quiet);
  }

  @Override
  protected void validate() {
    JodaBeanUtils.notNull(_classifier, "classifier");
    super.validate();
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      final TempTargetRepositoryComponentFactory other = (TempTargetRepositoryComponentFactory) obj;
      return JodaBeanUtils.equal(getClassifier(), other.getClassifier()) &&
          JodaBeanUtils.equal(isPublishRest(), other.isPublishRest()) &&
          super.equals(obj);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash += hash * 31 + JodaBeanUtils.hashCode(getClassifier());
    hash += hash * 31 + JodaBeanUtils.hashCode(isPublishRest());
    return hash ^ super.hashCode();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the classifier that the factory should publish under.
   * @return the value of the property, not null
   */
  public String getClassifier() {
    return _classifier;
  }

  /**
   * Sets the classifier that the factory should publish under.
   * @param classifier  the new value of the property, not null
   */
  public void setClassifier(final String classifier) {
    JodaBeanUtils.notNull(classifier, "classifier");
    this._classifier = classifier;
  }

  /**
   * Gets the the {@code classifier} property.
   * @return the property, not null
   */
  public final Property<String> classifier() {
    return metaBean().classifier().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the flag determining whether the component should be published by REST (default true).
   * @return the value of the property
   */
  public boolean isPublishRest() {
    return _publishRest;
  }

  /**
   * Sets the flag determining whether the component should be published by REST (default true).
   * @param publishRest  the new value of the property
   */
  public void setPublishRest(final boolean publishRest) {
    this._publishRest = publishRest;
  }

  /**
   * Gets the the {@code publishRest} property.
   * @return the property, not null
   */
  public final Property<Boolean> publishRest() {
    return metaBean().publishRest().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code TempTargetRepositoryComponentFactory}.
   */
  public static class Meta extends AbstractComponentFactory.Meta {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code classifier} property.
     */
    private final MetaProperty<String> _classifier = DirectMetaProperty.ofReadWrite(
        this, "classifier", TempTargetRepositoryComponentFactory.class, String.class);
    /**
     * The meta-property for the {@code publishRest} property.
     */
    private final MetaProperty<Boolean> _publishRest = DirectMetaProperty.ofReadWrite(
        this, "publishRest", TempTargetRepositoryComponentFactory.class, Boolean.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> _metaPropertyMap$ = new DirectMetaPropertyMap(
      this, (DirectMetaPropertyMap) super.metaPropertyMap(),
        "classifier",
        "publishRest");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(final String propertyName) {
      switch (propertyName.hashCode()) {
        case -281470431:  // classifier
          return _classifier;
        case -614707837:  // publishRest
          return _publishRest;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends TempTargetRepositoryComponentFactory> builder() {
      return new DirectBeanBuilder<TempTargetRepositoryComponentFactory>(new TempTargetRepositoryComponentFactory());
    }

    @Override
    public Class<? extends TempTargetRepositoryComponentFactory> beanType() {
      return TempTargetRepositoryComponentFactory.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return _metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code classifier} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<String> classifier() {
      return _classifier;
    }

    /**
     * The meta-property for the {@code publishRest} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Boolean> publishRest() {
      return _publishRest;
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
