/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.analytics.model.equity.indexoption;

import com.opengamma.analytics.financial.equity.StaticReplicationDataBundle;
import com.opengamma.analytics.financial.equity.option.EquityIndexOption;
import com.opengamma.analytics.financial.equity.option.EquityIndexOptionBlackMethod;
import com.opengamma.engine.value.ValueRequirementNames;

/**
 *
 */
public class EquityIndexOptionImpliedVolFunction extends EquityIndexOptionFunction {

  /**
   * @param valueRequirementName
   */
  public EquityIndexOptionImpliedVolFunction() {
    super(ValueRequirementNames.IMPLIED_VOLATILITY);
  }

  @Override
  protected Object computeValues(EquityIndexOption derivative, StaticReplicationDataBundle market) {
    EquityIndexOptionBlackMethod model = EquityIndexOptionBlackMethod.getInstance();
    return model.impliedVol(derivative, market);
  }

}
