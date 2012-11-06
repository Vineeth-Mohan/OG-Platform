/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.analytics.model.swaption.deprecated;

import java.util.List;

import com.opengamma.engine.function.config.AbstractRepositoryConfigurationBean;
import com.opengamma.engine.function.config.FunctionConfiguration;
import com.opengamma.engine.function.config.RepositoryConfigurationSource;

/**
 * Function repository configuration source for the functions contained in this package.
 */
public class DeprecatedFunctions extends AbstractRepositoryConfigurationBean {

  /**
   * Default instance of a repository configuration source exposing the functions from this package.
   */
  public static final RepositoryConfigurationSource DEFAULT = (new DeprecatedFunctions()).getObjectCreating();

  @Override
  protected void addAllConfigurations(final List<FunctionConfiguration> functions) {
    functions.add(functionConfiguration(SwaptionBlackPresentValueFunctionDeprecated.class));
    functions.add(functionConfiguration(SwaptionBlackVolatilitySensitivityFunctionDeprecated.class));
    functions.add(functionConfiguration(SwaptionBlackPV01FunctionDeprecated.class));
    functions.add(functionConfiguration(SwaptionBlackYieldCurveNodeSensitivitiesFunctionDeprecated.class));
    functions.add(functionConfiguration(SwaptionBlackImpliedVolatilityFunctionDeprecated.class));
  }

}