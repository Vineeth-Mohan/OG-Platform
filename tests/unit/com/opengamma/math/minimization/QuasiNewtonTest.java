/**
 * Copyright (C) 2009 - 2010 by OpenGamma Inc.
 * 
 * Please see distribution for license.
 */
package com.opengamma.math.minimization;

import org.junit.Test;

/**
 * 
 */
public class QuasiNewtonTest extends MultidimensionalMinimizerWithGradiantTestCase {
  private static VectorMinimizerWithGradient MINIMISER = new QuasiNewtonVectorMinimizer();

  @Test
  public void testSolvingRosenbrock() {
    super.testSolvingRosenbrock(MINIMISER);
  }

  @Test
  public void testSolvingCoupledRosenbrock() {
    super.testSolvingCoupledRosenbrock(MINIMISER);
  }

}
