/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.examples.tutorial;

import java.util.List;

import com.opengamma.engine.function.config.AbstractRepositoryConfigurationBean;
import com.opengamma.engine.function.config.FunctionConfiguration;
import com.opengamma.engine.function.config.RepositoryConfigurationSource;
import com.opengamma.engine.function.config.StaticFunctionConfiguration;

/**
 * Function repository configuration source for the functions contained in this package.
 */
public class TutorialFunctions extends AbstractRepositoryConfigurationBean {

  /**
   * Default instance of a repository configuration source exposing the functions from this package.
   */
  public static final RepositoryConfigurationSource DEFAULT = (new TutorialFunctions()).getObjectCreating();

  @Override
  protected void addAllConfigurations(final List<FunctionConfiguration> functions) {
    functions.add(new StaticFunctionConfiguration(TutorialValueFunction.class.getName()));
  }

}