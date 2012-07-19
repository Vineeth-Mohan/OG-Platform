/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.livedata.server;

import com.opengamma.id.ExternalId;

/**
 * A Factory for {@link LastKnownValueStore}s.
 */
public interface LastKnownValueStoreProvider {
  
  LastKnownValueStore newInstance(ExternalId security, String normalizationRuleSetId);
}